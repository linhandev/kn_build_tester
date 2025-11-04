#!/bin/bash

set -ex

rm -rf bizA/build/ bizB/build/

BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

./gradlew :bizA:link${BUILD_MODE_CAPITALIZED}SharedOhosArm64

BIZ_A_BIN_DIR=bizA/build/bin/ohosArm64/${BUILD_MODE}Shared

cd c-caller
/Volumes/disk/cache/konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
      --sysroot /Volumes/disk/cache/konan/dependencies/sysroot-ohos-aarch64-5.0.11.110 \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 -lc++ \
      -I../${BIZ_A_BIN_DIR} \
      -L../${BIZ_A_BIN_DIR} \
      -lbizA \
      -o main main.cpp

file main

cd -

hdc file send ${BIZ_A_BIN_DIR}/libbizA.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main
