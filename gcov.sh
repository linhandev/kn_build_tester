#!/bin/bash

# GCOV Coverage Build Script for OHOS
# Build process: IR -> .o -> exe, then generate report if coverage data exists

set -e

CLANG_PATH="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/clang++"
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"
RESOURCE_DIR="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4"
CLANG_LIB_DIR="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4/lib/aarch64-linux-ohos"
TARGET="aarch64-linux-ohos"

GCOV_PATH="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-cov"
GCOV_TOOL="$HOME/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-gcov"

SOURCE="main.cpp"
IR_FILE="main_gcov.ll"
OBJ_FILE="main_gcov.o"
EXE_FILE="main_gcov"
REPORT_DIR="coverage_report_gcov"

echo "=== Step 1: Emit IR with GCOV instrumentation ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -emit-llvm -S \
  -O0 \
  "$SOURCE" \
  -target "$TARGET" \
  -resource-dir "$RESOURCE_DIR" \
  -o "main.ll"

"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-arcs -ftest-coverage \
  -emit-llvm -S \
  -O0 \
  "$SOURCE" \
  -target "$TARGET" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$IR_FILE"

echo "=== Step 2: Compile IR to Object File ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-arcs -ftest-coverage \
  -c "$IR_FILE" \
  -O0 \
  -target "$TARGET" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$OBJ_FILE"

echo "=== Step 3: Link Object File to Executable ==="
"$CLANG_PATH" \
  --sysroot "$SYSROOT" \
  -fprofile-arcs -ftest-coverage \
  -O0 \
  "$OBJ_FILE" \
  -target "$TARGET" \
  -L"$CLANG_LIB_DIR" \
  -resource-dir "$RESOURCE_DIR" \
  -o "$EXE_FILE"

echo "=== Build Complete ==="
echo "Executable: $EXE_FILE"
echo "IR file: $IR_FILE (contains GCOV instrumentation)"
echo "Object file: $OBJ_FILE"

# Generate report if coverage data exists
if [ -f "main_gcov.gcda" ]; then
    echo ""
    echo "=== Generating GCOV Coverage Report ==="
    
    # Create report directory
    mkdir -p "$REPORT_DIR"
    
    # Try using system gcov (needs gcno and gcda files with matching base name)
    if command -v gcov >/dev/null 2>&1 && [ -f "main_gcov.gcno" ]; then
        echo "Using system gcov..."
        # gcov expects files named after the source, so create symlinks or copy
        cp main_gcov.gcno main.gcno 2>/dev/null || true
        cp main_gcov.gcda main.gcda 2>/dev/null || true
        gcov "$SOURCE" 2>&1 | tee "$REPORT_DIR/gcov_output.txt" || true
        # Clean up temporary files
        rm -f main.gcno main.gcda 2>/dev/null || true
    fi
    
    # Try using llvm-cov with gcov format (correct syntax)
    if [ -f "$GCOV_PATH" ] && [ -f "main_gcov.gcno" ]; then
        echo "Using llvm-cov for GCOV format..."
        # llvm-cov gcov needs the gcno and gcda files
        cp main_gcov.gcno main.gcno 2>/dev/null || true
        cp main_gcov.gcda main.gcda 2>/dev/null || true
        # llvm-cov gcov uses -o for output directory
        "$GCOV_PATH" gcov "$SOURCE" -o "$REPORT_DIR" 2>&1 | tee "$REPORT_DIR/llvm_cov_output.txt" || true
        rm -f main.gcno main.gcda 2>/dev/null || true
    fi
    
    # Try using llvm-gcov if available
    if [ -f "$GCOV_TOOL" ] && [ -f "main_gcov.gcno" ]; then
        echo "Using llvm-gcov..."
        cp main_gcov.gcno main.gcno 2>/dev/null || true
        cp main_gcov.gcda main.gcda 2>/dev/null || true
        "$GCOV_TOOL" "$SOURCE" > "$REPORT_DIR/coverage.txt" 2>&1 || true
        rm -f main.gcno main.gcda 2>/dev/null || true
    fi
    
    echo "=== Coverage Report Generated ==="
    echo "Report directory: $REPORT_DIR"
    if [ -f "main.cpp.gcov" ]; then
        echo "GCOV file: main.cpp.gcov"
    fi
    if [ -f "$REPORT_DIR/coverage.txt" ]; then
        echo "Text report: $REPORT_DIR/coverage.txt"
    fi
else
    echo ""
    echo "Note: Coverage data not found. Run './deploy.sh gcov' to generate coverage data."
fi
