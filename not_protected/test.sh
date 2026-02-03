#!/bin/sh
set -e
cd "$(dirname -- "$0")"

echo "Connected to api $(hdc shell param get const.ohos.apiversion) device"

CXX="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++"
SYS22="/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot"
RES="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19"
OUT="out"
DEVICE_DIR="/data/local/tmp"

rm -rf "$OUT"
mkdir -p "$OUT"

# -z now
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" -lhitrace_ndk.z \
  -Wl,-z,now main.cpp -o "$OUT/main_znow"
hdc file send "$OUT/main_znow" "$DEVICE_DIR/main_znow"
hdc shell chmod 777 "$DEVICE_DIR/main_znow"
echo "--- main_znow ---"
hdc shell "$DEVICE_DIR/main_znow 2>&1; echo EXIT=\$?"

# -z lazy
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" -lhitrace_ndk.z \
  -Wl,-z,lazy main.cpp -o "$OUT/main_zlazy"
hdc file send "$OUT/main_zlazy" "$DEVICE_DIR/main_zlazy"
hdc shell chmod 777 "$DEVICE_DIR/main_zlazy"
echo "--- main_zlazy ---"
hdc shell "$DEVICE_DIR/main_zlazy 2>&1; echo EXIT=\$?"

# -z lazy, no -l
"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -Wl,-z,lazy -Wl,--unresolved-symbols=ignore-all main.cpp -o "$OUT/main_zlazy_nol"
hdc file send "$OUT/main_zlazy_nol" "$DEVICE_DIR/main_zlazy_nol"
hdc shell chmod 777 "$DEVICE_DIR/main_zlazy_nol"
echo "--- main_zlazy_nol ---"
hdc shell "$DEVICE_DIR/main_zlazy_nol 2>&1; echo EXIT=\$?"
