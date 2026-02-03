# not_protected: OH_HiTrace_IsTraceEnabled (unprotected)

**Not protected:** Direct call to the API, no weak stub or dlsym. Links **libhitrace_ndk.z.so**; on API 16 load would fail (EXIT=127) when symbol is missing.

**Result:** Exe calls [OH_HiTrace_IsTraceEnabled](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-trace-h#oh_hitrace_istraceenabled), links **libhitrace_ndk.z.so** and **libc.so** only. Deploy exe only; run without `LD_LIBRARY_PATH`. Loader uses `/system/lib64/ndk/libhitrace_ndk.z.so`. **EXIT=0**, prints `trace_enabled=0` or `trace_enabled=1`.

## Build (C++)

```bash
cd "$(git rev-parse --show-toplevel)/experiments/not_protected"
CXX=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++
SYS22=/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot
RES=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19
mkdir -p out
$CXX -target aarch64-linux-ohos --sysroot="$SYS22" -resource-dir="$RES" \
  -L"$SYS22/usr/lib/aarch64-linux-ohos" "$SYS22/usr/lib/aarch64-linux-ohos/libhitrace_ndk.z.so" \
  main.cpp -o out/main
```

Exe NEEDED: `libhitrace_ndk.z.so`, `libc.so`.

## Deploy and run

```bash
hdc file send out/main /data/local/tmp/main
hdc shell chmod 777 /data/local/tmp/main
hdc shell "/data/local/tmp/main 2>&1; echo EXIT=\$?"
```

Or: `sh build.sh`.

**Output (API 19+):** `trace_enabled=0` (or `1`), **EXIT=0**.

**API 16:** Run fails with **EXIT=127**, loader error: `Error relocating /data/local/tmp/main: OH_HiTrace_IsTraceEnabled: symbol not found`. OH_HiTrace_IsTraceEnabled is since API 19; on API 16 the on-device libhitrace_ndk.z.so does not export the symbol.

Device API: `hdc shell param get const.ohos.apiversion`.
