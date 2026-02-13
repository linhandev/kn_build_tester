#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "${PROJECT_ROOT}"

DEVECO_SDK="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native"
LLVM_BIN="${DEVECO_SDK}/llvm/bin"
SYSROOT="${DEVECO_SDK}/sysroot"
BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

# Build the static library for add (used by the published cinterop klib)
cd add/src/nativeInterop/add
"${LLVM_BIN}/clang" \
      --sysroot "${SYSROOT}" \
      --target=aarch64-linux-ohos \
      -fPIC \
      -c add.c -o add.o
"${LLVM_BIN}/llvm-ar" rcs libadd.a add.o
cd -

# Publish add (cinterop klib + Kotlin) to in-repo Maven
./gradlew :add:publishOhosArm64PublicationToProjectRepoRepository

# Build the KN shared library (libc2k.so)
./gradlew link"${BUILD_MODE_CAPITALIZED}"SharedOhosArm64 --rerun-tasks

# Build the C driver that links to libc2k.so
cd c-caller
"${LLVM_BIN}/clang" \
      --sysroot "${SYSROOT}" \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c11 \
      -isystem "${SYSROOT}/usr/include" \
      -isystem "${SYSROOT}/usr/include/aarch64-linux-ohos" \
      -I"${SYSROOT}/include" \
      -I../build/bin/ohosArm64/${BUILD_MODE}Shared \
      -o main main.c \
      -L../build/bin/ohosArm64/${BUILD_MODE}Shared \
      -lc2k
cd -

# Deploy and run on OHOS device
hdc shell "rm -rf /data/local/tmp/*"
hdc file send build/bin/ohosArm64/${BUILD_MODE}Shared/libc2k.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main

hdc shell file /data/local/tmp/libc2k.so
hdc shell file /data/local/tmp/main
