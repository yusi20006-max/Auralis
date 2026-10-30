# Auralis v0.1.0 — release notes (internal / experimental)

**versionName:** `0.1.0`  
**versionCode:** `1`  
**applicationId:** `com.auralis.app` (debug suffix `.debug`)  
**Git tag (create on GitHub):** `v0.1.0` → pin to the commit that contains this file and `LICENSE`

## Tag this release (owner, one-time)

In the repo on GitHub:

1. **Releases** → **Draft a new release**
2. Tag: `v0.1.0` (create new tag on `main`)
3. Title: `Auralis v0.1.0`
4. Paste summary from «What is included» below
5. Publish

Or CLI (with auth):

```bash
git fetch origin main
git tag -a v0.1.0 <main-sha> -m "Auralis v0.1.0"
git push origin v0.1.0
```

## What is included

- Offline library (import, delete, Room persistence, playback position)
- Media3 background playback (notification, lock screen, audio focus, becoming-noisy)
- Now Playing, Mini Player, speed control, seek ±15s
- Explore sample catalog: stream + private offline download (sandbox)
- Persian RTL UI, Vazirmatn via Google Fonts
- Brand launcher icon install in CI (`scripts/install-brand-icons.sh`)
- Error messages for bad URI / Media3 failures
- **LICENSE:** All Rights Reserved (proprietary)

## Build artifacts (CI)

From green **Android CI** on `main`:

| Artifact | Use |
|----------|-----|
| `auralis-debug-apk` | Daily install / QA |
| `auralis-release-apk` | Debug-keystore fallback unless `keystore.properties` is set |

### Local release with your keystore

1. Copy `keystore.properties.example` → `keystore.properties` (do not commit secrets).
2. Point `storeFile` at your `.jks` / `.keystore`.
3. `./gradlew :app:assembleRelease`

## Install

```text
adb install -r path/to/app-debug.apk
```

## Upgrade / rollback

- Same `applicationId`: `adb install -r`
- Room DB version **1**; clear app data resets library and private downloads
- Rollback: older CI APK with same applicationId

## License

**All Rights Reserved** — see [LICENSE](../LICENSE).

## Out of scope for v0.1

- Production Play Store keystore in GitHub Secrets
- Hosted API / real CDN (#14–#16)
- Playback queue, Search, Onboarding, at-rest encryption
- Branch protection (GitHub settings → owner)
