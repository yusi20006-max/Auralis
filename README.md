# Auralis

**Auralis** is an offline-first Android app for listening to AI-generated podcasts.

## Offline MVP milestone (ready for one-shot device check)

| Area | Status |
|------|--------|
| Library import + delete | Done |
| Now Playing (seek, ±15s, waveform, speed) | Done |
| Mini Player + bottom nav | Done |
| Explore / Profile / Settings | Done |
| Room persistence | Done |
| RTL + Vazirmatn (Google Fonts) | Done |
| Playback error banner | Done |
| Adaptive icon (vector interim) | Done |
| Gradle Wrapper | Done |
| Official PNG brand icon | Optional polish |
| Hosted catalog | Phase 3 |

### Batch test checklist

1. Install latest green CI Artifact `auralis-debug-apk`.
2. Launcher icon visible (vector brand mark).
3. Empty library → add audio → plays → Now Playing.
4. Top bar (بستن / سرعت) usable below status bar; speed shows `1x` not `x.1`.
5. Seek, ±۱۵, speed chips; mini player pause/resume.
6. Long-press episode → delete.
7. Kill app → reopen → list + position.
8. Persian typeface (Vazirmatn once fonts download on device).
9. Force a bad/missing file if possible → error banner + «افزودن دوباره».

### Build

```bash
sh scripts/bootstrap-wrapper.sh
./gradlew :app:assembleDebug
```

## Documentation

- [Roadmap](docs/ROADMAP.md)
- [Product](docs/PRODUCT.md) · [Brand](docs/BRAND.md) · [Design system](docs/DESIGN-SYSTEM.md) · [Architecture](docs/ARCHITECTURE.md)

## License

No license has been selected yet.
