#!/bin/bash

set -ex

rm -rf build/bin/

BUILD_MODE=release
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

./gradlew link${BUILD_MODE_CAPITALIZED}SharedOhosArm64 --rerun-tasks

cd c-caller
/Users/user/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
      --sysroot /Users/user/.konan/dependencies/sysroot-ohos-aarch64-5.0.11.110 \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
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
