#!/bin/bash

# Source-based Code Coverage Build Script for OHOS
# Full process: Build -> Deploy -> Generate Report

# Use set -e but allow commands in if statements to fail
set -e
set -o pipefail

CLANG_PATH="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/clang++"
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"
RESOURCE_DIR="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4"
CLANG_LIB_DIR="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4/lib/aarch64-linux-ohos"
TARGET="aarch64-linux-ohos"

LLVM_COV_PATH="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-cov"
LLVM_PROFDATA_PATH="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-profdata"

SOURCE="main.cpp"
IR_FILE="main_sourcebased.ll"
OBJ_FILE="main_sourcebased.o"
EXE_FILE="main_sourcebased"
PROFRAW_FILE="default_sourcebased.profraw"
PROFDATA_FILE="default_sourcebased.profdata"
REPORT_DIR="coverage_report_sourcebased"

DEVICE_PATH="/data/local/tmp"
DEVICE_EXE="$DEVICE_PATH/$EXE_FILE"
DEVICE_PROFRAW="$DEVICE_PATH/default_sourcebased.profraw"

echo "=== BUILD PHASE ==="
echo "=== Step 1: Emit IR with Source-based coverage instrumentation ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-instr-generate -fcoverage-mapping \
  -emit-llvm -S \
  -O3 \
  "$SOURCE" \
  -target "$TARGET" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$IR_FILE"

echo "=== Step 2: Compile IR to Object File ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-instr-generate -fcoverage-mapping \
  -c "$IR_FILE" \
  -O3 \
  -target "$TARGET" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$OBJ_FILE"

echo "=== Step 3: Link Object File to Executable ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-instr-generate -fcoverage-mapping \
  -O3 \
  "$OBJ_FILE" \
  -target "$TARGET" \
  -L"$CLANG_LIB_DIR" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$EXE_FILE"

echo "=== Build Complete ==="
echo "Executable: $EXE_FILE"
echo "IR file: $IR_FILE (contains source-based coverage instrumentation)"
echo "Object file: $OBJ_FILE"

echo ""
echo "=== DEPLOY PHASE ==="
echo "=== Deploying to OHOS device ==="

# Send executable to device
hdc file send "$EXE_FILE" "$DEVICE_EXE"
hdc shell chmod 777 "$DEVICE_EXE"

echo "=== Running on device ==="
hdc shell "cd $DEVICE_PATH && LD_LIBRARY_PATH=$DEVICE_PATH LLVM_PROFILE_FILE=$DEVICE_PROFRAW $DEVICE_EXE 20 4"

echo "=== Copying coverage data files from device ==="
hdc file recv "$DEVICE_PROFRAW" ./"$PROFRAW_FILE" 2>/dev/null || echo "Note: profraw file may not exist yet"
if [ -f "$PROFRAW_FILE" ]; then
    echo "Successfully copied $PROFRAW_FILE"
else
    echo "Warning: profraw file not found on device"
fi

