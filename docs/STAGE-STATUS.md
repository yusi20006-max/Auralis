# Auralis stage status (evidence-based)

Last updated against `main` after stages 2–4 land and catalog/download tests.

## Stage 1 — Data contracts

**PASS / VERIFIED** in code and docs (`docs/ARCHITECTURE.md`).

- Internal Episode id is Room primary key.
- Remote identity is `(sourceId, remoteId)`.
- Local-only / legacy keep `sourceId = null`.

## Stage 2 — Episode model + Room migration

**PASS / VERIFIED** on CI.

- `sourceId` on model/entity, unique index, `MIGRATION_1_2`, safe upsert.
- Schema v2 at `app/schemas/com.auralis.app.data.db.AuralisDatabase/2.json`.
- Tests: `EpisodeDaoTest`, `AuralisDatabaseMigrationTest`, `EpisodeModelTest`.
- CI example on related commits: success (run series 194–204).

## Stage 3 — LibraryRepository

**PASS / VERIFIED** on CI (`eede63a7` and predecessors).

- Composite lookup; play does not persist; `addToLibrary` + download persist with `CatalogConfig.SOURCE_ID`.
- Tests: `LibraryRepositoryTest`.
- CI head at time of library wiring: [run 41263404055](https://github.com/yusi20006-max/Auralis/actions/runs/41263404055) **success** on `eede63a7`.

## Stage 4 — PrivateDownloadStore

**PASS / VERIFIED** (implementation + unit tests landed).

- Key `sourceId::remoteId`, legacy remoteId-only fallback.
- Tests: composite distinct files, legacy fallback, storageKey (`PrivateDownloadStoreTest`).

## Stage 5–7 — Catalog API / repository / UI

**PARTIAL PASS** (sample catalog, not hosted HTTP API).

- Models + `CatalogRepository` sample HTTPS feed: **PASS**.
- UI Explore + podcast detail + stream/download hooks in `MainActivity`: **PASS** (existing).
- Real hosted API client / pagination / retry: **NOT RUN** (blocked on product hosting decision; sample path is intentional).
- Catalog unit tests: `CatalogRepositoryTest`.

## Stage 8 — Playback online/offline

**PARTIAL**

- Media3 path retained; no intentional PlaybackService rewrite in stages 2–4.
- Offline library + private download URIs still feed the same player path.
- Full device matrix (notification, audio focus, airplane mode resume): **NOT RUN** (requires physical device evidence).

## Stage 9 — Delivery checklist

| Item | Status |
|------|--------|
| Unit/integration tests on main | CI **success** on `eede63a7` |
| Debug/Release assemble in CI | included in Android CI workflow |
| Device regression | **NOT RUN** |
| Store release / real keystore | **BLOCKED** (owner secrets) |
| PR #25 draft | superseded by direct main land; close manually if still open |

## Continue-from

1. Confirm CI green on commits after `eede63a7` (download/catalog test commits).
2. Optional: HTTP `CatalogDataSource` behind repository when CDN exists.
3. Device playback checklist (`docs/ISSUE-12-DEVICE-CHECKLIST.md`).
4. Release keystore + Play listing only with owner approval.
