#!/usr/bin/env sh
# Install official Auralis brand mark into res/.
# Priority: local PNG > base64 parts > CDN fallback URL.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/scripts/icon-assets"
TMP="$ROOT/build/brand-icon.png"
# Owner-approved crop of the neon A mark (replace with repo file when available).
BRAND_URL="${AURALIS_BRAND_ICON_URL:-https://litter.catbox.moe/5mamgu.png}"

mkdir -p "$ROOT/build" \
  "$RES/drawable-hdpi" "$RES/drawable-xhdpi" "$RES/drawable-xxhdpi" "$RES/drawable-xxxhdpi" \
  "$RES/mipmap-hdpi" "$RES/mipmap-xhdpi" "$RES/mipmap-xxhdpi" "$RES/mipmap-xxxhdpi"

if [ -f "$ASSETS/brand-icon.png" ]; then
  cp "$ASSETS/brand-icon.png" "$TMP"
elif ls "$ASSETS"/brand-icon.part* >/dev/null 2>&1; then
  cat "$ASSETS"/brand-icon.part* | tr -d '\n\r ' | base64 -d > "$TMP"
elif command -v curl >/dev/null 2>&1; then
  echo "Downloading brand icon from $BRAND_URL"
  curl -fsSL --max-time 60 -o "$TMP" "$BRAND_URL" || true
fi

if [ ! -s "$TMP" ]; then
  echo "Brand icon unavailable; keeping vector fallback"
  exit 0
fi

for dens in hdpi xhdpi xxhdpi xxxhdpi; do
  cp "$TMP" "$RES/drawable-$dens/ic_launcher_foreground.png"
  cp "$TMP" "$RES/mipmap-$dens/ic_launcher.png"
  cp "$TMP" "$RES/mipmap-$dens/ic_launcher_round.png"
done
echo "Brand icons installed"
