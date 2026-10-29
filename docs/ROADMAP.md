# Auralis Roadmap

## Phase 0 — Foundation

- [x] Repo, brand direction, CI, Gradle Wrapper
- [x] Icon install pipeline (local file / parts / CDN)
- [ ] Commit `scripts/icon-assets/brand-icon.png` permanently (owner drop-in)
- [ ] Final applicationId before store release

## Phase 1 — Offline MVP ✅

- [x] Import, Room, Media3, Now Playing, Mini Player, speed, delete, RTL, Vazirmatn, error banner

## Phase 2 — Hardening (in progress)

- [x] Audio focus, becoming-noisy, wake mode
- [x] MediaSession `sessionActivity` (tap notification → app)
- [x] `POST_NOTIFICATIONS` permission declared
- [x] Release signing config (`keystore.properties` or debug fallback)
- [x] CI uploads **release** APK artifact
- [ ] Owner device check: notification, lock screen, BT headset buttons
- [ ] Runtime notification permission prompt (Android 13+)
- [ ] Permanent brand PNG in-repo

## Phase 3 — Catalog (started)

- [x] `CatalogPodcast` / `CatalogEpisode` models
- [x] `CatalogRepository` sample feed
- [x] Explore screen lists sample AI podcasts
- [ ] Real API + streaming URLs
- [ ] Download-to-offline

## Phase 4 — Publishing

- [ ] Upload workflow for AI episodes
