# dlsym: dlopen/dlsym only (OH_HiTrace_IsTraceEnabled)

Exe uses only `dlopen("libhitrace_ndk.z.so")` and `dlsym("OH_HiTrace_IsTraceEnabled")`; no direct reference, no `-lhitrace_ndk.z`. Catchable: dlopen/dlsym failure returns -2/-1.

**API:** [OH_HiTrace_IsTraceEnabled](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-trace-h#oh_hitrace_istraceenabled). Device lib: `/system/lib64/ndk/libhitrace_ndk.z.so`.

**Device API version:** `hdc shell param get const.ohos.apiversion`.

## Build (copy-paste from repo root)

```bash
cd "$(git rev-parse --show-toplevel)/experiments/dlsym"
CXX=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++
SYS22=/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot
RES=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19

$CXX -target aarch64-linux-ohos --sysroot=$SYS22 -resource-dir=$RES -L$SYS22/usr/lib/aarch64-linux-ohos main.cpp -o main_dlsym -ldl

$CXX -target aarch64-linux-ohos --sysroot=$SYS22 -resource-dir=$RES -L$SYS22/usr/lib/aarch64-linux-ohos -Wl,-z,lazy main.cpp -o main_dlsym_lazy -ldl

$CXX -target aarch64-linux-ohos --sysroot=$SYS22 -resource-dir=$RES -L$SYS22/usr/lib/aarch64-linux-ohos "$SYS22/usr/lib/aarch64-linux-ohos/libhitrace_ndk.z.so" -Wl,-z,now ../not_protected/main.cpp -o main_direct_znow
```

Or: `sh build.sh`.

## Results

| Binary           | API 16 | API 21 |
|------------------|--------|--------|
| main_dlsym        | Loads; trace_enabled=-2 (dlopen failed) or -1 (dlsym failed) — catchable | Loads; trace_enabled=0 or 1 (real call) |
| main_dlsym_lazy   | Same | Same |
| main_direct_znow  | Load-time fail (EXIT=127, symbol not found) — b.3 | Loads; trace_enabled=0 or 1 |

**b.3:** main_direct_znow links `-lhitrace_ndk.z` and calls symbol directly with `-z now`; on API 16 device lib lacks symbol → load fails (not catchable).

**Deploy/run:** `hdc file send main_dlsym /data/local/tmp/main_dlsym` then `hdc shell "/data/local/tmp/main_dlsym 2>&1; echo EXIT=\$?"`
