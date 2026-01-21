#!/bin/bash
set -e

echo ">>> Step 1: Publishing lib to ./repo/..."
./gradlew :lib:publishAllPublicationsToLocalRepository --rerun-tasks --console=plain 
echo "    Published artifacts:"
find repo -name "*.jar" -o -name "*.klib" 2>/dev/null | head -6 | sed 's/^/      /'

echo ">>> Step 2: Compiling app commonMain (uses metadata klib from lib-1.0.0.jar)..."
./gradlew :app:compileCommonMainKotlinMetadata --rerun-tasks --console=plain 

echo ">>> Step 3: Building app shared library for ohosArm64 (uses platform klib)..."
./gradlew :app:linkAppDebugSharedOhosArm64 --rerun-tasks --console=plain 

echo ">>> Step 4: Building c-caller for OHOS aarch64..."
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang \
  --sysroot /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot \
  -target aarch64-linux-ohos \
  -L/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4/lib/aarch64-linux-ohos \
  -resource-dir /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4 \
  -I app/build/bin/ohosArm64/appDebugShared \
  c-caller/main.c \
  -L app/build/bin/ohosArm64/appDebugShared \
  -lapp \
  -o c-caller/main

echo ">>> Step 5: Checking OHOS device connection..."
if ! hdc list targets | grep -q .; then
    echo "    ERROR: No OHOS device connected!"
    echo "    Please connect a device and try again."
    exit 1
fi
DEVICE=$(hdc list targets | head -1)
echo "    Found device: $DEVICE"

echo ">>> Step 6: Deploying to device..."
hdc file send c-caller/main /data/local/tmp/main
hdc file send app/build/bin/ohosArm64/appDebugShared/libapp.so /data/local/tmp/libapp.so
hdc shell chmod 777 /data/local/tmp/main

echo ">>> Step 7: Running on OHOS device..."
hdc shell "cd /data/local/tmp && LD_LIBRARY_PATH=. ./main"
