# Auralis — MVP and integrated regression test plan

## Purpose and verification boundaries

This plan distinguishes automated code-level verification from Emulator/physical-device acceptance. CI and Robolectric results must never be presented as proof of Media3 playback on a real device.

## Automated checks

Run from the repository root:
- ./gradlew testDebugUnitTest
- ./gradlew :app:assembleDebug
- ./gradlew :app:assembleRelease

The Android CI workflow is expected to execute the unit/Robolectric suite, export and upload the Room schema artifact, assemble Debug and Release APK artifacts, and report results against the exact commit SHA.

For a release candidate, verify that CI belongs to the latest PR head SHA. After merge, verify the independent main-branch run against the merge SHA.

## Regression coverage already present

The Issue #30 cross-layer suite uses Robolectric, Room, file-backed test storage, the production PrivateDownloadStore, and a local HTTP server. It covers:
- Catalog browse and Play without persistent Library membership.
- Explicit Add to Library and playback-position persistence after database reopen.
- Download failure and retry; offline URI resolution; explicit file deletion while retaining Library membership.
- Cancellation and retry, including production download-store partial-file cleanup.
- Incomplete HTTP transfer rejection and .part cleanup.
- Low-storage guard through an injected free-space probe (the production default remains filesystem-based).
- Same remote ID from different sources and local-file import.

## Required Emulator / physical-device checks

Record device model, Android version, app version, APK SHA-256, date, steps and outcome for each check. If not run, mark **NOT RUN**; do not infer PASS.

- [ ] Install the Debug APK on a clean supported device.
- [ ] Import a local MP3 and M4A/AAC file through the Android document picker.
- [ ] Play, pause, seek, resume and stop local media.
- [ ] Lock the screen and verify playback continues.
- [ ] Verify notification, lock-screen and headset/Bluetooth controls.
- [ ] Disconnect wired/Bluetooth audio and verify expected noisy-device behavior.
- [ ] Disable network and verify local playback remains available.
- [ ] Browse sample Catalog; verify Play does not add Library membership.
- [ ] Add to Library; restart the app and verify membership and playback position.
- [ ] Download, cancel, retry, play offline and explicitly delete a downloaded copy.
- [ ] Remove Library membership and verify the downloaded file is retained unless explicitly deleted.
- [ ] Verify behavior under low storage and interrupted network transfer.

## Release candidate checklist

- [ ] All automated tests pass on the exact candidate SHA.
- [ ] Debug and Release APKs build successfully.
- [ ] Room schema artifact matches the committed schema JSON.
- [ ] App version metadata and release notes match Gradle configuration.
- [ ] Signing identity is inspected and documented; debug signing is not represented as production signing.
- [ ] Asset/license audit has no unapproved item represented as cleared.
- [ ] Device checks are recorded or explicitly marked NOT RUN with owner sign-off.
- [ ] Install, upgrade and rollback instructions are reviewed.
- [ ] No secrets, keystore files or credentials are included in source or artifacts.

## Current evidence snapshot (2026-11-02)

Issue #30 post-merge Android CI run [#41382934207](https://github.com/yusi20006-max/Auralis/actions/runs/41382934207) succeeded on merge SHA 0c29ccd48f43a2373c7035280a74f2bcae62253b. GitHub Pages workflow [#41382933290](https://github.com/yusi20006-max/Auralis/actions/runs/41382933290) also succeeded.

Emulator and physical-device E2E remain **NOT RUN**. Production signing and production Catalog hosting remain unconfigured.
