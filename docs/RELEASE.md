# Auralis v0.1.0 — release notes (internal / experimental)

**versionName:** `0.1.0`  
**versionCode:** `1`  
**applicationId:** `com.auralis.app` (debug suffix `.debug`)

## What is included

- Offline library (import, delete, Room persistence, playback position)
- Media3 background playback (notification, lock screen, audio focus, becoming-noisy)
- Now Playing, Mini Player, speed control, seek ±15s
- Explore sample catalog: stream + private offline download (sandbox, not in Files app)
- Persian RTL UI, Vazirmatn via Google Fonts
- Brand launcher icon install in CI (`scripts/install-brand-icons.sh`)
- Error messages for bad URI / Media3 failures

## Build artifacts (CI)

From green **Android CI** on `main`:

| Artifact | Use |
|----------|-----|
| `auralis-debug-apk` | Daily install / QA |
| `auralis-release-apk` | Signed with **debug keystore** unless `keystore.properties` is provided on the builder |

### Local release with your keystore

1. Copy `keystore.properties.example` → `keystore.properties` (do not commit secrets).
2. Point `storeFile` at your `.jks` / `.keystore`.
3. `./gradlew :app:assembleRelease`

Without that file, release APK still builds for internal testing via debug signing (documented fallback in `app/build.gradle.kts`).

## Install

```text
adb install -r path/to/app-debug.apk
```

Or download the CI artifact and open on device (allow unknown sources if needed).

## Upgrade / rollback

- Same `applicationId`: install over previous build (`adb install -r`).
- Room DB version is **1**; wiping app data resets library and private downloads.
- Rollback: install an older CI artifact APK with the same applicationId.

## License

**Not selected.** All rights reserved by the owner until a license is chosen (see Issue #13).

## Out of scope for v0.1

- Production Play Store keystore in GitHub Secrets
- Hosted API / real CDN (#14–#16)
- Playback queue, Search, Onboarding, at-rest encryption
- Branch protection (repo settings — owner)
