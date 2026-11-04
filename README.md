# Auralis

**Auralis** is an offline-first Android audio player for AI-generated podcasts and local audio.

## Current development baseline

- App version: **0.2.1** (versionCode **3**)
- Application ID: **com.auralis.app**
- Minimum Android: API 26; compile/target SDK: 36/36
- Room schema: version **3**
- CI Debug and Release APKs are internal development/QA artifacts. Without owner-provided signing configuration, the Release variant falls back to the debug signing configuration and is **not production-signed**.
- Catalog refresh currently reads a static sample/test JSON file from this repository's raw GitHub URL; this is not a production API/backend or managed hosting service.

## Implemented capabilities

| Area | Evidence-based status |
|---|---|
| Local media import and offline playback | Implemented; Room-backed Library |
| Playback position persistence | Implemented; regression-tested |
| Media3 background playback and system controls | Implemented in code; physical-device validation remains owner-run |
| Android Auto media declaration | Manifest + `automotive_app_desc` (see [docs/android-auto.md](docs/android-auto.md)); in-car browse requires MediaLibraryService |
| Catalog browse and stream | Sample Catalog; no production hosting |
| Play, Add to Library and Download | Separate actions and persistence semantics |
| Private offline downloads | Source-aware storage, retry/cancel and partial-file cleanup |
| Library membership vs downloaded-file ownership | Explicitly separated |
| Cross-layer regression | Robolectric + Room + file-backed test store + local HTTP coverage |
| CI APKs and Room schema artifact | Built by Android CI |

## Build and test

Run from the repository root: bootstrap the Gradle wrapper, then execute ./gradlew testDebugUnitTest, ./gradlew :app:assembleDebug and ./gradlew :app:assembleRelease.

CI also runs scripts/install-brand-icons.sh before assembling APKs. That script may use a remote icon fallback; see [Asset and license audit](docs/ASSET-LICENSE-AUDIT.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Stage status and evidence](docs/STAGE-STATUS.md)
- [MVP and regression test plan](docs/MVP-TEST-PLAN.md)
- [Release, signing, install and rollback](docs/RELEASE.md)
- [Android Auto](docs/android-auto.md)
- [Asset and license audit](docs/ASSET-LICENSE-AUDIT.md)

## License

The repository declares **All Rights Reserved** for project-owned code and assets; see [LICENSE](LICENSE). This declaration does not replace the licenses or permissions required for third-party dependencies, fonts, remote artwork, sample audio or other external content. Unverified rights are tracked explicitly in the asset audit.
