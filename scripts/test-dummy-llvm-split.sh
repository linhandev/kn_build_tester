#!/usr/bin/env bash
set -euo pipefail

# Experiment: replace llvm-split with a dummy (exit 0) to isolate SIGSEGV cause.
# If the SIGSEGV disappears, the crash is caused by the missing binary (not by
# splitBCfile=2 logic itself).

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

LLVM_BIN="$HOME/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin"
DUMMY="$LLVM_BIN/llvm-split"
TASK=":kotlinApp:linkDebugSharedOhosArm64"

# Cleanup: always remove the dummy on exit
cleanup() {
    if [[ -f "$DUMMY" ]] && head -1 "$DUMMY" 2>/dev/null | grep -q '#!/bin/sh'; then
        rm -f "$DUMMY"
        echo "[cleanup] Removed dummy llvm-split"
    fi
}
trap cleanup EXIT

echo "=== Dummy llvm-split Experiment ==="
echo ""

# Check if a real llvm-split already exists
if [[ -f "$DUMMY" ]]; then
    echo "WARNING: $DUMMY already exists. Skipping to avoid overwriting a real binary."
    echo "Remove it manually if you want to run this experiment."
    exit 1
fi

# Create dummy llvm-split
echo "Creating dummy llvm-split at $DUMMY ..."
printf '#!/bin/sh\nexit 0\n' > "$DUMMY"
chmod +x "$DUMMY"
echo "Dummy created: $(file "$DUMMY")"
echo ""

# Run the build with splitBCfile=2 (already set in build.gradle.kts)
echo "Running build with splitBCfile=2 + dummy llvm-split ..."
echo ""

BUILD_OUTPUT=$(./gradlew clean "$TASK" --no-daemon 2>&1) || true

echo "$BUILD_OUTPUT" | tail -30
echo ""

# Analyze result
if echo "$BUILD_OUTPUT" | grep -q "SIGSEGV"; then
    echo "RESULT: SIGSEGV still occurs with dummy llvm-split"
    echo "  -> The crash is NOT caused by the missing binary alone"
elif echo "$BUILD_OUTPUT" | grep -q "ObjectFilesPhase\|out\.bc\|No such file"; then
    echo "RESULT: With dummy llvm-split: build fails at ObjectFilesPhase (no SIGSEGV)"
    echo "  -> The SIGSEGV is caused by the missing llvm-split binary"
    echo "  -> With a dummy in place, the build proceeds past splitBitcodeFile"
    echo "     and fails later with a clean Java exception (missing out.bc)"
elif echo "$BUILD_OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
    echo "RESULT: Build succeeded with dummy llvm-split (unexpected)"
else
    echo "RESULT: Build failed with an unrecognized error"
    echo "  -> Check the output above for details"
fi

echo ""
echo "=== Experiment complete ==="
