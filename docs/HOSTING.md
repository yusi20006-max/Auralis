# Hosting / CDN (when you are ready)

Auralis does **not** require a host for local import or sample streaming.
When you buy hosting:

1. Upload MP3/M4A files to HTTPS (CDN or object storage).
2. Either:
   - Put full `https://...` URLs in catalog `audioUrl`, or
   - Set `CatalogConfig.baseUrl = "https://your-cdn.example"` and use relative paths.
3. Keep downloads on-device in **private app storage** (`PrivateDownloadStore`) so files stay out of the system file manager.

## Privacy model

| Location | Visible in file managers? |
|----------|---------------------------|
| `context.filesDir/auralis_private/` | No (normal users) |
| Public Downloads / Music | Yes — **do not use** |

Optional later: encrypt files at rest with AndroidX Security Crypto.
