#!/bin/sh
# Wrapper minimal: memakai Gradle yang sudah terinstall di sistem.
# Alternatif: buka proyek ini di Android Studio (ia mengatur Gradle sendiri).
# Contoh: ./gradlew assembleDebug
set -e
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle tidak ditemukan. Install Gradle 8.7+ atau buka proyek ini di Android Studio."
exit 1
