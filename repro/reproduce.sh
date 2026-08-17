#!/bin/bash
# Reproduce IS-87: +fix-cortex-a53-835769 causes extremely slow LLVM codegen
# for OHOS arm64 release builds of kmp-cmp-test-demo Performance branch.
#
# Prerequisites:
#   - macOS arm64 host (tested on Apple M-series)
#   - Kotlin/Native prebuilt 2.2.21-0.5.0-14 installed via Gradle
#   - CPF LLVM 19 toolchain (installed as KN dependency)
#   - kmp-cmp-test-demo repo access (read-only HTTPS)
#
# Usage: ./reproduce.sh [feature-set]
#   feature-set: issue (default) | no-fix | fix-only
set -euo pipefail

MODE="${1:-issue}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

DEMO_URL="https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git"
DEMO_BRANCH="Performance"
WORKDIR="${WORKDIR:-$HOME/git/worktree/kmp-cmp-test-demo-is87-repro}"
GRADLE_HOME="${GRADLE_HOME:-$HOME/git/worktree/kmp-cmp-test-demo-is87-repro-gradle_home}"

echo "=== IS-87 Reproduction ==="
echo "Feature set: $MODE"
echo ""

# 1. Clone kmp-cmp-test-demo Performance branch
if [ ! -d "$WORKDIR" ]; then
  echo "[1/4] Cloning kmp-cmp-test-demo ($DEMO_BRANCH branch)..."
  git clone -b "$DEMO_BRANCH" "$DEMO_URL" "$WORKDIR"
else
  echo "[1/4] Workdir exists: $WORKDIR"
fi

# 2. Patch konan.properties
echo "[2/4] Patching konan.properties (feature set: $MODE)..."
bash "$SCRIPT_DIR/patch-konan-properties.sh" "$MODE"

# 3. Clean build output
echo "[3/4] Cleaning previous build output..."
rm -rf "$WORKDIR/composeApp/build/bin/ohosArm64"

# 4. Build
echo "[4/4] Building linkReleaseSharedOhosArm64 (release)..."
echo "Start: $(date)"
export GRADLE_USER_HOME="$GRADLE_HOME"
cd "$WORKDIR"
START_TS=$(date +%s)
./gradlew linkReleaseSharedOhosArm64 -Pcompose.ohos.target=ohosArm64 --no-daemon
END_TS=$(date +%s)
ELAPSED=$((END_TS - START_TS))
echo "End: $(date)"
echo ""
echo "=== Build completed in ${ELAPSED}s ==="

# Show the output
if [ -f "$WORKDIR/composeApp/build/bin/ohosArm64/releaseShared/libkn.so" ]; then
  echo "SUCCESS: libkn.so produced"
  ls -la "$WORKDIR/composeApp/build/bin/ohosArm64/releaseShared/libkn.so"
else
  echo "WARNING: libkn.so not found — build may have failed or not completed"
fi
