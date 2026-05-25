#!/usr/bin/env bash
set -euo pipefail

# Boundary test for splitBCfile=0/1/2 on ohosArm64
# Tests each value and reports pass/fail.

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

BUILD_FILE="kotlinApp/build.gradle.kts"
FLAG_LINE='                freeCompilerArgs += "-Xbinary=splitBCfile='
TASK=":kotlinApp:linkDebugSharedOhosArm64"

# Save original
cp "$BUILD_FILE" "$BUILD_FILE.bak"
trap 'mv "$BUILD_FILE.bak" "$BUILD_FILE"' EXIT

run_build() {
    local value="$1"
    local label="$2"

    # Restore original
    cp "$BUILD_FILE.bak" "$BUILD_FILE"

    if [[ "$value" != "none" ]]; then
        # Insert the splitBCfile flag after stripDebugInfoFromNativeLibs line
        sed -i.tmp "s|freeCompilerArgs += \"-Xbinary=stripDebugInfoFromNativeLibs=false\"|freeCompilerArgs += \"-Xbinary=stripDebugInfoFromNativeLibs=false\"\n${FLAG_LINE}${value}\"|" "$BUILD_FILE"
        rm -f "$BUILD_FILE.tmp"
    fi

    echo ""
    echo "=========================================="
    echo "Testing: $label"
    echo "=========================================="

    if ./gradlew clean "$TASK" --no-daemon 2>&1 | tail -5; then
        echo "RESULT: $label -> BUILD SUCCESSFUL"
    else
        echo "RESULT: $label -> BUILD FAILED (crash)"
    fi
    echo ""
}

echo "=== splitBCfile Boundary Test ==="
echo "Konan version: $(grep kotlinVersion gradle.properties)"
echo ""

# Check llvm-split presence
LLVM_BIN="$HOME/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin"
if [[ -f "$LLVM_BIN/llvm-split" ]]; then
    echo "llvm-split: PRESENT at $LLVM_BIN/llvm-split"
else
    echo "llvm-split: MISSING from $LLVM_BIN"
fi
echo ""

run_build "none" "splitBCfile=<not set> (default)"
run_build "1"    "splitBCfile=1"
run_build "2"    "splitBCfile=2"

echo "=== Boundary test complete ==="
