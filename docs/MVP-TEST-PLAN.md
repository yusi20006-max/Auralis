# Auralis MVP — acceptance test plan

## Scope
First milestone: select a local audio file and play it using a Media3 session service. No server, accounts, history, bookmarks, or full library UI.

## Automated verification
- [ ] GitHub Actions completes `:app:assembleDebug`.
- [ ] APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Required device checks
- [ ] Select and play a supported local audio file through Android's document picker.
- [ ] Pause, resume, and stop playback from the app.
- [ ] Lock the screen while audio plays; confirm audio continues.
- [ ] Confirm Android media notification / lock-screen controls are shown and work.
- [ ] Unplug wired headphones or disconnect Bluetooth; confirm playback pauses.
- [ ] Test at least one MP3 and one M4A/AAC file.
- [ ] Confirm playback still works with network disabled.

## Exit criteria
Do not start history, bookmarks, or full UI until CI passes and the device checks above are recorded. CI success alone does not prove background playback on a physical device.
