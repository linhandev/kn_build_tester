#!/bin/bash

set -euo pipefail
set -x

rm -rf build/bin/iosSimulatorArm64/

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}
BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

# Find iOS SDK path
IOS_SDK_PATH=$(xcrun --sdk iphonesimulator --show-sdk-path)

# Build the static library for add for iOS simulator arm64
cd src/nativeInterop/add
# Clean up any old object files and archives to avoid mixing Linux and iOS objects
rm -f add_ios_sim_arm64.o libadd.a
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --target=arm64-apple-ios13.0-simulator \
      -isysroot "${IOS_SDK_PATH}" \
      -fPIC \
      -c add.cpp -o add_ios_sim_arm64.o
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/llvm-ar" rcs libadd.a add_ios_sim_arm64.o
cd -

# Build the static library for multiply for iOS simulator arm64
cd multiply/src/nativeInterop/multiply
# Clean up any old object files and archives to avoid mixing Linux and iOS objects
rm -f multiply_ios_sim_arm64.o libmultiply.a
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --target=arm64-apple-ios13.0-simulator \
      -isysroot "${IOS_SDK_PATH}" \
      -fPIC \
      -c multiply.cpp -o multiply_ios_sim_arm64.o
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/llvm-ar" rcs libmultiply.a multiply_ios_sim_arm64.o
cd -

# Publish the multiply klib to local Maven
./gradlew :multiply:publishToMavenLocal

# rm -rf /path/to/kotlin/kotlin-native/dist/klib/cache/ios_simulator_arm64-gSTATIC-pl/com.example*
# Build the framework with Gradle
./gradlew link"${BUILD_MODE_CAPITALIZED}"FrameworkIosSimulatorArm64 --rerun-tasks

# Create a simple test program to verify the framework works
FRAMEWORK_DIR="build/bin/iosSimulatorArm64/${BUILD_MODE}Framework"

cd c-caller
# Compile Objective-C test program for iOS simulator using xcrun clang for proper SDK setup
xcrun clang \
      -arch arm64 \
      -isysroot "${IOS_SDK_PATH}" \
      -mios-simulator-version-min=13.0 \
      -fPIC \
      -Wall -Wextra \
      -I"../${FRAMEWORK_DIR}/c2k.framework/Headers" \
      -o main_ios_sim main_ios.m \
      -F"../${FRAMEWORK_DIR}" \
      -framework c2k \
      -framework Foundation \
      -rpath "@executable_path/../${FRAMEWORK_DIR}"

cd -
echo ""
echo "Test program successfully compiled!"
echo "Running test program on iOS simulator..."
xcrun simctl spawn booted "$(pwd)/c-caller/main_ios_sim"

