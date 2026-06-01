#!/bin/bash
# Reproduction script for ALI-72: splitBCfile crash on ohos_arm64
# Tests splitBCfile=0,1,2 across 4 targets using konan 2.2.21-0.3.0-04

set -euo pipefail

KONAN="${KONAN_HOME:-$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04}"
SRC="kotlinApp/src/commonMain/kotlin/SplitBCTest.kt"
OUTDIR="build/splitbc-test"

if [ ! -f "$KONAN/bin/kotlinc-native" ]; then
  echo "ERROR: kotlinc-native not found at $KONAN/bin/"
  echo "Set KONAN_HOME to the konan 2.2.21-0.3.0-04 prebuilt directory"
  exit 1
fi

echo "Using konan: $KONAN"
"$KONAN/bin/kotlinc-native" -version 2>&1
echo ""

TARGETS=("macos_arm64" "ios_arm64" "ohos_arm64" "ohos_x64")
LEVELS=(0 1 2)

mkdir -p "$OUTDIR"

for target in "${TARGETS[@]}"; do
  for level in "${LEVELS[@]}"; do
    echo "========================================"
    echo "Target: $target | splitBCfile=$level"
    echo "========================================"

    outfile="$OUTDIR/test_${target}_split${level}"
    rm -f "$outfile"*

    set +e
    output=$("$KONAN/bin/kotlinc-native" \
      -target "$target" \
      -produce dynamic \
      -o "$outfile" \
      -Xbinary=splitBCfile="$level" \
      "$SRC" 2>&1)
    rc=$?
    set -e

    if [ $rc -eq 0 ]; then
      echo "RESULT: ✅ BUILD SUCCESSFUL"
    else
      echo "RESULT: ❌ BUILD FAILED (exit code: $rc)"
      # Show relevant error lines
      echo "$output" | grep -E "SIGSEGV|fatal error|llvm-split|RuntimeException|crash|KotlinStubGenerator|exit code" | head -10
    fi
    echo ""
  done
done

echo "========================================"
echo "Test complete."
echo "========================================"
