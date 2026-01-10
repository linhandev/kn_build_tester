#!/bin/bash

# Common deploy and test script for coverage demos
# Usage: deploy.sh [gcov|sourcebased]

set -e

COVERAGE_TYPE="${1:-gcov}"  # Default to gcov if not specified

if [ "$COVERAGE_TYPE" = "sourcebased" ]; then
    EXE_FILE="main_sourcebased"
    PROFRAW_FILE="default_sourcebased.profraw"
    DEVICE_PROFRAW="/data/local/tmp/default_sourcebased.profraw"
else
    EXE_FILE="main_gcov"
    GCNO_FILE="main_gcov.gcno"
    DEVICE_GCDA="/data/local/tmp/main_gcov.gcda"
    DEVICE_GCNO="/data/local/tmp/main_gcov.gcno"
fi

DEVICE_PATH="/data/local/tmp"
DEVICE_EXE="$DEVICE_PATH/$EXE_FILE"

echo "=== Deploying to OHOS device ($COVERAGE_TYPE coverage) ==="

# Check if executable exists
if [ ! -f "$EXE_FILE" ]; then
    echo "Error: $EXE_FILE not found. Please run ./gcov.sh or ./sourcebased.sh first."
    exit 1
fi

# Send executable to device
hdc file send "$EXE_FILE" "$DEVICE_EXE"
hdc shell chmod 777 "$DEVICE_EXE"

# For GCOV, also send the .gcno file (generated at compile time)
if [ "$COVERAGE_TYPE" = "gcov" ]; then
    if [ ! -f "$GCNO_FILE" ]; then
        echo "Warning: $GCNO_FILE not found. Coverage data may not work correctly."
    else
        echo "Sending $GCNO_FILE to device..."
        hdc file send "$GCNO_FILE" "$DEVICE_GCNO"
        hdc shell chmod 644 "$DEVICE_GCNO"
    fi
fi

echo "=== Running on device ==="
if [ "$COVERAGE_TYPE" = "sourcebased" ]; then
    hdc shell "cd $DEVICE_PATH && LD_LIBRARY_PATH=$DEVICE_PATH LLVM_PROFILE_FILE=$DEVICE_PROFRAW $DEVICE_EXE 20 4"
else
    # For GCOV, use GCOV_PREFIX to redirect coverage files to device path
    # GCOV_PREFIX_STRIP strips N leading path components from the original path
    # Using a high number (99) to strip all build path components, leaving just the filename
    # This makes GCOV write files to /data/local/tmp/main_gcov.gcda
    hdc shell "cd $DEVICE_PATH && LD_LIBRARY_PATH=$DEVICE_PATH GCOV_PREFIX=$DEVICE_PATH GCOV_PREFIX_STRIP=99 $DEVICE_EXE 20 4"
fi

echo "=== Copying coverage data files from device ==="
if [ "$COVERAGE_TYPE" = "sourcebased" ]; then
    hdc file recv "$DEVICE_PROFRAW" ./"$PROFRAW_FILE" 2>/dev/null || echo "Note: profraw file may not exist yet"
    if [ -f "$PROFRAW_FILE" ]; then
        echo "Successfully copied $PROFRAW_FILE"
    fi
else
    # Try to copy .gcda from expected location
    hdc file recv "$DEVICE_GCDA" ./main_gcov.gcda 2>/dev/null
    if [ -f "main_gcov.gcda" ]; then
        echo "Successfully copied main_gcov.gcda"
    else
        # If not found, search for gcda files on device (in case path stripping didn't work)
        echo "Searching for gcda files on device..."
        GCDA_FOUND=$(hdc shell "find $DEVICE_PATH -name '*.gcda' 2>/dev/null | head -1" | tr -d '\r\n')
        if [ -n "$GCDA_FOUND" ] && [ "$GCDA_FOUND" != "" ]; then
            echo "Found gcda file at: $GCDA_FOUND"
            hdc file recv "$GCDA_FOUND" ./main_gcov.gcda 2>/dev/null && echo "Successfully copied main_gcov.gcda" || echo "Failed to copy gcda file"
        else
            echo "Note: gcda file not found on device"
        fi
    fi
fi

echo "=== Deployment complete ==="
