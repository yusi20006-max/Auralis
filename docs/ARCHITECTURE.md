# Auralis — Architecture

## Guiding decision

Build the Android app so local audio is a first-class source. A future remote catalog is an additional source, not a prerequisite for core playback.

## Initial Android direction

- Kotlin and Jetpack Compose for the native Android UI.
- AndroidX Media3 ExoPlayer for audio playback.
- MediaSessionService for playback that survives UI navigation and supports system media controls.
- Android Storage Access Framework for user-selected local files/folders, using persisted URI permissions where supported.
- Room for structured local data when persistence requirements justify it.
- A repository boundary between UI/state and episode sources.

## Episode identity (implemented)

- **Internal id:** stable primary key in Room (`Episode.id`). Never derived from URL.
- **Remote identity:** composite `(sourceId, remoteId)`.
  - Hosted catalog uses `CatalogConfig.SOURCE_ID = "catalog:auralis"`.
  - Local-only and legacy rows keep `sourceId = null`.
  - Same `remoteId` from different sources must not merge.
- **URLs are not identity:** `audioUrl` and `localUri` are location fields only.

Room enforces a unique index on `(sourceId, remoteId)`. Upsert is primary-key based (`@Insert(ABORT)` / `@Update`) so a composite conflict cannot REPLACE and erase playback/download state.

Database version is **2** with non-destructive `MIGRATION_1_2` (adds nullable `sourceId` + unique index). Schema JSON is exported under `app/schemas/`.

## Library vs Catalog boundary (implemented)

- Browsing the catalog does **not** write Room rows.
- **Play / stream alone** does **not** create permanent library membership (`addOrGetStreamEpisode` returns a transient Episode).
- Permanent membership is created only by explicit **Add to Library** (`addToLibrary`) or **Download** (`downloadEpisodeOffline`).
- Catalog failures must not corrupt Room or interrupt offline library playback.

## Private downloads (implemented)

- Files live under app-private storage (`PrivateDownloadStore`), not public MediaStore.
- Storage key is `sourceId::remoteId` when source is known; legacy remoteId-only files remain readable as fallback.
- Distinct sources with the same remoteId map to distinct files.

## Suggested conceptual layers

1. **Presentation:** Home/library, episode details, player, explore/catalog, settings.
2. **Domain:** Episode identity, playback actions, library operations, and playback state.
3. **Data:** Local file source, Room library, catalog repository (sample/local until hosted API), private download store.
4. **Playback:** Media3 player and media session hosted independently from Compose screens.
5. **Remote integration:** Catalog sample data today; swap to HTTP client behind `CatalogRepository` when CDN/API is ready. Do not embed secrets in the APK.

## Episode model fields

- id, title, podcastName, description, durationMs
- artworkUri, localUri, remoteId, audioUrl, sourceId
- isAiGenerated, lastPositionMs, lastPlayedAt

## Reliability and security

- Never embed server secrets in the Android app.
- Validate remote URLs and metadata before use.
- Handle missing files, expired URLs, network errors, and interrupted downloads.
- Keep local playback available when the server is unreachable.
- Avoid unnecessary analytics and account systems.

## Catalog status

`CatalogRepository` currently serves an in-memory sample feed of HTTPS streams so online playback can be exercised without a backend. When a hosted catalog is available, keep the same repository surface and inject a remote data source; do not couple UI directly to HTTP.
