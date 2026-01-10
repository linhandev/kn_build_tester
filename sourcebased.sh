#!/bin/bash

# Source-based Code Coverage Build Script for OHOS
# Build process: IR -> .o -> exe, then generate report if coverage data exists

set -e

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

# Generate report if coverage data exists
if [ -f "$PROFRAW_FILE" ]; then
    echo ""
    echo "=== Generating Source-based Coverage Report ==="
    
    # Merge profraw files into profdata
    echo "Merging profile data..."
    if ! "$LLVM_PROFDATA_PATH" merge -sparse "$PROFRAW_FILE" -o "$PROFDATA_FILE" 2>&1; then
        echo "Warning: Failed to merge profile data. This may be due to version mismatch between device and host LLVM."
        echo "Attempting to use profraw directly..."
        # Try to use profraw directly if merge fails
        PROFDATA_FILE="$PROFRAW_FILE"
    fi
    
    # Create report directory
    mkdir -p "$REPORT_DIR"
    
    # Generate HTML report
    if [ -f "$PROFDATA_FILE" ]; then
        echo "Generating HTML report..."
        "$LLVM_COV_PATH" show "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -format=html \
            -output-dir="$REPORT_DIR" \
            -show-line-counts-or-regions \
            -show-instantiations=false \
            "$SOURCE" 2>&1 | tee "$REPORT_DIR/html_report.log" || echo "HTML report generation failed"
        
        # Generate text report
        echo "Generating text report..."
        "$LLVM_COV_PATH" show "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -format=text \
            -show-line-counts-or-regions \
            -show-instantiations=false \
            "$SOURCE" > "$REPORT_DIR/coverage.txt" 2>&1 || echo "Text report generation failed"
        
        # Generate summary
        echo "Generating summary..."
        "$LLVM_COV_PATH" report "$EXE_FILE" \
            -instr-profile="$PROFDATA_FILE" \
            -show-region-summary=false \
            "$SOURCE" > "$REPORT_DIR/summary.txt" 2>&1 || echo "Summary generation failed"
    else
        echo "Error: Profile data file not available"
    fi
    
    echo "=== Coverage Report Generated ==="
    echo "HTML report: $REPORT_DIR/index.html"
    echo "Text report: $REPORT_DIR/coverage.txt"
    echo "Summary: $REPORT_DIR/summary.txt"
else
    echo ""
    echo "Note: Coverage data not found. Run './deploy.sh sourcebased' to generate coverage data."
fi
