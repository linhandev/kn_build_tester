#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "${PROJECT_ROOT}"

rm -rf build/bin/

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}
BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"

# Build the static library for add (used by the published cinterop klib)
cd add/src/nativeInterop/add
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --sysroot "${SYSROOT}" \
      --target=aarch64-linux-ohos \
      -fPIC \
      -c add.cpp -o add.o
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/llvm-ar" rcs libadd.a add.o
cd -

# Publish add (cinterop klib with static lib) to in-repo Maven
./gradlew :add:publishOhosArm64PublicationToInRepoRepository

# Build the KN shared library (libc2k.so)
./gradlew link"${BUILD_MODE_CAPITALIZED}"SharedOhosArm64 --rerun-tasks

# Build the C driver that links to libc2k.so
cd c-caller
"/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang++" \
      --sysroot "${SYSROOT}" \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
      -isystem "${SYSROOT}/usr/include" \
      -isystem "${SYSROOT}/usr/include/aarch64-linux-ohos" \
      -I"${SYSROOT}/include" \
      -I../build/bin/ohosArm64/${BUILD_MODE}Shared \
      -o main main.cpp \
      -L../build/bin/ohosArm64/${BUILD_MODE}Shared \
      -lc2k
cd -

# Deploy and run on OHOS device
hdc shell rm /data/local/tmp/*
hdc file send build/bin/ohosArm64/${BUILD_MODE}Shared/libc2k.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main

hdc shell file /data/local/tmp/libc2k.so
hdc shell file /data/local/tmp/main
