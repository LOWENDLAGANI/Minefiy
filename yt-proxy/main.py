"""Minefiy YouTube Music proxy (metadata only).

Wraps `ytmusicapi` behind a small FastAPI service so the Android app can
search and browse YouTube Music without embedding a Python runtime.

This service is intentionally metadata-only: it never proxies audio. Stream
URLs are resolved on the device (see docs/07-stage-4-playback.md) because
YouTube throttles datacenter IPs.

Endpoints
    GET /health
    GET /search?q=&filter=songs|albums|artists|playlists&limit=20
    GET /album/{browse_id}
    GET /artist/{channel_id}
    GET /song/{video_id}
    GET /watch/{video_id}?limit=25&radio=false
    GET /lyrics/{track_id}          # videoId or MPLYt_... browseId

Requires ytmusicapi >= 1.12: earlier versions raise KeyError('endpoint') in
get_watch_playlist against the current YouTube Music responses.
"""

from __future__ import annotations

import dataclasses
import os
import threading
import time
from typing import Any, Callable

from fastapi import FastAPI, HTTPException, Path, Query
from fastapi.middleware.cors import CORSMiddleware
from ytmusicapi import YTMusic

try:
    from ytmusicapi import __version__ as YTMUSICAPI_VERSION
except Exception:  # pragma: no cover - version attribute is optional
    YTMUSICAPI_VERSION = "unknown"

APP_VERSION = "1.1.0"
CACHE_TTL_SECONDS = int(os.getenv("CACHE_TTL_SECONDS", "600"))
MAX_LIMIT = 50
LYRICS_BROWSE_ID_PREFIX = "MPLYt_"

SEARCH_FILTERS = {
    "songs",
    "videos",
    "albums",
    "artists",
    "playlists",
    "community_playlists",
    "featured_playlists",
}

app = FastAPI(
    title="Minefiy YT Proxy",
    description="Metadata-only YouTube Music API for the Minefiy Android app.",
    version=APP_VERSION,
)

# The Android client sends no browser Origin, and a wildcard here only exposes
# public metadata. Tighten this once the app has a fixed web origin.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"],
)


class _TtlCache:
    """Tiny thread-safe TTL cache.

    Instance-local and disposable: Cloud Run may run several instances, and a
    cold start simply starts with an empty cache.
    """

    def __init__(self, ttl_seconds: int) -> None:
        self._ttl = ttl_seconds
        self._lock = threading.Lock()
        self._store: dict[str, tuple[float, Any]] = {}

    def get(self, key: str) -> Any | None:
        with self._lock:
            entry = self._store.get(key)
            if entry is None:
                return None
            expires_at, value = entry
            if expires_at < time.monotonic():
                del self._store[key]
                return None
            return value

    def set(self, key: str, value: Any) -> None:
        with self._lock:
            self._store[key] = (time.monotonic() + self._ttl, value)

    def __len__(self) -> int:
        with self._lock:
            return len(self._store)


_cache = _TtlCache(CACHE_TTL_SECONDS)

_ytmusic: YTMusic | None = None
_ytmusic_lock = threading.Lock()


def get_client() -> YTMusic:
    """Create the YTMusic client once per process.

    Unauthenticated for now: public search and browsing only. Point
    YTMUSIC_AUTH_FILE at a browser.json / oauth.json to enable library access.
    """
    global _ytmusic
    if _ytmusic is None:
        with _ytmusic_lock:
            if _ytmusic is None:
                auth_file = os.getenv("YTMUSIC_AUTH_FILE")
                _ytmusic = YTMusic(auth_file) if auth_file else YTMusic()
    return _ytmusic


def _cached(key: str, produce: Callable[[], Any]) -> Any:
    hit = _cache.get(key)
    if hit is not None:
        return hit
    value = produce()
    if value is not None:
        _cache.set(key, value)
    return value


def _apply_limit(value: Any, limit: int, key: str | None = None) -> Any:
    """Honor the caller's limit.

    ytmusicapi returns fixed-size shelves (20 search results, 50 queue tracks)
    and ignores ``limit`` for several filters, so trim here to keep the API
    contract honest for the Android client.
    """
    if isinstance(value, list):
        return value[:limit]
    if isinstance(value, dict) and key and isinstance(value.get(key), list):
        trimmed = dict(value)
        trimmed[key] = value[key][:limit]
        return trimmed
    return value


