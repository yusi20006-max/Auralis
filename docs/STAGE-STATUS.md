# Auralis stage status (evidence-based)

Last updated against `main` after P0 CI fix, PR #25 close, and playback source-priority hardening.

## Priority board

| Priority | Item | Status |
|----------|------|--------|
| P0 | Fix legacy download URI test + green CI | **PASS** — `cadd61f` CI [run 41269222769](https://github.com/yusi20006-max/Auralis/actions/runs/41269222769) success |
| P1 | Close PR #25 (no merge) | **PASS** — closed 2026-11-01 (superseded by main) |
| P2 | Playback offline/online hardening | **PARTIAL** — source priority local → private download → stream; unit tests added; device checklist still owner-run |
| P3 | Hosted HTTP catalog | **BLOCKED** on hosting decision |
| P4 | Store release | **BLOCKED** on owner keystore |

## Stage 1 — Data contracts

**PASS / VERIFIED**

## Stage 2 — Episode model + Room migration

**PASS / VERIFIED** on CI (schema v2 committed, safe upsert, migration tests).

## Stage 3 — LibraryRepository

**PASS / VERIFIED** — play does not persist; `addToLibrary` / download persist; `privateDownloadUri` helper.

## Stage 4 — PrivateDownloadStore

**PASS / VERIFIED** — composite keys, legacy fallback, resilient content URI (`cadd61f`).

## Stage 5–7 — Catalog

**PARTIAL PASS** — sample in-memory catalog + UI; no hosted HTTP client yet.

## Stage 8 — Playback online/offline

**PARTIAL / code-side hardened**

- Priority: readable `localUri` → private download URI → `audioUrl` stream.
- Media3: audio focus, noisy, wake, MediaSession, task-removed keep-alive retained.
- Device matrix (`docs/ISSUE-12-DEVICE-CHECKLIST.md`): **NOT RUN** (physical device).

## Stage 9 — Delivery

- Unit tests + assemble: CI on green commits
- Device regression: **NOT RUN**
- Real keystore / Play listing: **BLOCKED**

## Continue-from

1. Confirm CI green on latest playback commits.
2. Owner runs device checklist on a debug APK from green main.
3. HTTP catalog only after CDN/API base URL is chosen.
4. Release only with owner keystore.
