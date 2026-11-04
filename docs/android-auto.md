# Android Auto (media)

Auralis targets the standard **media** path for Android Auto (not a custom Car App Library UI).

Tracker: [#38](https://github.com/yusi20006-max/Auralis/issues/38)

## Declared in the app

- `res/xml/automotive_app_desc.xml` with `<uses name="media" />`
- Application meta-data `com.google.android.gms.car.application`
- `PlaybackService` intent-filters:
  - `androidx.media3.session.MediaSessionService`
  - `android.media.browse.MediaBrowserService` (compatibility)

## MediaLibraryService

`PlaybackService` extends **MediaLibraryService** and exposes a **MediaLibrarySession**.

### Browse tree

| mediaId | Type | Content |
|---------|------|--------|
| `auralis_root` | browsable | Root |
| `auralis_library` | browsable | «کتابخانه» — `isInLibrary = 1` |
| `auralis_downloads` | browsable | «دانلودها» — `localUri IS NOT NULL` |
| `auralis_recent` | browsable | «اخیراً پخش‌شده» — by `lastPlayedAt` |
| `episode:{id}` | playable | Episode leaf |

Play resolves `localUri` first, then `audioUrl`. Metadata includes title, artist (`podcastName`), duration, and `artworkUri` when present.

Browse/play does **not** create Library membership (architecture #24).

## DHU / developer test checklist (#43)

Code-side support is on `main`. **Physical Auto / DHU validation is owner-run** and must not be inferred from CI.

### Setup (developer)

1. Install a debug/release APK built from the SHA under test.
2. On the phone: open Android Auto → enable **Developer mode** (tap version repeatedly if needed).
3. Enable **Unknown sources** for development if required by your Auto version.
4. Start **Head unit server** from Android Auto developer settings.
5. On desktop: run [Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing) and connect over USB/ADB as documented by Google.

### Scenarios

- [ ] Auralis appears in the Auto media apps list (or is selectable as a media source).
- [ ] Root opens; folders کتابخانه / دانلودها / اخیراً پخش‌شده are visible.
- [ ] Opening کتابخانه lists Library episodes when the phone Library is non-empty.
- [ ] Selecting an episode starts playback.
- [ ] Play / pause / seek work from Auto controls.
- [ ] Now-playing shows title (and artwork when URI is readable by Auto).
- [ ] Disconnect/reconnect session does not crash the service.
- [ ] Phone notification / mini-player still control the same playback.

### Result template

```
SHA:
Phone model / Android:
Android Auto version:
DHU version (if used):
Result: PASS | FAIL | BLOCKED
Notes:
```

Until a run is recorded on an issue or PR, treat device Auto testing as **NOT RUN**.

## Play Console readiness (#44)

Pre-submit checklist (no production publish without explicit owner approval):

- [x] Media category declared via `automotive_app_desc`
- [x] MediaLibraryService browse + play path in code
- [ ] DHU/device checklist executed and recorded
- [ ] No driver-distraction custom car UI (we use standard media only)
- [ ] Privacy / data safety forms reflect offline-first + optional network catalog
- [ ] Known limitation documented: full online Catalog browse inside Auto is not in v1 tree
- [ ] Owner approval before any Play track that advertises Android Auto

No keystore, paid service, or public production release is implied by this document.
