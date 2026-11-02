# Auralis stage status — evidence based

Last updated for Issue #31 against main baseline 0c29ccd48f43a2373c7035280a74f2bcae62253b (2026-11-02).

## Current build metadata

| Item | Verified value |
|---|---|
| Application ID | com.auralis.app |
| versionName | 0.2.1 |
| versionCode | 3 |
| minSdk | 26 |
| compileSdk / targetSdk | 36 / 36 |
| Room schema | 3 |
| Room runtime/compiler | 2.7.2 |
| Room migrations | 1→2, 2→3 |
| Latest main SHA | 0c29ccd48f43a2373c7035280a74f2bcae62253b |

## Phase status

| Issue | Area | Status / evidence |
|---|---|---|
| #26 | Preserve offline files | **MERGED** — PR #32 |
| #27 | Source-aware download concurrency | **MERGED** — PR #33 |
| #28 | Catalog parser and HTTP tests | **MERGED** — PR #34 |
| #29 | Catalog UI and explicit Library membership | **MERGED** — PR #35; Room schema v3 |
| #30 | Integrated Catalog/Library/Download/Playback regression | **MERGED** — PR #30; post-merge Android CI PASS |
| #31 | Documentation and Release readiness | **IN PROGRESS** |

## Issue #30 verification boundary

The merged regression suite contains seven cross-layer tests using Robolectric, Room, file-backed test storage, the production PrivateDownloadStore, and a local HTTP server. Covered cases include browse/Play/Add/reopen, download failure/retry/delete, cancellation/retry, incomplete HTTP transfer cleanup, low-storage guard, same remote ID across sources, and local import.

Evidence:
- PR CI run [#41382405432](https://github.com/yusi20006-max/Auralis/actions/runs/41382405432): SUCCESS on tested head SHA 994a73d1cdc81177bb8334b50f5e13ceedf92e90.
- Push CI run [#41382400831](https://github.com/yusi20006-max/Auralis/actions/runs/41382400831): SUCCESS on the same tested head SHA.
- Post-merge Android CI [#41382934207](https://github.com/yusi20006-max/Auralis/actions/runs/41382934207): SUCCESS on merge SHA 0c29ccd48f43a2373c7035280a74f2bcae62253b.
- GitHub Pages workflow [#41382933290](https://github.com/yusi20006-max/Auralis/actions/runs/41382933290): SUCCESS.
- Codex review: completed with no findings; no unresolved review threads were reported.

These tests do **not** establish Media3 decoding, real network-loss behavior during active playback, Android background playback on a physical device, or full device E2E.

## Signing and distribution status

- CI builds Debug and Release APK artifacts.
- In app/build.gradle.kts, the Release build uses the configured release signing config only when root keystore.properties exists; otherwise it falls back to the Debug signing config.
- Therefore, absent owner-provided signing configuration, the CI Release APK is **debug-signed and not suitable as a production release**.
- No production keystore or GitHub signing secret was created or inspected for this work.
- Public distribution and Play Store publication are out of scope.

## Known limitations / owner-run checks

- Physical-device and Emulator E2E: **NOT RUN**.
- Media3 codec/device compatibility matrix: **NOT RUN**.
- Production keystore and production-signed APK: **NOT CONFIGURED / NOT VERIFIED**.
- Hosted production Catalog/CDN: **NOT CONFIGURED**; current Catalog is sample/in-app.
- Remote brand icon fallback and provenance: see [Asset and license audit](ASSET-LICENSE-AUDIT.md); permission evidence remains outstanding.
- External artwork/sample media rights: not fully evidenced by repository LICENSE; unresolved items are explicitly flagged in the asset audit.

## Current phase

Issue #31 is updating documentation, release procedures and asset provenance. No production release is authorized by this phase.
