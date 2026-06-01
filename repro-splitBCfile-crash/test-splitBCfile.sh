#!/bin/bash

echo "=========================================="
echo "Testing splitBCfile=2 crash reproduction"
echo "Kotlin/Native CPF version: 2.2.21-0.2.0-04"
echo "=========================================="
echo ""

# Define targets and their build tasks
declare -A TARGETS=(
    ["ohosArm64"]="linkDebugSharedOhosArm64"
    ["ohosX64"]="linkDebugSharedOhosX64"
    ["iosArm64"]="linkDebugFrameworkIosArm64"
    ["macosArm64"]="linkDebugExecutableMacosArm64"
)

# Test function
test_target() {
    local target=$1
    local task=$2
    local with_flag=$3
    
    echo "----------------------------------------"
    if [ "$with_flag" = "true" ]; then
        echo "Testing $target WITH -Xbinary=splitBCfile=2"
    else
        echo "Testing $target WITHOUT splitBCfile flag (baseline)"
    fi
    echo "Task: $task"
    echo "----------------------------------------"
    
    # Clean first
    ./gradlew clean > /dev/null 2>&1 || true
    
    # Build with or without flag
    if [ "$with_flag" = "true" ]; then
        ./gradlew $task -PenableSplitBC=true --no-daemon --stacktrace 2>&1 | tee "output-${target}-with-flag.log"
        local exit_code=${PIPESTATUS[0]}
        
        if [ $exit_code -eq 0 ]; then
            echo "✓ Build succeeded (unexpected - expected crash)"
        else
            echo "✗ Build failed with exit code $exit_code (expected crash)"
        fi
    else
        ./gradlew $task --no-daemon 2>&1 | tee "output-${target}-baseline.log"
        local exit_code=${PIPESTATUS[0]}
        
        if [ $exit_code -eq 0 ]; then
            echo "✓ Baseline build succeeded"
        else
            echo "✗ Baseline build failed with exit code $exit_code (unexpected)"
        fi
    fi
    
    echo ""
}

# Run tests
echo "PHASE 1: Baseline compilation (without splitBCfile=2)"
echo "======================================================="
for target in "ohosArm64" "ohosX64" "iosArm64" "macosArm64"; do
    test_target "$target" "${TARGETS[$target]}" "false"
done

echo ""
echo "PHASE 2: With splitBCfile=2 flag"
echo "======================================================="
for target in "ohosArm64" "ohosX64" "iosArm64" "macosArm64"; do
    test_target "$target" "${TARGETS[$target]}" "true"
done

echo ""
echo "=========================================="
echo "Test complete. Check output-*.log files for details."
echo "=========================================="
