#!/bin/bash

set -euo pipefail
set -x

rm -rf build/bin/

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}
BUILD_MODE=release
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

SYSROOT="/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot"

# Build the shared library with Gradle (keep wrapper for now).
./gradlew link"${BUILD_MODE_CAPITALIZED}"SharedOhosArm64 --rerun-tasks

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

file main

cd -

hdc file send build/bin/ohosArm64/${BUILD_MODE}Shared/libc2k.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main
