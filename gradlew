#!/bin/sh
set -eu

GRADLE_VERSION="8.10.2"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

GRADLE_HOME=""
for candidate in "$CACHE"/gradle-${GRADLE_VERSION} "$CACHE"/*/gradle-${GRADLE_VERSION}; do
  if [ -x "$candidate/bin/gradle" ]; then GRADLE_HOME="$candidate"; break; fi
done

if [ -z "$GRADLE_HOME" ]; then
  TMP="${CACHE}/download"
  mkdir -p "$TMP"
  ZIP="${TMP}/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 -o "$ZIP" "$DIST_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$DIST_URL"
  else
    echo "Gradle ${GRADLE_VERSION} is required and curl/wget is unavailable." >&2
    exit 1
  fi
  mkdir -p "$CACHE"
  rm -rf "$CACHE/gradle-${GRADLE_VERSION}"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP" -d "$CACHE"
  else
    echo "unzip is required to bootstrap Gradle ${GRADLE_VERSION}." >&2
    exit 1
  fi
  GRADLE_HOME="$CACHE/gradle-${GRADLE_VERSION}"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
