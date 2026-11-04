# Android Auto (media)

Auralis targets the standard **media** path for Android Auto (not a custom Car App Library UI).

## Declared in the app

- `res/xml/automotive_app_desc.xml` with `<uses name="media" />`
- Application meta-data `com.google.android.gms.car.application`
- `PlaybackService` intent-filters:
  - `androidx.media3.session.MediaSessionService`
  - `android.media.browse.MediaBrowserService` (compatibility)

## Current limitation (issue #39 scope)

Declaring Auto support does **not** by itself provide an in-car browse tree.

Until `PlaybackService` is upgraded to **MediaLibraryService** with a library callback:

- The app may appear (or be discoverable) as a media session source depending on OS/Auto version.
- Selecting episodes from a full in-car hierarchy is **not** complete.

Follow-up work:

- #40 — upgrade to `MediaLibraryService`
- #41 — browse root + Library episodes from Room
- #42 — downloads / recent + artwork for Auto
- #43 — DHU test checklist
- #44 — Play Console readiness

Tracker: #38
