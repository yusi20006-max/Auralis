# Android Auto (media)

Auralis targets the standard **media** path for Android Auto (not a custom Car App Library UI).

## Declared in the app

- `res/xml/automotive_app_desc.xml` with `<uses name="media" />`
- Application meta-data `com.google.android.gms.car.application`
- `PlaybackService` intent-filters:
  - `androidx.media3.session.MediaSessionService`
  - `android.media.browse.MediaBrowserService` (compatibility)

## MediaLibraryService

`PlaybackService` extends **MediaLibraryService** and exposes a **MediaLibrarySession**.

### Browse tree (P1)

| mediaId | Type | Content |
|---------|------|--------|
| `auralis_root` | browsable | Root |
| `auralis_library` | browsable | Folder title «کتابخانه» |
| `episode:{id}` | playable | Episodes with `isInLibrary = 1` from Room |

Play resolves `localUri` first, then `audioUrl`. Browse/play does **not** create Library membership (see architecture #24).

### Remaining work

- #42 — downloads / recent + artwork for Auto
- #43 — DHU test checklist
- #44 — Play Console readiness

Tracker: #38
