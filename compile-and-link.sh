#!/usr/bin/env bash
set -euo pipefail

# Build the GraalVM native shared library and headers into kotlin-lib/
(
  cd "$(dirname "$0")/kotlin-lib"
  ./gradlew buildNativeLibrary
)

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
KOTLIN_LIB_DIR="${ROOT_DIR}/kotlin-lib"
C_APP_DIR="${ROOT_DIR}/c-application"
LIB_SO="${KOTLIN_LIB_DIR}/kotlin-lib.so"

if [[ ! -f "${LIB_SO}" ]]; then
  echo "Error: ${LIB_SO} not found after buildNativeLibrary. Check build output." >&2
  exit 1
fi

# Compile C application and link against the Kotlin shared library.
# Use rpath so the executable can find kotlin-lib.so at runtime.
clang \
  -std=c11 \
  -I "${KOTLIN_LIB_DIR}" \
  "${C_APP_DIR}/main.c" \
  "${LIB_SO}" \
  -Wl,-rpath,'$ORIGIN/../kotlin-lib' \
  -o "${C_APP_DIR}/main"

echo "Build successful."
