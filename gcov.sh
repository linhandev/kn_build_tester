#!/bin/bash

# GCOV Coverage Build Script for OHOS
# Full process: Build -> Deploy -> Generate Report

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
GCNO_FILE="main_gcov.gcno"
REPORT_DIR="coverage_report_gcov"

DEVICE_PATH="/data/local/tmp"
DEVICE_EXE="$DEVICE_PATH/$EXE_FILE"
DEVICE_GCDA="$DEVICE_PATH/main_gcov.gcda"
DEVICE_GCNO="$DEVICE_PATH/main_gcov.gcno"

echo "=== BUILD PHASE ==="
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

echo ""
echo "=== DEPLOY PHASE ==="
echo "=== Deploying to OHOS device ==="

# Send executable to device
hdc file send "$EXE_FILE" "$DEVICE_EXE"
hdc shell chmod 777 "$DEVICE_EXE"

# Send the .gcno file (generated at compile time)
if [ ! -f "$GCNO_FILE" ]; then
    echo "Warning: $GCNO_FILE not found. Coverage data may not work correctly."
else
    echo "Sending $GCNO_FILE to device..."
    hdc file send "$GCNO_FILE" "$DEVICE_GCNO"
    hdc shell chmod 644 "$DEVICE_GCNO"
fi

echo "=== Running on device ==="
# Clean up any old gcda files on device to avoid merge conflicts
echo "Cleaning old coverage data on device..."
hdc shell "rm -f $DEVICE_PATH/*.gcda 2>/dev/null" || true

# For GCOV, use GCOV_PREFIX to redirect coverage files to device path
# GCOV_PREFIX_STRIP strips N leading path components from the original path
# Using a high number (99) to strip all build path components, leaving just the filename
# This makes GCOV write files to /data/local/tmp/main_gcov.gcda
hdc shell "cd $DEVICE_PATH && LD_LIBRARY_PATH=$DEVICE_PATH GCOV_PREFIX=$DEVICE_PATH GCOV_PREFIX_STRIP=99 $DEVICE_EXE 20 4"

echo "=== Copying coverage data files from device ==="
# Remove old gcda file if it exists to ensure we get fresh data
rm -f main_gcov.gcda 2>/dev/null || true

# Try to copy .gcda from expected location
hdc file recv "$DEVICE_GCDA" ./main_gcov.gcda 2>/dev/null
if [ -f "main_gcov.gcda" ]; then
    echo "Successfully copied main_gcov.gcda"
    # Verify the file size (should be > 0)
    GCDA_SIZE=$(stat -f%z main_gcov.gcda 2>/dev/null || stat -c%s main_gcov.gcda 2>/dev/null || echo "0")
    if [ "$GCDA_SIZE" -eq 0 ]; then
        echo "Warning: Copied gcda file is empty"
    fi
else
    # If not found, search for gcda files on device (in case path stripping didn't work)
    echo "Searching for gcda files on device..."
    GCDA_FOUND=$(hdc shell "find $DEVICE_PATH -name '*.gcda' 2>/dev/null | head -1" | tr -d '\r\n')
    if [ -n "$GCDA_FOUND" ] && [ "$GCDA_FOUND" != "" ]; then
        echo "Found gcda file at: $GCDA_FOUND"
        hdc file recv "$GCDA_FOUND" ./main_gcov.gcda 2>/dev/null && echo "Successfully copied main_gcov.gcda" || echo "Failed to copy gcda file"
    else
        echo "Warning: gcda file not found on device"
    fi
fi

echo ""
echo "=== REPORT GENERATION PHASE ==="
# Generate report if coverage data exists
if [ -f "main_gcov.gcda" ]; then
    echo "=== Generating GCOV Coverage Report ==="
    
    # Create report directory
    mkdir -p "$REPORT_DIR"
    
    # Try using system gcov (needs gcno and gcda files with matching base name)
    if command -v gcov >/dev/null 2>&1 && [ -f "main_gcov.gcno" ] && [ -f "main_gcov.gcda" ]; then
        echo "Using system gcov..."
        # gcov expects files named after the source, so create symlinks or copy
        # Use the actual gcno and gcda files with the correct names
        cp main_gcov.gcno main.gcno 2>/dev/null || true
        cp main_gcov.gcda main.gcda 2>/dev/null || true
        
        # Verify the files match before running gcov
        echo "Verifying coverage data files..."
        if [ -f "main.gcno" ] && [ -f "main.gcda" ]; then
            gcov "$SOURCE" 2>&1 | tee "$REPORT_DIR/gcov_output.txt" || true
            
            # Check if the report was generated successfully
            if [ -f "main.cpp.gcov" ]; then
                # Check if we got valid coverage data (not all #####)
                EXECUTED_LINES=$(grep -E "^[[:space:]]*[0-9]+:" main.cpp.gcov | wc -l | tr -d ' ')
                if [ "$EXECUTED_LINES" -gt 0 ]; then
                    echo "Valid coverage data found: $EXECUTED_LINES executed lines"
                else
                    echo "Warning: Coverage report generated but shows 0% coverage"
                    echo "This may indicate a mismatch between .gcno and .gcda files"
                fi
            fi
        fi
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
    echo "Warning: Coverage data (main_gcov.gcda) not found. Report generation skipped."
fi

echo ""
echo "=== Complete ==="
