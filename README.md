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
| Now Playing (seek, ±15s, waveform, speed) | Done |
| Mini Player + bottom navigation | Done |
| Explore / Profile / Settings | Done |
| Room persistence + episode delete | Done |
| RTL layout | Done |
| Media3 background service | Done |
| Adaptive app icon | Done |
| Gradle Wrapper | Done (bootstrap JAR if needed) |
| Hosted catalog / streaming | Planned |
| Vazirmatn bundled font | Planned |

## Build

```bash
# First clone — fetch wrapper JAR if missing
sh scripts/bootstrap-wrapper.sh

./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
CI also uploads Artifact `auralis-debug-apk` on each green `main` run.

## Device test checklist

1. Install debug APK from CI Artifacts (`auralis-debug-apk`).
2. Confirm launcher shows the Auralis icon.
3. Open **کتابخانه** → empty state → **افزودن فایل صوتی**.
4. Pick an MP3/M4A → playback starts → Now Playing opens.
5. Pause / resume from Now Playing and Mini Player.
6. Seek; use −۱۵ / +۱۵; change speed chips.
7. Long-press an episode → delete confirm.
8. Leave app / lock screen → notification controls.
9. Force-stop and reopen → library + position restore.

## Documentation

- [Product scope](docs/PRODUCT.md)
- [Brand guide](docs/BRAND.md)
- [Design system](docs/DESIGN-SYSTEM.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)

## License

No license has been selected yet.
