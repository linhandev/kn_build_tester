#!/bin/bash
# Compile hitrace_only.kt (selected lines 10-16 of GcovTest.kt) to kexe, run on device
# Uses same Kotlin version as project: 2.2.21-OH.0.1.0-01
set -e

COMPILER="$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-OH.0.1.0-01/bin/kotlinc-native"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Compiling hitrace_only.kt ==="
"$COMPILER" "$SCRIPT_DIR/hitrace_only.kt" -target ohos_arm64 -o hitrace_only -g

LIB_DIR="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/aarch64-linux-ohos"
echo "=== Deploying to device ==="
hdc file send "$SCRIPT_DIR/hitrace_only.kexe" /data/local/tmp/hitrace_only
hdc file send "$LIB_DIR/libc++_shared.so" /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/hitrace_only /data/local/tmp/libc++_shared.so

echo "=== Running on device ==="
hdc shell "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/hitrace_only"
