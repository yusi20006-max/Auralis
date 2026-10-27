# Auralis Roadmap

## Phase 0 — Project foundation

- [x] Create the independent Auralis repository.
- [x] Define initial product scope.
- [x] Record provisional brand direction.
- [x] Outline offline-first architecture.
- [ ] Add the approved logo source asset and document its origin.
- [ ] Inspect YasinCast source, issues, and tests as a reuse candidate.
- [ ] Decide final Android application ID before public distribution.
- [ ] Create a baseline Android build and CI workflow.

## Phase 1 — Offline MVP

- [ ] Import local audio through Android's file picker.
- [ ] Build the library and episode information model.
- [ ] Implement persistent playback service and media session.
- [ ] Implement player screen and essential controls.
- [ ] Persist playback position and recent history.
- [ ] Apply the Auralis design system and Persian RTL layout.
- [ ] Add accessibility, error handling, and automated tests.
- [ ] Test on a real Android device, including screen-off playback.

## Phase 2 — Hardening

- [ ] Test small screens, large fonts, orientation changes, and TalkBack.
- [ ] Test Bluetooth/headset controls, audio focus, interruptions, and notification behavior.
- [ ] Measure startup, scrolling, memory use, and playback responsiveness.
- [ ] Produce a signed test build and document installation.

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
