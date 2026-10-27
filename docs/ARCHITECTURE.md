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

These are proposed implementation choices and must be checked against the current project and supported Android versions before coding.

## Suggested conceptual layers

1. **Presentation:** Home/library, episode details, player, history, and bookmarks.
2. **Domain:** Episode identity, playback actions, library operations, and playback state.
3. **Data:** Local file source, local metadata/database, and future remote catalog source.
4. **Playback:** Media3 player and media session hosted independently from Compose screens.
5. **Remote integration (future):** Catalog API client, remote metadata, streaming URLs, and download manager.

## Episode model

Use a stable internal episode ID. Keep local file references separate from future remote IDs and URLs. A conceptual episode record may include:

- id
- title
- description
- durationMs
- artworkUri
- sourceType (local or remote)
- localUri (optional)
- remoteId (optional)
- audioUrl (optional)
- publishedAt (optional)
- lastPositionMs
- lastPlayedAt (optional)

This is a conceptual contract, not a finalized database schema. Validate fields and migrations before implementation.

## Future server contract

Prefer a simple, versioned catalog API that returns episode metadata and stable IDs. Keep audio files and artwork in file/object storage where practical rather than embedding large binaries in API responses. Use standard HTTP range requests if supported by the chosen audio hosting path.

Do not choose a hosting provider until expected storage, bandwidth, privacy, availability, and budget are understood.

## Reliability and security

- Never embed server secrets in the Android app.
- Validate remote URLs and metadata before use.
- Handle missing files, expired URLs, network errors, and interrupted downloads.
- Keep local playback available when the server is unreachable.
- Avoid unnecessary analytics and account systems.
- Add authentication before exposing private or unpublished content.

## Migration from YasinCast

Inspect the current YasinCast implementation and tests first. Reuse only code that meets Auralis requirements, document changes, and verify package/application ID, app label, resources, service declarations, permissions, and tests. Do not copy the old repository wholesale without review.
