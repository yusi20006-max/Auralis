# Auralis Roadmap

## Current baseline

- App version 0.2.1, versionCode 3.
- Room schema version 3; migrations 1→2 and 2→3 are implemented.
- Latest main baseline before Issue #31: 0c29ccd48f43a2373c7035280a74f2bcae62253b.
- Post-merge Android CI run [#41382934207](https://github.com/yusi20006-max/Auralis/actions/runs/41382934207) succeeded on the Issue #30 merge SHA; GitHub Pages run [#41382933290](https://github.com/yusi20006-max/Auralis/actions/runs/41382933290) also succeeded.
- The Android CI workflow explicitly builds the Release APK with a debug-keystore fallback; CI labels this step accordingly. This artifact is not production-signed.
- Emulator and physical-device E2E playback: **NOT RUN**.

## Completed implementation phases

1. **#26 — Offline file ownership:** merged in PR #32. Library membership removal is separate from explicit downloaded-file deletion.
2. **#27 — Source-aware download concurrency:** merged in PR #33. State, progress, errors, cancellation and mutexes use source identity.
3. **#28 — Catalog parser and HTTP behavior:** merged in PR #34. Parser and repository tests cover valid/invalid JSON, local HTTP responses and failure behavior.
4. **#29 — Catalog UI and explicit Library action:** merged in PR #35. Play, Add to Library and Download have independent semantics; Room schema v3 records membership.
5. **#30 — Integrated Catalog/Library/Download/Playback regression:** merged in PR #30. Seven cross-layer tests cover browse/Play/Add/reopen, download failure/retry/delete, cancellation/retry with the production download store and local HTTP, truncated transfer cleanup, low-storage guard, source collision and local import. Robolectric/Room tests do not claim Media3 device E2E.

## Current phase

6. **#31 — Documentation, versioning and Release readiness (P2): IN PROGRESS**
   - Align README, architecture, roadmap, stage status, test plan and release guide with current code.
   - Record app metadata and Room schema/migrations.
   - Distinguish CI Debug/Release APK signing from production signing.
   - Audit project and third-party asset/license provenance; flag unknown permissions.
   - Document install, upgrade, rollback and known limitations.
   - Require CI green on the final PR SHA and after merge.

## Later phases — not authorized by this documentation issue

7. **#14 — Hosted Catalog API and infrastructure decision.** Define versioned API, metadata, cache/error semantics and hosting trade-offs. Provider selection and cost require explicit owner approval.
8. **#15 — Hosted Catalog, streaming and offline download.** Depends on #14 and approved infrastructure.
9. **#16 — Publishing workflow and content operations.** Requires a secure server-side publishing path, backup/restore and rollback.

## Project completion vs production release

“Release readiness” for the current offline-first milestone means phases #26–#31 have implementation, test and CI evidence on the final main SHA, with unresolved device and asset-rights limitations clearly recorded.

It does **not** mean production signing, public release, Play Store publication, production hosting, or approval of paid infrastructure. Those require separate owner decisions.

## Execution rules

- Follow: Inspect → Implement → Test → Commit → Push → PR → CI → Review → Merge → Final Verify.
- Re-check main SHA, issue scope, PRs and CI before changes.
- A PASS applies only to the exact SHA tested.
- Keep PR draft until acceptance criteria and CI evidence are ready.
- Do not infer physical-device results from CI or Robolectric.
- Do not create, expose or commit keystores/secrets.
- Do not enable paid services or publish publicly without explicit approval.
