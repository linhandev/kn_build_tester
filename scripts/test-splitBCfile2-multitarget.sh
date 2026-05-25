#!/usr/bin/env bash
set -euo pipefail

# Multi-target splitBCfile=2 crash reproduction test (ALI-37)
# Tests ohosArm64, ohosX64, iosArm64, macosArm64

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

echo "=== splitBCfile=2 Multi-Target Crash Reproduction (ALI-37) ==="
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
    ["ohosX64"]=":kotlinApp:linkDebugSharedOhosX64"
    ["iosArm64"]=":kotlinApp:linkDebugSharedIosArm64"
    ["macosArm64"]=":kotlinApp:linkDebugSharedMacosArm64"
)

# Test order — priority targets first
ORDER=("ohosArm64" "ohosX64" "iosArm64" "macosArm64")

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

        # Check for llvm-split invocation
        if echo "$output" | grep -q "llvm-split command:"; then
            echo "  llvm-split was invoked:"
            echo "$output" | grep "llvm-split command:"
        fi

        # Check for the specific error
        if echo "$output" | grep -q "Failed to execute llvm-split"; then
            echo "  *** llvm-split EXECUTION FAILURE ***"
        fi
    fi
    echo ""
done

echo "=== Multi-target test complete ==="
