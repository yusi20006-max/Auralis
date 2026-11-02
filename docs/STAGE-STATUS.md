# Auralis stage status — evidence based

Last updated for Issue #14 against main merge SHA `37893a28835aa4ed879641254520b994b3a363c5` (2026-11-02).

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
| Latest main SHA | 37893a28835aa4ed879641254520b994b3a363c5 |

## Phase status

| Issue | Area | Status / evidence |
|---|---|---|
| #26 | Preserve offline files | **MERGED** — PR #32 |
| #27 | Source-aware download concurrency | **MERGED** — PR #33 |
| #28 | Catalog parser and HTTP tests | **MERGED** — PR #34 |
| #29 | Catalog UI and explicit Library membership | **MERGED** — PR #35; Room schema v3 |
| #30 | Integrated Catalog/Library/Download/Playback regression | **MERGED** — PR #30; post-merge Android CI PASS on its merge SHA |
| #31 | Documentation and Release readiness | **MERGED** — PR #36; Issue #31 closed |
| #14 | Catalog API contract and infrastructure decision | **IN PROGRESS — design only; PR #37 under review; no provider selected and no service deployed** |
| #15 | Hosted Catalog, streaming and offline download | **BLOCKED** pending explicit approval of #14 design and cost |
| #16 | Publishing workflow and content operations | **NOT STARTED**; follows #15 |

## Issue #31 merge evidence

- PR #36: https://github.com/yusi20006-max/Auralis/pull/36
- Squash merge commit and current main SHA: `37893a28835aa4ed879641254520b994b3a363c5`.
- Issue #31 closed as completed.
- PR-head Android CI [#41414891161](https://github.com/yusi20006-max/Auralis/actions/runs/41414891161): SUCCESS on PR head SHA `9b4621b43aaf37304c549dacfab92dd60ef31b7f`.
- Codex review summary on the same PR head: completed, no findings; no unresolved review threads.
- Post-merge Android CI [#41416512537](https://github.com/yusi20006-max/Auralis/actions/runs/41416512537): **SUCCESS** on merge SHA `37893a28835aa4ed879641254520b994b3a363c5`.

## Issue #14 design boundary

The design artifact is [CATALOG-API-DECISION.md](CATALOG-API-DECISION.md), under review in [PR #37](https://github.com/yusi20006-max/Auralis/pull/37). It defines a provider-neutral v1 JSON contract, read-only resources, immutable media URLs, HTTP cache/range/error behavior, identity rules, security/privacy controls, backup/retention proposals and indicative provider cost scenarios.

No provider has been selected. No production account, bucket, database, domain, paid plan or deployment has been created. Issue #15 remains gated until the owner explicitly approves the final design and cost ceiling.

## Issue #30 verification boundary

The merged regression suite contains seven cross-layer tests using Robolectric, Room, file-backed test storage, the production PrivateDownloadStore, and a local HTTP server. Covered cases include browse/Play/Add/reopen, download failure/retry/delete, cancellation/retry, incomplete HTTP transfer cleanup, low-storage guard, same remote ID across sources, and local import.

Evidence:
- PR CI run [#41382405432](https://github.com/yusi20006-max/Auralis/actions/runs/41382405432): SUCCESS on tested head SHA `994a73d1cdc81177bb8334b50f5e13ceedf92e90`.
- Push CI run [#41382400831](https://github.com/yusi20006-max/Auralis/actions/runs/41382400831): SUCCESS on the same tested head SHA.
- Post-merge Android CI [#41382934207](https://github.com/yusi20006-max/Auralis/actions/runs/41382934207): SUCCESS on merge SHA `0c29ccd48f43a2373c7035280a74f2bcae62253b`.
- GitHub Pages workflow [#41382933290](https://github.com/yusi20006-max/Auralis/actions/runs/41382933290): SUCCESS.
- Codex review: completed with no findings; no unresolved review threads were reported.

These tests do **not** establish Media3 decoding, real network-loss behavior during active playback, Android background playback on a physical device, or full device E2E.

## Signing and distribution status

- .github/workflows/android-ci.yml builds and uploads auralis-debug-apk, auralis-release-apk and auralis-room-schemas artifacts.
- The workflow step is explicitly named “Build signed release APK (debug keystore fallback)”. Gradle uses the configured release signing config only when root keystore.properties exists; otherwise it falls back to Debug signing. No CI secret-to-keystore setup is present in this workflow.
- Therefore, absent owner-provided signing configuration, the CI Release APK is **debug-signed and not suitable as a production release**.
- No production keystore or GitHub signing secret was created or inspected for this work.
- Public distribution and Play Store publication are out of scope.

## Known limitations / owner-run checks

- Physical-device and Emulator E2E: **NOT RUN**.
- Media3 codec/device compatibility matrix: **NOT RUN**.
- Production keystore and production-signed APK: **NOT CONFIGURED / NOT VERIFIED**.
- Hosted production Catalog/CDN: **NOT CONFIGURED**; current Catalog is sample/test only.
- Remote brand icon fallback and provenance: see [Asset and license audit](ASSET-LICENSE-AUDIT.md); permission evidence remains outstanding.
- External artwork/sample media rights: not fully evidenced by repository LICENSE; unresolved items are explicitly flagged in the asset audit.

## Current phase

Issue #14 is limited to API design, architecture trade-offs and cost estimation. Production hosting, paid services, accounts, deployments and public release remain unauthorized until explicit owner approval.
