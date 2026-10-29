#!/usr/bin/env sh
# Downloads gradle-wrapper.jar if missing (binary is not always in git).
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar"
mkdir -p "$ROOT/gradle/wrapper"
if [ ! -f "$JAR" ]; then
  echo "Downloading gradle-wrapper.jar ..."
  curl -fsSL -o "$JAR" "$URL"
fi
chmod +x "$ROOT/gradlew"
echo "Wrapper ready. Example: ./gradlew :app:assembleDebug"
