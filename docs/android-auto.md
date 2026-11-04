# Android Auto (media)

Auralis targets the standard **media** path for Android Auto (not a custom Car App Library UI).

## Declared in the app

- `res/xml/automotive_app_desc.xml` with `<uses name="media" />`
- Application meta-data `com.google.android.gms.car.application`
- `PlaybackService` intent-filters:
  - `androidx.media3.session.MediaSessionService`
  - `android.media.browse.MediaBrowserService` (compatibility)

## MediaLibraryService (#40)

`PlaybackService` extends **MediaLibraryService** and exposes a **MediaLibrarySession** with a stub callback:

- `onGetLibraryRoot` → browsable root id `auralis_root` (title "Auralis")
- `onGetChildren` → empty list until Library episodes are wired (#41)

Phone playback, notification, and lock-screen controls remain driven by the same ExoPlayer instance and session activity → `MainActivity`.

## Remaining work

- #41 — browse root children: Library episodes from Room + play from Auto
- #42 — downloads / recent + artwork for Auto
- #43 — DHU test checklist
- #44 — Play Console readiness

Tracker: #38
