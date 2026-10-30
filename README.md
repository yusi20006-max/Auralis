# Auralis

**Auralis** — offline-first Android player for AI-generated podcasts.

## Status: **v0.1.0**

| Area | Status |
|------|--------|
| Library import + delete + Room | Done |
| Now Playing / Mini Player / speed | Done |
| Explore stream + private download | Done |
| Background Media3 + system controls | Done |
| RTL + Vazirmatn | Done |
| CI APKs (debug + release) | Done |
| Brand icon (CI install script) | Done |
| Hosted API / real CDN | Later (#14+) |
| Store license / production keystore | Owner (#13) |

See [docs/RELEASE.md](docs/RELEASE.md) for install, upgrade, and signing notes.

### Build

```bash
sh scripts/bootstrap-wrapper.sh
./gradlew :app:assembleDebug
```

CI also runs `scripts/install-brand-icons.sh` before assemble.

## Documentation

- [Release](docs/RELEASE.md) · [Roadmap](docs/ROADMAP.md)
- [Product](docs/PRODUCT.md) · [Brand](docs/BRAND.md) · [Design system](docs/DESIGN-SYSTEM.md) · [Architecture](docs/ARCHITECTURE.md)

## License

Not selected yet (owner decision).
