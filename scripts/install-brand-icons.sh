#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/scripts/icon-assets"
TMP="$ROOT/build/brand-icon.png"

mkdir -p "$ROOT/build" \
  "$RES/drawable-hdpi" "$RES/drawable-xhdpi" "$RES/drawable-xxhdpi" "$RES/drawable-xxxhdpi" \
  "$RES/mipmap-hdpi" "$RES/mipmap-xhdpi" "$RES/mipmap-xxhdpi" "$RES/mipmap-xxxhdpi"

# Prefer assembled local brand mark from part files (locked in repo)
if ls "$ASSETS"/brand-icon.part* >/dev/null 2>&1; then
  cat "$ASSETS"/brand-icon.part* | tr -d '\n' | base64 -d > "$TMP"
elif [ -f "$ASSETS/brand-icon.png" ]; then
  cp "$ASSETS/brand-icon.png" "$TMP"
else
  echo "No brand icon assets found; keeping vector fallback"
  exit 0
fi

if [ ! -s "$TMP" ]; then
  echo "Brand icon empty; keeping vector fallback"
  exit 0
fi

for dens in hdpi xhdpi xxhdpi xxxhdpi; do
  cp "$TMP" "$RES/drawable-$dens/ic_launcher_foreground.png"
  cp "$TMP" "$RES/mipmap-$dens/ic_launcher.png"
  cp "$TMP" "$RES/mipmap-$dens/ic_launcher_round.png"
done
echo "Brand icons installed from repo-locked assets"
