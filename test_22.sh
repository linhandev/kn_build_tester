#!/bin/sh
set -e
SCRIPT_DIR="$(cd "$(dirname -- "$0")" && pwd)"
DEVECO_APP="/Applications/DevEco-Studio-6.0.2.636.app"
CXX="$DEVECO_APP/Contents/sdk/default/openharmony/native/llvm/bin/clang++"
SYS22="$DEVECO_APP/Contents/sdk/default/openharmony/native/sysroot"
RES="$DEVECO_APP/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4"
OUT="$SCRIPT_DIR/out"
DEVICE_DIR="/data/local/tmp"

mkdir -p "$OUT"
cat > "$OUT/main.cpp" << 'MAIN_CPP'
#include <iostream>
#include <hitrace/trace.h>
#include <deviceinfo.h>

int main() {
    std::cout << "Before OH_HiTrace_IsTraceEnabled\n";
    
    uint32_t apiVersion = OH_GetSdkApiVersion();
    if (apiVersion >= 19) {
        std::cout << "API version is 19 or higher, calling OH_HiTrace_IsTraceEnabled\n";
        #if __OHOS_API__ >= 19 
        bool r = OH_HiTrace_IsTraceEnabled(); // @since 19
        std::cout << "Called OH_HiTrace_IsTraceEnabled, trace_enabled=" << r << "\n";
        #endif
    } else {
        std::cout << "API version is less than 19";
    }
    return 0;
}
MAIN_CPP

"$CXX" -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" -Wl,-z,lazy -ldeviceinfo_ndk.z -lhitrace_ndk.z "$OUT/main.cpp" -o "$OUT/main_zlazy"
hdc file send "$OUT/main_zlazy" "$DEVICE_DIR/main_zlazy"
hdc shell chmod 777 "$DEVICE_DIR/main_zlazy"

echo "--- main_zlazy ---"
echo "Connected to api $(hdc shell param get const.ohos.apiversion) device"
hdc shell "$DEVICE_DIR/main_zlazy 2>&1; echo EXIT=\$?"
