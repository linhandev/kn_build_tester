#!/bin/bash

set -ex

rm -rf build/bin/

BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

./gradlew link${BUILD_MODE_CAPITALIZED}SharedBizA

cd c-caller
/Users/user/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
      --sysroot /Volumes/disk/cache/konan/dependencies/sysroot-ohos-aarch64-6.0.0.858-02 \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
      -I../build/bin/bizA/${BUILD_MODE}Shared \
      -L../build/bin/bizA/${BUILD_MODE}Shared \
      -lbizA \
      -o main main.cpp

file main

cd -

hdc file send build/bin/bizA/${BUILD_MODE}Shared/libbizA.so /data/local/tmp/
hdc file send build/bin/bizB/${BUILD_MODE}Shared/libbizB.so /data/local/tmp/
hdc file send c-caller/main /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main
