#!/bin/sh
# Combined: weak stub + dlsym call path; BIND_NOW. API 22 headers; API 16 lib dir (no hybrid).
set -e
cd "$(dirname -- "$0")"
CXX="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++"
RES="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19"
SYS22="/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot"
SYS16="/Applications/DevEco-Studio-5.0.11.110.app/Contents/sdk/default/openharmony/native/sysroot"

"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS16/usr/lib/aarch64-linux-ohos" -Wl,-z,now \
  main.cpp -o main_combined -ldl
