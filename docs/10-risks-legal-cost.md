# 10 — Risks, Legal, Cost, Maintenance

## Legal

- `ytmusicapi` and InnerTube API are **unofficial**. Using them violates **YouTube Terms of Service**.
- **Do not submit to Google Play Store** as a YouTube Music clone. Risk: takedown, developer ban.
- Distribute as sideloaded APK (`assembleRelease` → share via GitHub Releases / Firebase App Distribution).
- Add disclaimer in `AboutScreen.kt` and README: "Unofficial client, not affiliated with Google/YouTube."
- Libraries used are MIT/Apache-2.0 (ytmusicapi MIT, NewPipeExtractor GPL-3.0 — check license if you ship). InnerTune `innertube` is Apache-2.0.

## Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| YouTube changes InnerTube `player` | High (every few months) | Playback breaks for all YT songs | Client fallback (ANDROID→WEB→IOS), NewPipe fallback, budget 1 day per break, track InnerTune upstream |
| Datacenter IP block (Cloud Run) | Medium | Search 403/429 | In-memory cache, retry with jitter, keep `limit=20` low, use `search` sparingly |
| PO-Token requirement | Medium (2025-26) | `WEB` client fails | Use `ANDROID` client primary, add `visitorData` |
| Stream URL expiry mid-play | Low | 403 on seek | Re-resolve on `Player.STATE` error, cache 1h TTL |
| B2 keys leaked in APK | Known (existing) | Anyone can list bucket | Least-privilege B2 key, short presigned expiry, plan backend signing |
| firebase `google-services.json` | Low | Repo exposes project ID | It's public identifier, not secret. Real auth is Firebase Auth rules. |
| Media3 1.7 breaking changes | Medium | Playback regression | Stage 0 isolate Media3 bump, QA B2 + YT playback |
| Cloud Run cold start | Medium | Search 1-3s slow first hit | `min-instances=0` (cost) vs `1` (warm). For MVP, `0` + loading spinner is fine. |

## Cost Estimate (Personal / Small)

- **Cloud Run (yt-proxy):** $0–5/mo (free tier: 180k requests, 360k vCPU-seconds). YouTube search only, no audio.
- **Firestore:** Unchanged. Favorites: ~10 docs/user, negligible.
- **B2:** Unchanged (your MP3s + artwork).
- **Firebase Auth/Hosting:** Free tier.
- **Audio:** $0 — streams direct Google → phone.

## Maintenance Cadence

- **Weekly:** Check `yt-proxy` logs for 429/5xx, check Cloud Run health.
- **Monthly:** Bump `ytmusicapi` (`pip install -U ytmusicapi`), Bump Gradle deps (Dependabot).
- **As-needed (YouTube breaks):** Bump InnerTube client version or `NewPipeExtractor` version. Test on real device.
- **Quarterly:** Review B2 key rotation, Firebase rules, `local.properties.example` accuracy.

## Alternatives Considered & Rejected

1. **Pure Kotlin YTM (no backend):** Re-implement ytmusicapi in Kotlin. Rejected for MVP — 2x work, no auth library, harder to patch.
2. **Firebase Functions (Gen1 JS) for YTM:** Can't run `ytmusicapi` in JS. Would need Python Functions (Gen2) — possible, but Cloud Run simpler.
3. **Proxy audio via backend:** Rejected — bandwidth cost ($), IP block risk, legal headache.
4. **Firebase Hosting for backend:** Impossible — static only.

## Rollback Plan

Each stage is reversible:
- Stage 0: Git tag before upgrade (`git tag pre-stage0`). Revert bump PRs individually.
- Stage 1-6: YT code is additive. Disable by setting `yt.proxy.base.url=""` → YT tab shows "Not configured", B2 library still works.

## Open Questions

- [ ] Cloud Run region: `us-central1` vs closest to users?
- [ ] Do you want `oauth.json` for user library import (Stage 5+)?
- [ ] Sideload distribution: GitHub Releases vs Firebase App Distribution?
- [ ] Raise `minSdk 26 → 30`? (Drops ~5% old devices, simplifies permissions)
- [ ] Coil 3 migration in Stage 0 or Stage 6?
