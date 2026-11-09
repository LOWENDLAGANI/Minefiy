# Stage 6 — Infra, Deploy, Harden

## APPLIED 2026-11-09 — deploy config + scripts only; nothing deployed

Nothing in this stage was executed against GCP: `gcloud` is not installed in this
sandbox and no project credentials exist. What is committed is the config and the
scripts, plus local verification of everything that does not need a cloud account.

### `yt-proxy/deploy.sh` (new, executable)

```bash
./yt-proxy/deploy.sh                 # defaults
REGION=europe-west1 MAX_INSTANCES=1 ./yt-proxy/deploy.sh
ALLOW_UNAUTH=false  ./yt-proxy/deploy.sh
```

- Fails fast with an actionable message if `gcloud` is missing, no account is active,
  or no project is set (uses `gcloud config get-value project` unless `PROJECT_ID` is set).
- Enables `run.googleapis.com` + `cloudbuild.googleapis.com` (idempotent).
- `gcloud run deploy --source` so Cloud Build uses the Dockerfile; no local Docker needed.
- Defaults: `512Mi` / `1` CPU, `0..3` instances, concurrency `80`, timeout `60`,
  `CACHE_TTL_SECONDS=600`.
- Smoke-tests `/health` (hard) and `/search?q=coldplay&…&limit=1` (soft: cold start or
  upstream throttle only warns).
- Resolves the service URL from `status.url` and prints the exact
  `yt.proxy.base.url=…` line plus log/rollback/delete commands.

### `yt-proxy/.gcloudignore` (new)

Keeps `.venv`, `__pycache__`, logs, `local.properties` and the deploy script itself
out of the Cloud Build upload. Note this matters more than usual: the local venv is
hundreds of MB and `--source` would otherwise tar it.

### `yt-proxy/Dockerfile` (hardened)

- `CMD exec uvicorn … --port "${PORT}"` with **`--proxy-headers`** and
  `--forwarded-allow-ips='*'`: Cloud Run terminates TLS at its own proxy, so without
  this uvicorn logs the proxy IP and mis-detects the scheme.
- `--workers 1` on purpose — Cloud Run scales by instances, and multiple uvicorn
  workers would each keep a separate in-memory response cache (the naive
  "workers = cores" tuning actively hurts this service).
- `PIP_NO_CACHE_DIR=1` and non-root `uid 10001` already matched the previous image.

### `yt-proxy/README.md`

Deploy section rewritten around the script, with the env-var table, the manual
`gcloud` equivalent, secret mounting for `YTMUSIC_AUTH_FILE`, and an explicit note
that `--allow-unauthenticated` means anyone with the URL spends your quota.

### Verification actually performed

- `bash -n yt-proxy/deploy.sh` → syntax clean; file is executable.
- `python3 -m ast` on `main.py` → parses.
- The Dockerfile's exact runtime command was executed locally against the existing
  venv, including `$PORT` expansion. Results: `/health` →
  `{"ok":true,"ytmusicapi":"1.12.3",…}`; `/search?q=coldplay&filter=songs&limit=2`
  → 2 live tracks ("Viva La Vida", "A Sky Full of Stars"); `X-Forwarded-Proto`/`-For`
  headers accepted; bad filter → `400`.
- The `/lyrics` probe returned **200**, not 404 — that track does have lyrics. The 404
  path was verified in Stage 1 (`docs/04`), so it is not re-claimed here.

### Verification NOT performed (explicit)

- `docker build` — **could not run**: `/var/run/docker.sock` is permission-denied in
  this sandbox, so the image was never built and no container ever ran. The flag
  verification above proves the CMD is valid, not that the image is.
- `gcloud run deploy` — no CLI, no credentials, no project. The first real deploy is
  yours to run.

### Before the first deploy

