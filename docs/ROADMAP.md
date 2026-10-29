# Auralis Roadmap

## Phase 0 — Project foundation

- [x] Create the independent Auralis repository.
- [x] Define initial product scope and brand direction.
- [x] Baseline Android build + CI.
- [x] Gradle Wrapper bootstrap.
- [x] Launcher icon pipeline (owner brand mark installed in CI).
- [ ] Commit logo PNG permanently under `scripts/icon-assets/` (replace temp URL).
- [ ] Final applicationId before public store release.

## Phase 1 — Offline MVP ✅

- [x] Local import, Room library, Media3 session, Now Playing, Mini Player.
- [x] Speed control, delete episode, RTL, Vazirmatn, error banner.
- [x] Safe-area top bar / LTR speed labels.

## Phase 2 — Hardening (in progress)

- [x] Audio focus handling + pause on becoming noisy (headset unplug).
- [x] Local wake lock while playing (`WAKE_MODE_LOCAL`).
- [ ] Device matrix: small screens, large fonts, rotation, TalkBack.
- [ ] Bluetooth / headset media buttons verification.
- [ ] Notification + lock-screen control verification on real devices.
- [ ] Signed release/test APK + install notes.
- [ ] Permanent brand PNG in-repo (no temp CDN).

## Phase 3 — Hosted catalog

- [ ] Catalog API, browse, stream, offline download.

## Phase 4 — Publishing

- [ ] AI episode upload / admin workflow when needed.
