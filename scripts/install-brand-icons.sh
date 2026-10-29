#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/scripts/icon-assets"
decode() {
  rel="$1"
  src="$2"
  mkdir -p "$RES/$(dirname "$rel")"
  base64 -d < "$ASSETS/$src" > "$RES/$rel"
}
decode "drawable-xxhdpi/ic_launcher_foreground.png" "drawable-xxhdpi__ic_launcher_foreground.b64"
decode "drawable-xxxhdpi/ic_launcher_foreground.png" "drawable-xxxhdpi__ic_launcher_foreground.b64"
decode "mipmap-xxhdpi/ic_launcher.png" "mipmap-xxhdpi__ic_launcher.b64"
decode "mipmap-xxhdpi/ic_launcher_round.png" "mipmap-xxhdpi__ic_launcher_round.b64"
decode "mipmap-xxxhdpi/ic_launcher.png" "mipmap-xxxhdpi__ic_launcher.b64"
decode "mipmap-xxxhdpi/ic_launcher_round.png" "mipmap-xxxhdpi__ic_launcher_round.b64"
echo "Brand icons installed from scripts/icon-assets"
