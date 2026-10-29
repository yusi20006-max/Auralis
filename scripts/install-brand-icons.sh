#!/usr/bin/env sh
# Installs the official Auralis brand mark (user-provided neon A) into res/.
# Prefer local PNG if present; otherwise download the published brand asset URL.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
RES="$ROOT/app/src/main/res"
TMP="$ROOT/build/brand-icon.png"
# Temporary host of the owner-approved brand crop (replace with repo-hosted asset later).
BRAND_URL="${AURALIS_BRAND_ICON_URL:-https://litter.catbox.moe/yflsja.png}"
LOCAL="$ROOT/scripts/icon-assets/brand-icon.png"

mkdir -p "$ROOT/build" "$RES/drawable-xxhdpi" "$RES/drawable-xxxhdpi" \
  "$RES/mipmap-xxhdpi" "$RES/mipmap-xxxhdpi" "$RES/mipmap-xhdpi" "$RES/mipmap-hdpi"

if [ -f "$LOCAL" ]; then
  cp "$LOCAL" "$TMP"
elif command -v curl >/dev/null 2>&1; then
  echo "Downloading brand icon from $BRAND_URL"
  curl -fsSL --max-time 60 -o "$TMP" "$BRAND_URL"
else
  echo "No local brand icon and curl missing; keeping vector fallback"
  exit 0
fi

if [ ! -s "$TMP" ]; then
  echo "Brand icon download empty; keeping vector fallback"
  exit 0
fi

# Same high-res mark for adaptive foreground + legacy launcher (system scales).
cp "$TMP" "$RES/drawable-xxhdpi/ic_launcher_foreground.png"
cp "$TMP" "$RES/drawable-xxxhdpi/ic_launcher_foreground.png"
cp "$TMP" "$RES/mipmap-hdpi/ic_launcher.png"
cp "$TMP" "$RES/mipmap-hdpi/ic_launcher_round.png"
cp "$TMP" "$RES/mipmap-xhdpi/ic_launcher.png"
cp "$TMP" "$RES/mipmap-xhdpi/ic_launcher_round.png"
cp "$TMP" "$RES/mipmap-xxhdpi/ic_launcher.png"
cp "$TMP" "$RES/mipmap-xxhdpi/ic_launcher_round.png"
cp "$TMP" "$RES/mipmap-xxxhdpi/ic_launcher.png"
cp "$TMP" "$RES/mipmap-xxxhdpi/ic_launcher_round.png"
echo "Brand icons installed into app/src/main/res"
