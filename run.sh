#!/bin/bash
set -e

echo "=== Kotlin Native Metadata Klib Demo ==="

# Step 1: Publish lib to mavenLocal
echo ">>> Step 1: Publishing lib to mavenLocal..."
./gradlew :lib:publishToMavenLocal --quiet

# Step 2: Build app shared library for ohosArm64
echo ">>> Step 2: Building app shared library for ohosArm64..."
./gradlew :app:linkAppDebugSharedOhosArm64 --quiet

# Step 3: Build c-caller
echo ">>> Step 3: Building c-caller for OHOS aarch64..."
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

# Step 4: Check device connection
echo ">>> Step 4: Checking OHOS device connection..."
if ! hdc list targets | grep -q .; then
    echo "    ERROR: No OHOS device connected!"
    echo "    Please connect a device and try again."
    exit 1
fi
DEVICE=$(hdc list targets | head -1)
echo "    Found device: $DEVICE"

# Step 5: Deploy to device
echo ">>> Step 5: Deploying to device..."
hdc file send c-caller/main /data/local/tmp/main
hdc file send app/build/bin/ohosArm64/appDebugShared/libapp.so /data/local/tmp/libapp.so
hdc shell chmod 777 /data/local/tmp/main

# Step 6: Run on device
echo ">>> Step 6: Running on OHOS device..."
hdc shell "cd /data/local/tmp && LD_LIBRARY_PATH=. ./main"
