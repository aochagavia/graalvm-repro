#!/usr/bin/env bash
set -euo pipefail

# Build the GraalVM native shared library and headers into java-lib/
(
  cd "$(dirname "$0")/java-lib"
  ./gradlew buildNativeLibrary
)

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_LIB_DIR="${ROOT_DIR}/java-lib"
C_APP_DIR="${ROOT_DIR}/c-application"
LIB_SO="${JAVA_LIB_DIR}/java-lib.so"

if [[ ! -f "${LIB_SO}" ]]; then
  echo "Error: ${LIB_SO} not found after buildNativeLibrary. Check build output." >&2
  exit 1
fi

# Compile C application and link against the Java shared library.
# Use rpath so the executable can find java-lib.so at runtime.
clang \
  -std=c11 \
  -I "${JAVA_LIB_DIR}" \
  "${C_APP_DIR}/main.c" \
  "${LIB_SO}" \
  -Wl,-rpath,'$ORIGIN/../java-lib' \
  -o "${C_APP_DIR}/main"

echo "Build successful."
