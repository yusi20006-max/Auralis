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

## Planned phases

1. Define product scope, brand, architecture, and acceptance criteria.
2. Build the offline Android MVP.
3. Validate playback and UI on real Android devices.
4. Add a hosted episode catalog, streaming, and offline downloads.
5. Add publishing/admin workflows only when they are actually needed.

## Documentation

- [Product scope](docs/PRODUCT.md)
- [Brand guide](docs/BRAND.md)
- [Design system](docs/DESIGN-SYSTEM.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)

## Status

Project initialization. The application implementation has not yet been migrated from YasinCast. Existing YasinCast code is a reference to inspect and selectively reuse; it is not automatically considered production-ready for Auralis.

## License

No license has been selected yet.
