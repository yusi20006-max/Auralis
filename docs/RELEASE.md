# Auralis v0.2.1 — internal release candidate notes

This document describes the current CI/build baseline. It does **not** announce a public or production release.

## Build metadata

| Field | Value |
|---|---|
| versionName | 0.2.1 |
| versionCode | 3 |
| applicationId | com.auralis.app |
| Debug application ID | com.auralis.app.debug |
| minSdk | 26 |
| compileSdk / targetSdk | 36 / 36 |
| Room schema | 3 |
| Room runtime/compiler | 2.7.2 |

Sources of truth: app/build.gradle.kts, app/src/main/java/com/auralis/app/data/db/AuralisDatabase.kt, and app/schemas/com.auralis.app.data.db.AuralisDatabase/3.json.

## Included functionality

- Local media import and Room-backed Library.
- Media3 playback integration, background service and system controls (device validation remains outstanding).
- Sample Catalog browse and stream.
- Separate Play, Add to Library and Download behavior.
- Source-aware private downloads, retry/cancellation and partial-file cleanup.
- Explicit distinction between Library membership and downloaded-file ownership.
- Integrated Catalog/Library/Download/Playback regression coverage.

Catalog refresh currently reads the static sample/test JSON at the repository's raw GitHub URL (docs/catalog.json). This is not a production API, managed publishing service or production CDN.

## CI artifacts and signing

The .github/workflows/android-ci.yml workflow runs :app:testDebugUnitTest, uploads auralis-room-schemas, assembles Debug/Release APKs, and uploads auralis-debug-apk and auralis-release-apk. Its Release build step is explicitly named “Build signed release APK (debug keystore fallback)”.

The Release build configuration is conditional:
- If root keystore.properties exists, the configured release signing values are used.
- If it does not exist, the Release build falls back to the Debug signing configuration.

Consequently, a CI *-release.apk built without owner-provided signing configuration is **debug-signed**. It is useful for internal QA only and must not be labeled production-signed or uploaded as a production release.

No keystore, signing secret or credential is created, requested, exposed or committed by this procedure.

## Build

Run scripts/bootstrap-wrapper.sh, then:
- ./gradlew testDebugUnitTest
- ./gradlew :app:assembleDebug
- ./gradlew :app:assembleRelease

CI runs scripts/install-brand-icons.sh before assembly. Its remote fallback and provenance are documented in [Asset and license audit](ASSET-LICENSE-AUDIT.md).

## Install

For an internal Debug APK: adb install -r path/to/app-debug.apk

The Debug variant has application ID suffix .debug and can coexist with the non-debug application ID.

## Upgrade and database compatibility

- Install an update with the same application ID using adb install -r.
- Current Room schema is version 3.
- Supported in-app migrations are 1→2 and 2→3; the latter adds isInLibrary with default value 1 to preserve existing membership.
- The app's normal migration path is forward-only. Installing an older APK against a newer Room database may fail because no downgrade migration is defined.
- Do **not** clear app data as a rollback shortcut: that can delete the local database and app-private downloaded media.
- Before attempting rollback, preserve user-owned media outside the app and confirm a tested backup/restore path. A safe database downgrade procedure is not currently provided.

## Rollback

1. Stop distribution of the candidate build.
2. Preserve the candidate APK, its SHA-256, CI run URL and logs.
3. Do not uninstall or clear app data if local downloads/library data must be retained.
4. Do not install an older version over a schema-v3 database unless a compatible downgrade/restore procedure has been implemented and tested.
5. Record the affected app version, database version, device and failure evidence; prepare a forward-fix or a separately tested data restore.

## Release checklist

- [ ] Candidate commit and main SHA are recorded.
- [ ] CI passes on the exact candidate SHA.
- [ ] Post-merge CI passes on the exact main merge SHA.
- [ ] Debug and Release APK artifacts are present.
- [ ] Signing certificate identity is inspected; debug fallback is clearly labeled.
- [ ] Room schema artifact matches the committed schema JSON.
- [ ] Device checks are completed or explicitly marked NOT RUN.
- [ ] Asset/license audit has no unresolved permission represented as approved.
- [ ] Install/upgrade/rollback risks are reviewed.
- [ ] No keystore or secret is committed or disclosed.
- [ ] Public release, Play Store and production signing have separate explicit owner approval.

## License and distribution

The repository declares All Rights Reserved for project-owned materials; see [LICENSE](../LICENSE). Third-party libraries, fonts, remote artwork and sample content remain subject to their own terms. See [Asset and license audit](ASSET-LICENSE-AUDIT.md).

Public distribution, Play Store publication, production signing and production hosting are outside this Issue and require separate approval.
