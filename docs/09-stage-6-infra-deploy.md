# Stage 6 — Infra, Deploy, Harden

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
