# Auralis Roadmap

## Current baseline

- App version 0.2.1, versionCode 3.
- Room schema version 3; migrations 1→2 and 2→3 are implemented.
- Latest main commit for the Issue #31 merge: `37893a28835aa4ed879641254520b994b3a363c5`.
- PR #36 was squash-merged and Issue #31 closed on 2026-11-02.
- Post-merge Android CI [#41416512537](https://github.com/yusi20006-max/Auralis/actions/runs/41416512537): **SUCCESS** on the exact main merge SHA above.
- The Android CI workflow builds the Release APK with a debug-keystore fallback; this artifact is not production-signed.
- Emulator and physical-device E2E playback: **NOT RUN**.

## Completed implementation phases

1. **#26 — Offline file ownership:** merged in PR #32. Library membership removal is separate from explicit downloaded-file deletion.
2. **#27 — Source-aware download concurrency:** merged in PR #33. State, progress, errors, cancellation and mutexes use source identity.
3. **#28 — Catalog parser and HTTP behavior:** merged in PR #34. Parser and repository tests cover valid/invalid JSON, local HTTP responses and failure behavior.
4. **#29 — Catalog UI and explicit Library action:** merged in PR #35. Play, Add to Library and Download have independent semantics; Room schema v3 records membership.
5. **#30 — Integrated Catalog/Library/Download/Playback regression:** merged in PR #30. Seven cross-layer tests cover browse/Play/Add/reopen, download failure/retry/delete, cancellation/retry with the production download store and local HTTP, truncated transfer cleanup, low-storage guard, source collision and local import. Robolectric/Room tests do not claim Media3 device E2E.
6. **#31 — Documentation, versioning and Release readiness:** merged in PR #36; Issue #31 closed. The release guide, architecture, roadmap, stage status, test plan and asset/license audit were aligned to the code. Unresolved asset rights, debug signing and device-test limitations remain explicit.

## Current phase

7. **#14 — Hosted Catalog API and infrastructure decision (DESIGN ONLY): IN PROGRESS**
   - Define the versioned API and metadata contract.
   - Specify cache, HTTP error, immutable media URL, range request and integrity behavior.
   - Compare static JSON hosting and object-storage candidates with transparent assumptions and cost scenarios.
   - Record security, privacy, backup and retention requirements.
   - Do not select or activate a provider, create accounts/buckets/databases, buy a domain, deploy or incur charges without explicit owner approval.

The design proposal is [Catalog API v1 and Infrastructure Decision](CATALOG-API-DECISION.md), under review in [PR #37](https://github.com/yusi20006-max/Auralis/pull/37).

## Later phases — gated by Issue #14 approval

8. **#15 — Hosted Catalog, streaming and offline download.** May start only after the owner explicitly approves the API design, provider, cost ceiling and operational constraints.
9. **#16 — Publishing workflow and content operations.** Follows #15; requires a secure server-side publishing path, backup/restore and rollback.

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
