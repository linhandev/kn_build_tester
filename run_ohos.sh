#!/bin/bash

set -e

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}

LLVM_PATH="${KONAN_DATA_DIR}/dependencies/llvm-19.1.7-aarch64-macos-ohos-2"
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"
TARGET="aarch64-linux-ohos"
CLANG="${LLVM_PATH}/bin/clang++"

echo "--- Test 1: Manual Electric Fence (mprotect) ---"
echo "Compiling without ASan..."
"${CLANG}" \
      --sysroot "${SYSROOT}" \
      --target="${TARGET}" \
      -g \
      -Wall -Wextra \
      -std=c++17 \
      -lhilog_ndk.z \
      -o protected_buffer_demo protected_buffer_demo.cpp

echo "Deploying to device..."
hdc file send protected_buffer_demo /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/protected_buffer_demo

echo "Running demo on device..."
hdc shell /data/local/tmp/protected_buffer_demo

echo ""
echo "--- Test 2: Stock ASan ---"
echo "Compiling with ASan..."
"${CLANG}" \
      --sysroot "${SYSROOT}" \
      --target="${TARGET}" \
      -fsanitize=address \
      -g \
      -Wall -Wextra \
      -std=c++17 \
      -lhilog_ndk.z \
      -o protected_buffer_demo_asan protected_buffer_demo.cpp

echo "Deploying to device..."
hdc file send protected_buffer_demo_asan /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/protected_buffer_demo_asan

echo "Running demo with ASan on device..."
hdc shell /data/local/tmp/protected_buffer_demo_asan
