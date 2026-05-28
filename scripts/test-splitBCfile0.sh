#!/usr/bin/env bash
set -euo pipefail

# Test splitBCfile=0 vs default behavior on OHOS arm64 + iOS arm64
# Issue: ALI-52

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

echo "=== splitBCfile=0 vs Default Behavior Test (ALI-52) ==="
echo "Konan version: $(grep kotlinVersion gradle.properties)"
echo "Host: $(uname -s) $(uname -m)"
echo "Date: $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
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

RESULTS_DIR="build/splitBCfile0-results"
mkdir -p "$RESULTS_DIR"

# Define test configurations
declare -a CONFIGS=("default" "splitBC0")
declare -A CONFIG_LABELS=(
    ["default"]="Default (no splitBCfile option)"
    ["splitBC0"]="splitBCfile=0 (explicit no-split)"
)
declare -A CONFIG_ARGS=(
    ["default"]=""
    ["splitBC0"]="-PsplitBC=0"
)

# Define targets
declare -A TARGETS=(
    ["ohosArm64"]=":kotlinApp:linkDebugSharedOhosArm64"
    ["iosArm64"]=":kotlinApp:linkDebugFrameworkIosArm64"
)

ORDER=("ohosArm64" "iosArm64")

for config in "${CONFIGS[@]}"; do
    label="${CONFIG_LABELS[$config]}"
    extra_args="${CONFIG_ARGS[$config]}"
    
    echo ""
    echo "================================================================"
    echo "Configuration: $label"
    echo "Extra args: ${extra_args:-'(none)'}"
    echo "================================================================"
    
    for target in "${ORDER[@]}"; do
        task="${TARGETS[$target]}"
        logfile="$RESULTS_DIR/${config}_${target}.log"
        
        echo ""
        echo "------------------------------------------"
        echo "Target: $target (task: $task)"
        echo "Log: $logfile"
        echo "------------------------------------------"
        
        # Clean before each build
        set +e
        ./gradlew clean --no-daemon > /dev/null 2>&1
        
        # Build and capture full output
        if [[ -n "$extra_args" ]]; then
            ./gradlew "$task" --no-daemon $extra_args --info 2>&1 | tee "$logfile"
        else
            ./gradlew "$task" --no-daemon --info 2>&1 | tee "$logfile"
        fi
        exit_code=$?
        set -e
        
        echo ""
        if [[ $exit_code -eq 0 ]]; then
            echo "RESULT: $config / $target -> BUILD SUCCESSFUL (exit code 0)"
            
            # Check for llvm-split invocation in logs
            if grep -q "llvm-split" "$logfile" 2>/dev/null; then
                echo "  llvm-split mentions in log:"
                grep -n "llvm-split" "$logfile" | head -5
            else
                echo "  No llvm-split mentions in log"
            fi
            
            # Show output artifact sizes
            echo "  Output artifacts:"
            if [[ "$target" == "ohosArm64" ]]; then
                find kotlinApp/build/bin/ohosArm64 -name "*.so" -exec ls -lh {} \; 2>/dev/null || echo "  (no .so found)"
                find kotlinApp/build/bin/ohosArm64 -name "*.bc" -exec ls -lh {} \; 2>/dev/null || echo "  (no .bc found)"
            elif [[ "$target" == "iosArm64" ]]; then
                find kotlinApp/build/bin/iosArm64 -name "*.framework" -type d -exec du -sh {} \; 2>/dev/null || echo "  (no .framework found)"
                find kotlinApp/build/bin/iosArm64 -name "*.bc" -exec ls -lh {} \; 2>/dev/null || echo "  (no .bc found)"
            fi
            
        else
            echo "RESULT: $config / $target -> BUILD FAILED (exit code $exit_code)"
            
            # Show last 20 lines of the log
            echo "  Last 20 lines:"
            tail -20 "$logfile"
            
            # Check for specific errors
            if grep -q "llvm-split" "$logfile" 2>/dev/null; then
                echo "  llvm-split related errors:"
                grep -n "llvm-split" "$logfile" | head -10
            fi
            if grep -q "splitBCfile" "$logfile" 2>/dev/null; then
                echo "  splitBCfile mentions:"
                grep -n "splitBCfile" "$logfile" | head -5
            fi
        fi
        echo ""
    done
done

# Summary comparison
echo ""
echo "================================================================"
echo "SUMMARY COMPARISON"
echo "================================================================"

for target in "${ORDER[@]}"; do
    echo ""
    echo "--- $target ---"
    
    default_log="$RESULTS_DIR/default_${target}.log"
    split0_log="$RESULTS_DIR/splitBC0_${target}.log"
    
    # Check if both succeeded
    default_ok="FAILED"
    split0_ok="FAILED"
    
    if [[ -f "$default_log" ]] && grep -q "BUILD SUCCESSFUL" "$default_log" 2>/dev/null; then
        default_ok="SUCCESS"
    fi
    if [[ -f "$split0_log" ]] && grep -q "BUILD SUCCESSFUL" "$split0_log" 2>/dev/null; then
        split0_ok="SUCCESS"
    fi
    
    echo "  Default:   $default_ok"
    echo "  splitBC=0: $split0_ok"
    
    # Compare llvm-split mentions
    default_split=$(grep -c "llvm-split" "$default_log" 2>/dev/null || echo "0")
    split0_split=$(grep -c "llvm-split" "$split0_log" 2>/dev/null || echo "0")
    echo "  llvm-split mentions (default): $default_split"
    echo "  llvm-split mentions (splitBC=0): $split0_split"
done

echo ""
echo "=== Test complete. Full logs in $RESULTS_DIR/ ==="
