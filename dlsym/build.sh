#!/bin/sh
# Phase 2b: dlopen/dlsym only (no direct ref); and direct link + -z now for b.3.
set -e
cd "$(dirname -- "$0")"
CXX="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++"
SYS22="/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot"
RES="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19"

# dlopen/dlsym only
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS22/usr/lib/aarch64-linux-ohos" main.cpp -o main_dlsym -ldl

"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS22/usr/lib/aarch64-linux-ohos" -Wl,-z,lazy main.cpp -o main_dlsym_lazy -ldl

# b.3: direct ref + -z now (link libhitrace, expect load fail on API 16 when symbol missing)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS22/usr/lib/aarch64-linux-ohos" "$SYS22/usr/lib/aarch64-linux-ohos/libhitrace_ndk.z.so" \
  -Wl,-z,now ../not_protected/main.cpp -o main_direct_znow
