# Auralis

**Auralis** is an offline-first Android app for listening to AI-generated podcasts.

The first release focuses on a polished, reliable local listening experience. A later phase will connect the app to a hosted catalog of podcast episodes so the same app can discover and stream or download published episodes.

## Product principles

- **Podcast-only:** Auralis is for AI-generated podcasts, not a general-purpose music player.
- **Offline first:** Local playback must work without an account, network connection, or server.
- **Online later:** The future hosted catalog must extend the offline app rather than replace it.
- **Playback reliability:** Background playback, audio focus, notification controls, lock-screen controls, and saved playback position matter more than decorative effects.
- **Persian-first, RTL-aware:** Persian content and right-to-left layouts are first-class; mixed Persian/English metadata must remain readable.
- **Distinctive brand:** Use the Auralis logo and its black, electric-blue, and violet visual identity without copying another player's UI.

## Current MVP status (main)

| Area | Status |
|------|--------|
| Library (import local audio) | Done |
| Now Playing (seek, ±15s, waveform) | Done |
| Playback speed (Now Playing + Settings → Media3) | Done |
| Mini Player + bottom navigation | Done |
| Explore (categories + placeholders) | Done |
| Profile + Settings | Done |
| Room persistence (episodes + position) | Done |
| RTL layout (forced) | Done |
| Media3 background service | Done |
| Hosted catalog / streaming | Planned |
| Vazirmatn bundled font | Planned |

## Device test checklist

1. Install debug APK from CI Artifacts (`auralis-debug-apk`).
2. Open **کتابخانه** → empty state → **افزودن فایل صوتی**.
3. Pick an MP3/M4A → playback starts → Now Playing opens.
4. Pause / resume from Now Playing and from Mini Player.
5. Seek with slider; use −۱۵ / +۱۵.
6. Change speed chips (`0.75x` … `2x`) and confirm audio rate changes.
7. Leave app / lock screen → notification controls still work.
8. Force-stop and reopen → library list and last position restore.
9. Check RTL: titles, bottom bar labels, and buttons align right-to-left.
10. Open **کشف** and **پروفایل** → **تنظیمات** → speed slider matches Now Playing.

## Build

```bash
gradle :app:assembleDebug
# or with wrapper once present:
# ./gradlew :app:assembleDebug
```

CI: `.github/workflows/android-ci.yml` runs unit tests and uploads the debug APK.

## Planned phases

1. Define product scope, brand, architecture, and acceptance criteria.
2. Build the offline Android MVP. ← **in progress / largely complete on main**
3. Validate playback and UI on real Android devices.
4. Add a hosted episode catalog, streaming, and offline downloads.
5. Add publishing/admin workflows only when they are actually needed.

## Documentation

- [Product scope](docs/PRODUCT.md)
- [Brand guide](docs/BRAND.md)
- [Design system](docs/DESIGN-SYSTEM.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)

## License

No license has been selected yet.
