#!/bin/bash

set -euo pipefail
set -x

rm -rf build/bin/

KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}
BUILD_MODE=debug
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')

# Build the static library for add for linux x86
cd src/nativeInterop/add
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --target=x86_64-linux-gnu \
      -fPIC \
      -c add.cpp -o add.o
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/llvm-ar" rcs libadd.a add.o
cd -

# Build the static library for multiply for linux x86
cd multiply/src/nativeInterop/multiply
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --target=x86_64-linux-gnu \
      -fPIC \
      -c multiply.cpp -o multiply.o
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/llvm-ar" rcs libmultiply.a multiply.o
cd -

# Publish the multiply klib to in-repo Maven (maven-repo/ in project root)
./gradlew :multiply:publishAllPublicationsToInRepoRepository

rm -rf /Users/hl/git/kmp/KuiklyBase-kotlin/kotlin-native/dist/klib/cache/linux_x64-gSTATIC-pl/com.example*
# Build the shared library with Gradle
./gradlew link"${BUILD_MODE_CAPITALIZED}"SharedLinuxX64 --rerun-tasks

cd c-caller
"${KONAN_DATA_DIR}/dependencies/llvm-11.1.0-aarch64-macos-essentials-60/bin/clang++" \
      --target=x86_64-linux-gnu \
      --sysroot=/Users/hl/.konan/dependencies/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2/x86_64-unknown-linux-gnu/sysroot \
      --gcc-toolchain=/Users/hl/.konan/dependencies/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2 \
      -fuse-ld=lld \
      -fPIC -pthread \
      -Wall -Wextra -std=c++17 \
      -I../build/bin/linuxX64/${BUILD_MODE}Shared \
      -o main main.cpp \
      -L../build/bin/linuxX64/${BUILD_MODE}Shared \
      -lc2k

cd -

docker run --platform linux/amd64 -v $(pwd):/workspace --rm ubuntu:24.04 /bin/bash -c "cd /workspace && LD_LIBRARY_PATH=build/bin/linuxX64/${BUILD_MODE}Shared ./c-caller/main"
