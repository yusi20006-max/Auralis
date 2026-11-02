# Auralis — Architecture

## Product direction

Auralis is an offline-first Android audio player. Local media and the local library remain usable without a hosted Catalog. Remote Catalog functionality is an additional source, not a prerequisite for local playback.

## Android stack

- Kotlin, Jetpack Compose and AndroidX.
- AndroidX Media3 ExoPlayer and MediaSession for playback and system controls.
- Android Storage Access Framework for user-selected local media.
- Room for persistent episode/library metadata.
- Repository boundaries isolate UI/state from Catalog and download operations.

The current Android Gradle configuration sets namespace and application ID to **com.auralis.app**, minSdk 26, compileSdk 36, targetSdk 36, Java/Kotlin JVM target 17, versionName **0.2.1**, and versionCode **3**. Room runtime/compiler are **2.7.2**.

## Episode identity

- Internal identity is the stable Room primary key Episode.id; it is not derived from a URL.
- Remote identity is the composite pair (sourceId, remoteId).
- Catalog uses CatalogConfig.SOURCE_ID = catalog:auralis.
- Local-only and legacy rows retain null source identity where appropriate.
- Equal remote IDs from different sources must remain distinct.
- audioUrl and localUri are locations, not identities.

Room has a unique index on (sourceId, remoteId). Upsert behavior must not replace an existing row and erase download or playback state on a composite-key conflict.

## Room persistence and migrations

- Current Room database version: **3**.
- Exported schema: app/schemas/com.auralis.app.data.db.AuralisDatabase/3.json.
- Migration 1→2 adds nullable sourceId and the unique composite index.
- Migration 2→3 adds non-null isInLibrary with default 1, preserving prior Library visibility of existing rows.
- The current entity schema includes isInLibrary; download-only records can exist without becoming Library members.

The committed schema JSON is source-controlled. CI also exports the schema during the Android build; compare both when schema-related code changes.

## Catalog, Library and Download boundaries

- Browsing Catalog does not create a persistent Room row.
- Play/stream alone does not create permanent Library membership.
- Explicit Add to Library creates membership.
- Download can create a persisted download record without Library membership.
- Removing Library membership is distinct from explicitly deleting the private downloaded file.
- Deleting a downloaded copy preserves explicit Library membership when the episode is a Library member.
- Catalog/network failure must not corrupt Room or prevent local/offline playback.

## Private downloads

- Downloaded media is stored in app-private storage by PrivateDownloadStore.
- Known-source storage identity is source-aware; identical remote IDs from different sources map to distinct files.
- Legacy remoteId-only files are handled conservatively; ambiguous files are not automatically assigned to a source.
- Cancellation and failed/incomplete transfers must clean partial .part files.
- The free-space guard uses the real filesystem probe by default; an injectable probe supports deterministic tests.

## Playback and verification boundary

The regression suite verifies local URI resolution and persistence boundaries. It does not prove Media3 decoding, background behavior under every Android vendor policy, or physical-device playback. Keep the device checklist in [MVP-TEST-PLAN.md](MVP-TEST-PLAN.md) explicit and owner-run.

## Current Catalog integration boundary

CatalogRepository starts with in-memory sample data and can refresh JSON through CatalogConfig.catalogUrl. The current default URL is the raw GitHub file docs/catalog.json in this repository. This is a static sample/test catalog, not a production API, managed publishing service, object store or production CDN. The JSON references separately hosted external sample audio URLs. A future production implementation must remain behind the repository boundary, validate metadata/URLs, handle timeout/cache/offline failures, and keep server secrets out of the APK.

## Proposed Catalog API boundary

Issue #14 defines a provider-neutral, public read-only v1 contract and infrastructure trade-offs in [CATALOG-API-DECISION.md](CATALOG-API-DECISION.md). The current sample JSON is not yet compliant with that proposed contract. Implementation and migration of the Android client are not part of this design document and must be handled in the approved follow-up scope.

## Security and operational constraints

- Never embed server/admin secrets in the Android application.
- Never commit a real keystore or signing credentials.
- Validate remote URLs and metadata before use.
- Keep local playback available when remote services are unavailable.
- Production hosting, paid services, production signing and public distribution require separate owner approval.
