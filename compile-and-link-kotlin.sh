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
LIB_A="${KOTLIN_LIB_DIR}/libkotlin.a"

if [[ ! -f "${LIB_A}" ]]; then
  echo "Error: ${LIB_A} not found after buildNativeLibrary. Check build output." >&2
  exit 1
fi

# Compile C application and link against the Kotlin static library (libkotlin.a).
clang \
  -std=c11 \
  -I "${KOTLIN_LIB_DIR}" \
  "${C_APP_DIR}/main.c" \
  "${LIB_A}" \
  -o "${C_APP_DIR}/main"

echo "Build successful."
