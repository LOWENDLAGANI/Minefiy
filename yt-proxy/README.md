# Minefiy yt-proxy — local run and deploy

Metadata-only YouTube Music API for the Minefiy Android app, built on
[`ytmusicapi`](https://github.com/sigma67/ytmusicapi) + FastAPI.

**It never proxies audio.** Search/album/artist/queue/lyrics only. Stream URLs
are resolved on the device ([docs/07-stage-4-playback.md](../docs/07-stage-4-playback.md))
because YouTube throttles datacenter IPs.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/` | Service info |
| GET | `/health` | Liveness + cache stats |
| GET | `/search?q=&filter=&limit=` | `songs`, `albums`, `artists`, `playlists`, `videos` (+3 more) |
| GET | `/album/{browseId}` | Album with tracks |
| GET | `/artist/{channelId}` | Artist with songs/albums |
| GET | `/song/{videoId}` | Single track metadata |
| GET | `/watch/{videoId}` | Radio / "up next" queue |
| GET | `/lyrics/{videoId}` | Plain-text lyrics (404 when unavailable) |

Interactive docs at `/docs` when running locally.

## Local run

```bash
cd yt-proxy
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

```bash
curl -s localhost:8000/health
curl -s "localhost:8000/search?q=coldplay&filter=songs&limit=3"
curl -s "localhost:8000/search?q=random%20access%20memories&filter=albums&limit=2"
```

## Deploy to Cloud Run

```bash
gcloud run deploy minefiy-yt \
  --source ./yt-proxy \
  --region us-central1 \
  --allow-unauthenticated \
  --min-instances 0 --max-instances 3 \
  --memory 512Mi --cpu 1 \
  --timeout 60 --concurrency 80
```

Then put the printed URL into `local.properties` at the repo root:

```properties
yt.proxy.base.url=https://minefiy-yt-xxxx-uc.a.run.app
```

## Configuration

| Env var | Default | Meaning |
|---------|---------|---------|
| `PORT` | `8080` | Port uvicorn binds to (Cloud Run sets this) |
| `CACHE_TTL_SECONDS` | `600` | In-memory TTL for search/album/artist/song responses |
| `YTMUSIC_AUTH_FILE` | unset | Path to `browser.json` / `oauth.json` for library access |

Generate auth headers only if you need the signed-in library:

```bash
ytmusicapi browser    # or: ytmusicapi oauth
```

Keep that file out of git and mount it as a Cloud Run secret.

## Notes / limits

- Unauthenticated mode: public search and browsing work; personal library does not.
- Cache is per-instance and disposable — a cold start begins empty.
- YouTube rate-limits datacenter IPs. If `/search` starts returning 502, the
  upstream is throttling; wait a minute, lower `limit`, or keep `max-instances` low.
- Errors are normalized: upstream failures return `502` with a `detail` string,
  bad input returns `400`, missing lyrics returns `404`.

## Legal

Unofficial YouTube Music access. Violates YouTube's Terms of Service. Sideload
the APK; do not publish this as a YouTube Music client on Google Play.
