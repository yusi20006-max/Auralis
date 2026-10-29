# Brand icon assets

To **permanently lock** the official Auralis launcher icon in the repo:

1. Export the approved neon **A** mark as a square PNG (256×256 or 512×512).
2. Save it here as:

```text
scripts/icon-assets/brand-icon.png
```

3. Commit the file. CI (`scripts/install-brand-icons.sh`) will prefer this path over CDN.

Until that file exists, CI downloads a temporary copy of the same mark so APKs still ship the brand icon.
