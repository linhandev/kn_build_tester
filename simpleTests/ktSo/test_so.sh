#!/bin/bash
# Test Kotlin .so linked with C driver (direct linking, no dlopen) with GCOV
set -e

COMPILER="/Users/hl/git/kmp/KuiklyBase-kotlin/kotlin-native/dist/bin/kotlinc-native"
CLANG="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang++"
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"
LLVM_COV="/Users/hl/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-cov"

echo "=== Compile Kotlin .so ==="
$COMPILER c_driver_test.kt -target ohos_arm64 -o libktdemo -g -Xbinary=coverage=true -produce dynamic

echo "=== Compile C Driver ==="
# Link directly with libktdemo (no dlopen needed)
$CLANG c_driver.cpp -o c_driver --sysroot=$SYSROOT -target aarch64-linux-ohos \
    -I. -L. -lktdemo -Wl,-rpath,.

echo "=== Deploy ==="
hdc file send libktdemo.so /data/local/tmp/
hdc file send libktdemo.gcno /data/local/tmp/
hdc file send c_driver /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/c_driver /data/local/tmp/libktdemo.so
# Set library path for runtime

echo "=== Run ==="
# Run with library path set - GCOV environment is set in constructor
hdc shell "cd /data/local/tmp && rm -rf gcov_c_driver && export LD_LIBRARY_PATH=/data/local/tmp && ./c_driver"

echo "=== Retrieve ==="
# Find .gcda file - handle paths with spaces and special characters
GCDA_PATH=$(hdc shell "find /data/local/tmp/gcov_c_driver -name '*.gcda' 2>/dev/null | head -1" | tr -d '\r\n' | xargs)
if [ -n "$GCDA_PATH" ]; then
    GCDA_NAME=$(basename "$GCDA_PATH")
    hdc file recv "$GCDA_PATH" "$GCDA_NAME"
    $LLVM_COV gcov c_driver_test.kt --gcno libktdemo.gcno --gcda "$GCDA_NAME" 2>&1 | grep -E "(c_driver_test|File|Lines|Creating)" || true
    echo ""
    if [ -f "c_driver_test.kt.gcov" ]; then
        cat c_driver_test.kt.gcov
        echo "✅ Coverage report: c_driver_test.kt.gcov"
    else
        echo "⚠️  Coverage file not found"
    fi
else
    echo "❌ No .gcda found in /data/local/tmp/gcov_c_driver"
    echo "   Check device: hdc shell 'ls -la /data/local/tmp/gcov_c_driver/'"
fi