def _jsonable(obj: Any) -> Any:
    """Convert ytmusicapi model objects (e.g. Lyrics) into plain JSON."""
    if obj is None or isinstance(obj, (str, int, float, bool)):
        return obj
    if isinstance(obj, dict):
        return {str(k): _jsonable(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [_jsonable(item) for item in obj]
    if dataclasses.is_dataclass(obj) and not isinstance(obj, type):
        return {
            field.name: _jsonable(getattr(obj, field.name, None))
            for field in dataclasses.fields(obj)
        }
    if hasattr(obj, "__dict__"):
        return {
            key: _jsonable(value)
            for key, value in vars(obj).items()
            if not key.startswith("_")
        }
    return str(obj)


def _upstream_error(action: str, exc: Exception) -> HTTPException:
    """Map ytmusicapi/network failures onto a 502 the app can retry."""
    return HTTPException(
        status_code=502,
        detail=f"{action} failed: {type(exc).__name__}: {exc}",
    )


@app.get("/")
def root() -> dict[str, Any]:
    return {
        "service": "minefiy-yt-proxy",
        "version": APP_VERSION,
        "ytmusicapi": YTMUSICAPI_VERSION,
        "docs": "/docs",
    }


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "ok": True,
        "version": APP_VERSION,
        "ytmusicapi": YTMUSICAPI_VERSION,
        "cache_entries": len(_cache),
        "cache_ttl_seconds": CACHE_TTL_SECONDS,
    }


@app.get("/search")
def search(
    q: str = Query(..., min_length=1, max_length=200, description="Search query"),
    filter: str = Query("songs", description="songs|albums|artists|playlists"),
    limit: int = Query(20, ge=1, le=MAX_LIMIT),
) -> Any:
    if filter not in SEARCH_FILTERS:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported filter '{filter}'. Use one of: {', '.join(sorted(SEARCH_FILTERS))}",
        )

    def produce() -> Any:
        return get_client().search(q, filter=filter, limit=limit)

    try:
        result = _cached(f"search:{filter}:{limit}:{q.lower()}", produce)
    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001 - surface upstream failure as 502
        raise _upstream_error("search", exc) from exc
    return _apply_limit(result, limit)


@app.get("/album/{browse_id}")
def album(browse_id: str = Path(..., min_length=1)) -> Any:
    try:
        return _cached(f"album:{browse_id}", lambda: get_client().get_album(browse_id))
    except Exception as exc:  # noqa: BLE001
        raise _upstream_error("get_album", exc) from exc


@app.get("/artist/{channel_id}")
def artist(channel_id: str = Path(..., min_length=1)) -> Any:
    try:
        return _cached(f"artist:{channel_id}", lambda: get_client().get_artist(channel_id))
    except Exception as exc:  # noqa: BLE001
        raise _upstream_error("get_artist", exc) from exc


@app.get("/song/{video_id}")
def song(video_id: str = Path(..., min_length=1)) -> Any:
    try:
        return _cached(f"song:{video_id}", lambda: get_client().get_song(video_id))
    except Exception as exc:  # noqa: BLE001
        raise _upstream_error("get_song", exc) from exc


@app.get("/watch/{video_id}")
def watch(
    video_id: str = Path(..., min_length=1),
    limit: int = Query(25, ge=1, le=MAX_LIMIT),
    radio: bool = Query(False, description="Return a radio queue instead of the album queue"),
) -> Any:
    """Radio / "up next" queue for a track, plus the track's lyrics browseId."""

    def produce() -> Any:
        return get_client().get_watch_playlist(videoId=video_id, limit=limit, radio=radio)

    try:
        result = _cached(f"watch:{video_id}:{limit}:{radio}", produce)
    except Exception as exc:  # noqa: BLE001
        raise _upstream_error("get_watch_playlist", exc) from exc
    return _apply_limit(result, limit, key="tracks")


@app.get("/lyrics/{track_id}")
def lyrics(track_id: str = Path(..., min_length=1)) -> Any:
    """Lyrics for a track.

    Accepts either a YouTube videoId (resolved through /watch) or a Lyrics
    browseId (``MPLYt_...``) as returned by ``GET /watch/{video_id}``.
    """

    def produce() -> Any:
        browse_id = track_id
        if not browse_id.startswith(LYRICS_BROWSE_ID_PREFIX):
            watch_data = _cached(
                f"watch:{track_id}:25:False",
                lambda: get_client().get_watch_playlist(videoId=track_id),
            )
            browse_id = (watch_data or {}).get("lyrics") or ""
            if not browse_id:
                return None
        return _jsonable(get_client().get_lyrics(browse_id))

    try:
        result = _cached(f"lyrics:{track_id}", produce)
    except Exception as exc:  # noqa: BLE001
        raise _upstream_error("get_lyrics", exc) from exc

    if not result:
        raise HTTPException(status_code=404, detail="No lyrics available for this track")
    return result
