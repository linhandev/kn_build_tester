#!/bin/bash
# Test script for splitBCfile crash reproduction with Konan CPF 2.2.21-0.2.0-04
# Tests all four targets with splitBCfile=0, 1, 2

set -e

TARGETS=("OhosArm64" "OhosX64" "IosArm64" "MacosArm64")
LEVELS=("0" "1" "2")

echo "=== splitBCfile Crash Reproduction Test ==="
echo "Konan: 2.2.21-0.2.0-04"
echo "Host: $(uname -s) $(uname -m)"
echo ""

for level in "${LEVELS[@]}"; do
    for target in "${TARGETS[@]}"; do
        echo "--- Testing splitBCfile=$level on $target ---"
        # Clean previous build
        rm -rf kotlinApp/build
        
        if ./gradlew ":kotlinApp:linkDebugShared${target}" -PsplitBCLevel=$level --no-daemon 2>&1; then
            echo "RESULT: splitBCfile=$level + $target => SUCCESS"
        else
            EXIT_CODE=$?
            echo "RESULT: splitBCfile=$level + $target => CRASH (exit $EXIT_CODE)"
        fi
        echo ""
    done
done

echo "=== Test Complete ==="