echo ""
echo "=== REPORT GENERATION PHASE ==="
# Generate report if coverage data exists
if [ -f "$PROFRAW_FILE" ]; then
    echo "=== Generating Source-based Coverage Report ==="
    
    # Merge profraw files into profdata
    echo "Merging profile data..."
    MERGE_OUTPUT=$(mktemp /tmp/profdata_merge.XXXXXX.log)
    if ! "$LLVM_PROFDATA_PATH" merge -sparse "$PROFRAW_FILE" -o "$PROFDATA_FILE" > "$MERGE_OUTPUT" 2>&1; then
        echo "Warning: Failed to merge profile data. Check $MERGE_OUTPUT for details."
        echo "This may be due to version mismatch between device and host LLVM."
        cat "$MERGE_OUTPUT"
        # Try to continue anyway - sometimes llvm-cov can work with profraw directly
        if [ -f "$PROFRAW_FILE" ] && [ -s "$PROFRAW_FILE" ]; then
            echo "Attempting to use profraw file directly..."
            PROFDATA_FILE="$PROFRAW_FILE"
        else
            echo "Error: Cannot proceed without valid profile data."
            rm -f "$MERGE_OUTPUT"
            exit 1
        fi
    else
        echo "Profile data merged successfully."
        # Verify profdata file was created and is not empty
        if [ ! -f "$PROFDATA_FILE" ] || [ ! -s "$PROFDATA_FILE" ]; then
            echo "Error: Profile data file ($PROFDATA_FILE) was not created or is empty."
            cat "$MERGE_OUTPUT"
            rm -f "$MERGE_OUTPUT"
            exit 1
        fi
    fi
    rm -f "$MERGE_OUTPUT"
    
    # Verify executable exists
    if [ ! -f "$EXE_FILE" ]; then
        echo "Error: Executable file ($EXE_FILE) not found. Cannot generate coverage report."
        exit 1
    fi
    
    # Create report directory
    mkdir -p "$REPORT_DIR"
    
    # Generate HTML report
    echo "Generating HTML report..."
    HTML_LOG="$REPORT_DIR/html_report.log"
    if "$LLVM_COV_PATH" show "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -format=html \
            -output-dir="$REPORT_DIR" \
            -show-line-counts-or-regions \
            -show-instantiations=false \
            "$SOURCE" > "$HTML_LOG" 2>&1; then
        if [ -f "$REPORT_DIR/index.html" ]; then
            echo "HTML report generated successfully: $REPORT_DIR/index.html"
        else
            echo "Warning: HTML report command succeeded but index.html not found. Check $HTML_LOG"
        fi
    else
        echo "Warning: HTML report generation failed. Check $HTML_LOG for details."
        cat "$HTML_LOG" | tail -20
    fi
    
    # Generate text report
    echo "Generating text report..."
    if "$LLVM_COV_PATH" show "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -format=text \
            -show-line-counts-or-regions \
            -show-instantiations=false \
            "$SOURCE" > "$REPORT_DIR/coverage.txt" 2>&1; then
        if [ -f "$REPORT_DIR/coverage.txt" ] && [ -s "$REPORT_DIR/coverage.txt" ]; then
            echo "Text report generated successfully: $REPORT_DIR/coverage.txt"
        else
            echo "Warning: Text report command succeeded but file is missing or empty."
        fi
    else
        echo "Warning: Text report generation failed. Check $REPORT_DIR/coverage.txt for details."
        [ -f "$REPORT_DIR/coverage.txt" ] && cat "$REPORT_DIR/coverage.txt" | tail -20
    fi
    
    # Generate summary
    echo "Generating summary..."
    if "$LLVM_COV_PATH" report "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -show-region-summary=false \
            "$SOURCE" > "$REPORT_DIR/summary.txt" 2>&1; then
        if [ -f "$REPORT_DIR/summary.txt" ] && [ -s "$REPORT_DIR/summary.txt" ]; then
            echo "Summary generated successfully: $REPORT_DIR/summary.txt"
            echo "Coverage summary:"
            cat "$REPORT_DIR/summary.txt"
        else
            echo "Warning: Summary command succeeded but file is missing or empty."
        fi
    else
        echo "Warning: Summary generation failed. Check $REPORT_DIR/summary.txt for details."
        [ -f "$REPORT_DIR/summary.txt" ] && cat "$REPORT_DIR/summary.txt" | tail -20
    fi
    
    echo ""
    echo "=== Coverage Report Status ==="
    REPORTS_GENERATED=0
    if [ -f "$REPORT_DIR/index.html" ]; then
        echo "✓ HTML report: $REPORT_DIR/index.html"
        REPORTS_GENERATED=1
    else
        echo "✗ HTML report: Not generated"
    fi
    if [ -f "$REPORT_DIR/coverage.txt" ] && [ -s "$REPORT_DIR/coverage.txt" ]; then
        echo "✓ Text report: $REPORT_DIR/coverage.txt"
        REPORTS_GENERATED=1
    else
        echo "✗ Text report: Not generated or empty"
    fi
    if [ -f "$REPORT_DIR/summary.txt" ] && [ -s "$REPORT_DIR/summary.txt" ]; then
        echo "✓ Summary: $REPORT_DIR/summary.txt"
        REPORTS_GENERATED=1
    else
        echo "✗ Summary: Not generated or empty"
    fi
    
    if [ $REPORTS_GENERATED -eq 0 ]; then
        echo ""
        echo "ERROR: No coverage reports were generated successfully!"
        echo "Please check the log files in $REPORT_DIR/ for error details."
        exit 1
    fi
else
    echo "Warning: Coverage data ($PROFRAW_FILE) not found. Report generation skipped."
fi

echo ""
echo "=== Complete ==="
