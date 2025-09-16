#!/bin/bash

set -ex

./gradlew linkDebugSharedOhosArm64

cd c-caller
/Users/user/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
      --sysroot /Users/user/.konan/dependencies/sysroot-ohos-aarch64-5.0.11.110 \
      --target=aarch64-linux-ohos \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
      -I../build/bin/ohosArm64/debugShared \
      -o main main.cpp \
      -L../build/bin/ohosArm64/debugShared \
      -lc2k

file main

cd -

hdc file send build/bin/ohosArm64/debugShared/libc2k.so /data/
hdc file send c-caller/main /data/
hdc shell chmod 777 /data/main
hdc shell LD_LIBRARY_PATH=/data/ /data/main
