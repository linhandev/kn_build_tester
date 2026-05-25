#!/usr/bin/env bash
set -euo pipefail

# Multi-target splitBCfile=2 crash reproduction test
# Tests ohosArm64, iosArm64, linuxX64, macosX64

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

echo "=== splitBCfile=2 Multi-Target Crash Reproduction ==="
echo "Konan version: $(grep kotlinVersion gradle.properties)"
echo "Host: $(uname -s) $(uname -m)"
echo ""

# Check llvm-split presence in the toolchain
LLVM_BIN="$HOME/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin"
if [[ -d "$LLVM_BIN" ]]; then
    if [[ -f "$LLVM_BIN/llvm-split" ]]; then
        echo "llvm-split: PRESENT at $LLVM_BIN/llvm-split"
    else
        echo "llvm-split: MISSING from $LLVM_BIN"
    fi
else
    echo "LLVM toolchain not yet downloaded (will be fetched on first build)"
fi
echo ""

# Define targets and their build tasks
declare -A TARGETS=(
    ["ohosArm64"]=":kotlinApp:linkDebugSharedOhosArm64"
    ["iosArm64"]=":kotlinApp:linkDebugSharedIosArm64"
    ["linuxX64"]=":kotlinApp:linkDebugExecutableLinuxX64"
    ["macosX64"]=":kotlinApp:linkDebugExecutableMacosX64"
)

# Test order
ORDER=("ohosArm64" "iosArm64" "linuxX64" "macosX64")

for target in "${ORDER[@]}"; do
    task="${TARGETS[$target]}"
    echo ""
    echo "=========================================="
    echo "Testing: $target (task: $task)"
    echo "=========================================="

    # Capture output and exit code
    set +e
    output=$(./gradlew clean "$task" --no-daemon 2>&1)
    exit_code=$?
    set -e

    # Show last 20 lines of output
    echo "$output" | tail -20

    if [[ $exit_code -eq 0 ]]; then
        echo ""
        echo "RESULT: $target -> BUILD SUCCESSFUL (exit code 0)"
    else
        echo ""
        echo "RESULT: $target -> BUILD FAILED (exit code $exit_code)"

        # Check for JVM crash
        if echo "$output" | grep -q "JVM crash log found"; then
            echo "  *** JVM CRASH DETECTED ***"
            crash_log=$(echo "$output" | grep "JVM crash log found" | sed 's/.*file:\/\///')
            echo "  Crash log: $crash_log"
        fi

        # Check for daemon disappearance
        if echo "$output" | grep -q "daemon disappeared"; then
            echo "  *** GRADLE DAEMON DISAPPEARED ***"
        fi

        # Check for llvm-split invocation
        if echo "$output" | grep -q "llvm-split command:"; then
            echo "  llvm-split was invoked:"
            echo "$output" | grep "llvm-split command:"
        fi
    fi
    echo ""
done

echo "=== Multi-target test complete ==="
