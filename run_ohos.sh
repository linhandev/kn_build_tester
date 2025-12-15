#!/bin/bash

set -ex

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}

LLVM_PATH="${KONAN_DATA_DIR}/dependencies/llvm-19.1.7-aarch64-macos-ohos-2"
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"
TARGET="aarch64-linux-ohos"

echo "Compiling"

"${LLVM_PATH}/bin/clang++" \
      --sysroot "${SYSROOT}" \
      --target="${TARGET}" \
      -g \
      -Wall -Wextra \
      -std=c++17 \
      -o protected_buffer_demo protected_buffer_demo.cpp

file protected_buffer_demo

echo "Deploying to device..."
hdc file send protected_buffer_demo /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/protected_buffer_demo

echo "Running demo on device..."
hdc shell /data/local/tmp/protected_buffer_demo
