#!/bin/sh
# Build, deploy, and run weak demos on device. Headers: API 22 (--sysroot). Libs: API 16 (-L).
set -e

cd "$(dirname -- "$0")"
CXX="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++"
RES="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19"
SYS22="/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot"
SYS16="/Applications/DevEco-Studio-5.0.11.110.app/Contents/sdk/default/openharmony/native/sysroot"
L16="-L$SYS16/usr/lib/aarch64-linux-ohos"
DEVICE_DIR="/data/local/tmp"

# --- Build ---
# Weak only (no -lhitrace_ndk.z)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 main.cpp -o main_weak

# Weak with -lhitrace_ndk.z (API 16 .so has no symbol → still weak)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 -lhitrace_ndk.z main.cpp -o main_weak_l

# Weak + lazy binding (-Wl,-z,lazy)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 -lhitrace_ndk.z -Wl,-z,lazy main.cpp -o main_weak_zlazy

# Weak + BIND_NOW (-Wl,-z,now)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 -lhitrace_ndk.z -Wl,-z,now main.cpp -o main_weak_znow

# a.2: same source linked with API 22 lib → strong
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS22/usr/lib/aarch64-linux-ohos" "$SYS22/usr/lib/aarch64-linux-ohos/libhitrace_ndk.z.so" \
  main.cpp -o main_strong

# call then dlopen (a.1: weak def, call before/after dlopen — second call still weak)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 main_call_then_dlopen.cpp -o main_call_then_dlopen -ldl -Wl,-z,lazy

# dlopen then call (dlsym + call through pointer)
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  $L16 main_dlopen_then_call.cpp -o main_dlopen_then_call -ldl -Wl,-z,lazy

# --- Deploy ---
EXES="main_weak main_weak_l main_weak_zlazy main_weak_znow main_strong main_call_then_dlopen main_dlopen_then_call"
if ! hdc list targets | grep -q .; then
  echo "No device: run 'hdc list targets'" >&2
  exit 1
fi
echo "Device API: $(hdc shell param get const.ohos.apiversion 2>/dev/null | tr -d '\r\n')"
for exe in $EXES; do
  [ -f "$exe" ] || continue
  hdc file send "$exe" "$DEVICE_DIR/$exe"
  hdc shell chmod 777 "$DEVICE_DIR/$exe"
done

# --- Run ---
for exe in $EXES; do
  [ -f "$exe" ] || continue
  echo "--- $exe ---"
  hdc shell "$DEVICE_DIR/$exe 2>&1; echo EXIT=\$?"
done
