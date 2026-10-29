# Auralis Roadmap

## Phase 0 — Project foundation

- [x] Create the independent Auralis repository.
- [x] Define initial product scope.
- [x] Record provisional brand direction.
- [x] Outline offline-first architecture.
- [x] Create a baseline Android build and CI workflow.
- [x] Adaptive app icon (vector; PNG brand asset can still replace).
- [x] Gradle Wrapper scripts + properties (JAR bootstrapped in CI / `scripts/bootstrap-wrapper.sh`).
- [ ] Commit logo source PNG asset and document its origin.
- [ ] Decide final Android application ID before public distribution.

## Phase 1 — Offline MVP  ✅ milestone for batch device test

- [x] Import local audio through Android's file picker.
- [x] Build the library and episode information model (Room).
- [x] Implement persistent playback service and media session (Media3).
- [x] Implement player screen and essential controls (seek, ±15s, speed).
- [x] Persist playback position and library list.
- [x] Apply the Auralis design system and Persian RTL layout.
- [x] Mini player, Explore / Profile / Settings shells.
- [x] Episode delete from library.
- [x] Now Playing safe-area / larger hit targets / LTR speed labels.
- [x] Vazirmatn via Google Fonts downloadable provider.
- [x] Playback error banner with re-import action.
- [ ] Official brand PNG as launcher icon (vector is interim).
- [ ] Stronger TalkBack coverage and large-font stress test.
- [ ] Broader automated UI / integration tests.

## Phase 2 — Hardening

- [ ] Test small screens, large fonts, orientation changes, and TalkBack.
- [ ] Test Bluetooth/headset controls, audio focus, interruptions, and notification behavior.
- [ ] Measure startup, scrolling, memory use, and playback responsiveness.
- [ ] Produce a signed release/test build and document installation.
- [ ] Deeper error recovery when a URI grant is lost or file is missing.

## Phase 3 — Hosted podcast catalog

- [ ] Define catalog API and versioned episode metadata.
- [ ] Select hosting and file storage based on real usage and budget.
- [ ] Add catalog browsing and streaming.
- [ ] Add explicit episode downloads and download-state UI.
- [ ] Verify local playback works without network access.
- [ ] Add authentication only if content access requires it.

## Phase 4 — Publishing workflow

- [ ] Define how AI-generated episodes and artwork are uploaded.
- [ ] Add a minimal publishing process or admin tool if needed.
- [ ] Document backups, retention, monitoring, and operating costs.

## Delivery rule

Each phase should be delivered through reviewable changes with build/test evidence. Do not mark device-level behavior verified based on a successful CI build alone.