1. `gcloud auth login && gcloud config set project <id>` (billing must be enabled).
2. `./yt-proxy/deploy.sh` and keep the printed URL.
3. Put it in `local.properties` as `yt.proxy.base.url=…` and rebuild the app —
   `BuildConfig.YT_PROXY_BASE_URL` is a build-time constant, so a stale APK keeps
   pointing at the old URL.
4. Open `https://<url>/docs` once to confirm FastAPI is serving.


> Goal: Production-ready, documented, cheap to run.

## 6.1 Cloud Run Production

```bash
gcloud run deploy minefiy-yt \
  --source ./yt-proxy \
  --region us-central1 \
  --allow-unauthenticated \
  --min-instances 0 --max-instances 3 \
  --memory 512Mi --cpu 1 \
  --timeout 60 \
  --concurrency 80
```

- Set env: `PYTHONUNBUFFERED=1`
- Logs: `gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=minefiy-yt"`
- Custom domain (optional): Map `yt.minefiy.com` via Cloud Run domain mapping.

## 6.2 Firebase Functions (Existing)

`functions/index.js` currently uses Gen1 API `functions.https.onCall`. Either:
- **Keep** as-is (works, deprecated but stable)
- **Migrate** to Gen2 (`firebase-functions/v2/https`) for consistency

Do not break `addAdminRole` — it's used for admin claims.

## 6.3 local.properties Production

Use `local.properties.example` at repo root. Never commit real keys.

```properties
# B2 (required for library)
b2.access.key.id=YOUR_B2_KEY_ID
b2.secret.access.key=YOUR_B2_SECRET
b2.bucket.name=minefiy-music
b2.endpoint=https://f001.backblazeb2.com
admin.email=admin@example.com

# YouTube proxy (required for YTM)
yt.proxy.base.url=https://minefiy-yt-xxx-uc.a.run.app
```

## 6.4 CI / Build

- GitHub Actions: `./gradlew :app:assembleDebug` on push (needs `local.properties` stub with empty keys — build succeeds with empty `BuildConfig`)
- Signed release: `keystore` + `assembleRelease` (manual, keep keys out of repo)
- Dependabot: enable for `gradle` + `pip` (yt-proxy)

## 6.5 Hardening & Observability

- Backend: Add `X-Request-ID` header, structured JSON logging
- Backend: Rate limit per IP (Cloud Armor or simple dict) to prevent abuse proxy
- Android: `YtMusicApi` timeout 15s, retry 1x on IOException
- Android: Crashlytics (optional) — add `firebase-crashlytics` BOM dep, wrap `StreamResolver` failures
- Add `/health` uptime check (UptimeRobot free tier) to `https://...run.app/health`

## 6.6 PO-Token / Client Fallback (Future)

YouTube is rolling out Proof of Origin tokens for Player API. When `WEB` client starts failing:
1. Keep `ANDROID` client primary (needs PO less)
2. Add `visitorData` persistence (store `VISITOR_INFO1_LARGE` cookie)
3. Track InnerTune for `poToken` implementation — port when stable

## 6.7 Coil Migration (Deferred from Stage 0)

Migrate `coil-compose:2.6.0` → `io.coil-kt.coil3:coil-compose:3.x`:
- New import `coil3.compose.AsyncImage`
- `ImageLoader` config changes
- Touch all image composables in one PR

## Exit Criteria

- [ ] Cloud Run URL live, `/health` green
- [ ] Fresh clone + `local.properties.example` → `./gradlew assembleDebug` green
- [ ] Release APK installs on API 35 device, plays both B2 and YT songs
- [ ] README updated with deploy steps + `yt-proxy/README.md`

## Cost (Metadata Only)

- Cloud Run: ~$0–5/mo for personal use (free tier covers 180k requests)
- Firestore: existing cost unchanged (favorites add ~10 docs/user)
- B2: unchanged
- No audio proxy = no bandwidth bill

## Files

- Modified: `.github/workflows/android.yml` (new), `README.md`, `yt-proxy/README.md`
- Verified: `functions/index.js` (Gen1 vs Gen2 decision documented)
