#!/bin/bash

set -ex

rm -rf build/bin/

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}
BUILD_MODE=release
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

./gradlew link${BUILD_MODE_CAPITALIZED}SharedOhosArm64 --rerun-tasks

cd c-caller
${KONAN_DATA_DIR}/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
      --sysroot ${KONAN_DATA_DIR}/dependencies/sysroot-ohos-aarch64-5.0.11.110 \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
      -I../kotlinApp/build/bin/ohosArm64/${BUILD_MODE}Shared \
      -o main main.cpp \
      -L../kotlinApp/build/bin/ohosArm64/${BUILD_MODE}Shared \
      -lc2k

file main

cd -

hdc file send kotlinApp/build/bin/ohosArm64/${BUILD_MODE}Shared/libc2k.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main
