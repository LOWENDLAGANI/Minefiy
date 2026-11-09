# Stage 1 — Python yt-proxy (ytmusicapi Backend)

> Goal: Deployable FastAPI service that wraps `ytmusicapi` for search/metadata only. No audio, no keys, no DB.

## APPLIED 2026-11-09 — verified locally, NOT deployed

Built in `yt-proxy/`: `main.py`, `requirements.txt`, `Dockerfile`, `.dockerignore`, `README.md`. Ran under uvicorn against live YouTube Music and every endpoint returned real data:

| Endpoint | Result |
|----------|--------|
| `/health` | 200, reports `ytmusicapi: 1.12.3` |
| `/search?filter=songs` | 200, 20 songs with `videoId`, `duration_seconds`, thumbnails |
| `/search?filter=albums` | 200, `browseId` present |
| `/search?filter=artists` | 200, `browseId` (channelId) present |
| `/search?filter=playlists` | 200 |
| `/album/{browseId}` | 200, title + tracks + year |
| `/artist/{channelId}` | 200, songs + albums |
| `/song/{videoId}` | 200, `videoDetails` when the track is playable |
| `/watch/{videoId}` | 200, 50 queue tracks + `lyrics` browseId |
| `/lyrics/{videoId}` | 200, 1877 chars |
| `/lyrics/{MPLYt_...}` | 200, 1606 chars |
| `filter=bogus` | 400 with a helpful `detail` |
| cache | identical search 0.45s → 0.003s |

### Corrections found by testing

1. **`ytmusicapi` must be ≥ 1.12.** On 1.11.1, `get_watch_playlist` raises `KeyError: 'endpoint'` against current responses. Pinned **1.12.3** in `requirements.txt`.
2. **`get_lyrics()` takes a browseId, not a videoId.** Correct contract: `/lyrics/{track_id}` accepts either a videoId (resolved through `/watch` → `lyrics`) or the `MPLYt_...` browseId returned by `/watch`. Returns **404** when a track has no lyrics.
3. **`limit` is not honored upstream.** `ytmusicapi` returns fixed shelves (20 search results, 50 queue tracks); the proxy now trims to `limit` for `/search` and `/watch`.
4. **`get_lyrics()` returns a model object**, not a dict — the proxy serializes dataclasses/objects before responding.
5. Some videoIds return only `playabilityStatus` (region/age restricted). `/song` passes that through; the app must treat a missing `videoDetails` as unplayable.

### Not done yet

- Cloud Run deploy (`gcloud run deploy`) and putting the URL in `local.properties` as `yt.proxy.base.url` — needs your GCP project.
- `YTMUSIC_AUTH_FILE` (browser.json / oauth.json) for signed-in library access — not needed for search.

## Folder: `yt-proxy/`

```
yt-proxy/
├── main.py            # FastAPI app, all endpoints
├── requirements.txt
├── Dockerfile
├── README.md          # local run + gcloud deploy steps
└── .dockerignore
```

## requirements.txt

```
fastapi==0.115.0
uvicorn[standard]==0.30.6
ytmusicapi==1.12.3
httpx==0.27.2
pydantic==2.9.2
```

Python 3.11. `ytmusicapi` requires 3.10+.

## main.py — Endpoints

```python
from fastapi import FastAPI, HTTPException
from ytmusicapi import YTMusic

app = FastAPI(title="Minefiy YT Proxy")
yt = YTMusic()

@app.get("/health")
def health(): return {"ok": True}

@app.get("/search")
def search(q: str, filter: str = "songs", limit: int = 20):
    if not q: raise HTTPException(400, "q required")
    return yt.search(q, filter=filter, limit=limit)

@app.get("/album/{browse_id}")
def album(browse_id: str): return yt.get_album(browse_id)

@app.get("/artist/{channel_id}")
def artist(channel_id: str): return yt.get_artist(channel_id)

@app.get("/song/{video_id}")
def song(video_id: str): return yt.get_song(video_id)

@app.get("/watch/{video_id}")
def watch(video_id: str): return yt.get_watch_playlist(video_id)

@app.get("/lyrics/{video_id}")
def lyrics(video_id: str): return yt.get_lyrics(video_id)
```

Add in-memory cache: `functools.lru_cache` or dict with TTL 600s keyed by `(endpoint, args)`.

Add CORS: allow `*` for MVP, restrict to your app later.

## Return Shapes (What Android Maps)

`ytmusicapi` returns raw dicts. Key fields Android needs:

**search songs:**
```json
[{ "videoId": "dQw4w9WgXcQ", "title": "Never Gonna Give You Up",
   "artists": [{"name": "Rick Astley"}],
   "album": {"name": "Whenever You Need Somebody"},
   "duration_seconds": 213,
   "thumbnails": [{"url": "https://..."}, {"url": "https://.../hqdefault.jpg"}] }]
```

**search albums:** `browseId`, `title`, `artists`, `thumbnails`, `year`
**search artists:** `browseId` (channelId), `title`, `thumbnails`, `subscribersCount`
**search playlists:** `browseId`, `title`, `thumbnails`, `trackCount`

**get_album:** `title`, `artists`, `description`, `thumbnails`, `tracks[]` (each with `videoId, title, length, thumbnails`)
**get_artist:** `name`, `description`, `thumbnails`, `songs[]`, `albums[]`
**get_watch_playlist (radio):** `tracks[]` (same shape as search song)
**get_lyrics:** `lyrics` (plain text)

Android should read `thumbnails.last().url` for highest res.

## Local Test

```bash
cd yt-proxy
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
curl "http://localhost:8000/search?q=coldplay&filter=songs&limit=3"
curl "http://localhost:8000/health"
```

Expected: JSON array with `videoId`s. If 429/403 from YouTube, wait 60s, retry single request. YouTube rate-limits datacenter IPs.

## Deploy (Cloud Run)

```bash
gcloud run deploy minefiy-yt \
  --source ./yt-proxy \
  --region us-central1 \
  --allow-unauthenticated \
  --min-instances 0 --max-instances 2 \
  --memory 512Mi --cpu 1 \
  --timeout 60
# Outputs URL like https://minefiy-yt-xxx-uc.a.run.app
```

Put URL in `local.properties`:
```properties
yt.proxy.base.url=https://minefiy-yt-xxx-uc.a.run.app
```

## Auth (Later, Optional)

Start with `YTMusic()` (no headers) — public search only. For library/playlists, add `ytmusicapi ytmusicapi oauth` to generate `oauth.json`, mount as secret in Cloud Run.

## Exit Criteria

- [ ] `curl /health` → `{"ok": true}`
- [ ] `curl /search?q=coldplay` → 3+ songs with `videoId`
- [ ] `curl /album/MPREb_...` → album with tracks
- [ ] Cloud Run URL returns same from phone (no VPN)
- [ ] Cache: second identical search returns in <50ms (check logs)

## Files Created

- `yt-proxy/main.py`, `requirements.txt`, `Dockerfile`, `README.md`
- Update `local.properties.example` with `yt.proxy.base.url`
