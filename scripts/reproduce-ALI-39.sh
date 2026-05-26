#!/usr/bin/env bash
set -uo pipefail
# Reproduction script for ALI-39: mpcore-llvm-19-aarch64-macos-dev-11 + splitBCfile=2 → gradle daemon crash
#
# This script:
# 1. Downloads mpcore-llvm-19-aarch64-macos-dev-11 if not present
# 2. Swaps the LLVM in konan.properties (in the installed KN distribution)
# 3. Runs the build targeting ohosArm64 with splitBCfile=2
# 4. Captures full crash output

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

KONAN_DATA_DIR="${KONAN_DATA_DIR:-$HOME/.konan}"
KN_VERSION="2.2.21-0.3.0-04"
KN_DIST="$KONAN_DATA_DIR/kotlin-native-prebuilt-macos-aarch64-$KN_VERSION"
KONAN_PROPS="$KN_DIST/konan/konan.properties"
OLD_LLVM="llvm-1914-aarch64-macos-dev-10"
NEW_LLVM="mpcore-llvm-19-aarch64-macos-dev-11"
DEPS_DIR="$KONAN_DATA_DIR/dependencies"
MAVEN_BASE="https://maven.eazytec-cloud.com/nexus/repository/file-storage"

echo "=== ALI-39 Reproduction: mpcore-llvm-19 + splitBCfile=2 ==="
echo ""

# Step 1: Check KN distribution exists
if [ ! -d "$KN_DIST" ]; then
    echo "ERROR: Kotlin/Native distribution not found at $KN_DIST"
    echo "Run './gradlew :kotlinApp:linkDebugSharedOhosArm64 --dry-run' first to download it."
    exit 1
fi
echo "[OK] KN distribution found: $KN_DIST"

# Step 2: Download mpcore LLVM if not present
if [ ! -d "$DEPS_DIR/$NEW_LLVM" ]; then
    echo "[...] Downloading $NEW_LLVM..."
    mkdir -p "$DEPS_DIR"
    curl -L "$MAVEN_BASE/llvm/$NEW_LLVM.tar.gz" -o "$DEPS_DIR/$NEW_LLVM.tar.gz"
    echo "[...] Extracting..."
    tar xzf "$DEPS_DIR/$NEW_LLVM.tar.gz" -C "$DEPS_DIR/"
    rm -f "$DEPS_DIR/$NEW_LLVM.tar.gz"
    echo "[OK] $NEW_LLVM installed"
else
    echo "[OK] $NEW_LLVM already installed"
fi

# Step 3: Backup and swap LLVM in konan.properties
if [ ! -f "$KONAN_PROPS.bak" ]; then
    cp "$KONAN_PROPS" "$KONAN_PROPS.bak"
    echo "[OK] Backed up konan.properties"
fi

# Restore from backup first (in case of re-run)
cp "$KONAN_PROPS.bak" "$KONAN_PROPS"

# Swap the LLVM reference
sed -i '' "s/$OLD_LLVM/$NEW_LLVM/g" "$KONAN_PROPS"
echo "[OK] Swapped LLVM: $OLD_LLVM → $NEW_LLVM"
echo ""
echo "Verify swap:"
grep "llvm.macos_arm64" "$KONAN_PROPS"
echo ""

# Step 4: Stop any running gradle daemon
echo "[...] Stopping gradle daemon..."
./gradlew --stop 2>/dev/null || true
echo ""

# Step 5: Run the build and capture ALL output
echo "=== Running build (this may crash the daemon) ==="
echo ""

CRASH_LOG="build/crash-output-$(date +%Y%m%d-%H%M%S).log"
mkdir -p build

# Run with --no-daemon to capture output even if daemon crashes
# Also capture JVM crash dumps
export _JAVA_OPTIONS="-XX:ErrorFile=$PWD/build/hs_err_pid%p.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$PWD/build/"

./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-daemon --stacktrace --info 2>&1 | tee "$CRASH_LOG"
BUILD_EXIT=${PIPESTATUS[0]}

echo ""
echo "=== Build exit code: $BUILD_EXIT ==="
echo ""

# Check for JVM crash dumps
echo "=== Checking for JVM crash dumps ==="
ls -la build/hs_err_pid*.log 2>/dev/null || echo "No JVM hs_err_pid files found"
ls -la build/java_pid*.hprof 2>/dev/null || echo "No heap dumps found"
echo ""

# Check for native crash signals
if grep -qE "SIGSEGV|SIGABRT|SIGBUS|SIGFPE|signal|Segmentation|Aborted|daemon.*stopped|daemon.*crash" "$CRASH_LOG" 2>/dev/null; then
    echo "=== CRASH INDICATORS FOUND IN LOG ==="
    grep -nE "SIGSEGV|SIGABRT|SIGBUS|SIGFPE|signal|Segmentation|Aborted|daemon.*stopped|daemon.*crash" "$CRASH_LOG"
fi

echo ""
echo "Full output saved to: $CRASH_LOG"

# Step 6: Restore original konan.properties
cp "$KONAN_PROPS.bak" "$KONAN_PROPS"
echo "[OK] Restored original konan.properties"
