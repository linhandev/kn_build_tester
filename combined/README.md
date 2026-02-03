# combined: Weak stub + dlsym call path

Weak stub satisfies BIND_NOW; call path via dlsym (catchable when symbol missing). **API 22 headers**; **API 16 lib dir** (no hybrid) so weak is used.

**API:** [OH_HiTrace_IsTraceEnabled](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-trace-h#oh_hitrace_istraceenabled). Device lib: `/system/lib64/ndk/libhitrace_ndk.z.so`.

**Device API version:** `hdc shell param get const.ohos.apiversion`.

## Build

```bash
cd "$(git rev-parse --show-toplevel)/experiments/combined"
sh build.sh
```

## Results

| Binary       | API 16 | API 21 |
|--------------|--------|--------|
| main_combined| Loads; trace_enabled=-2 (dlopen failed) — catchable | Loads; trace_enabled=0 or 1 (dlsym → real call) |

**c.1:** Weak fallback requires linker not seeing symbol as strong (API 22 headers + API 16 lib dir, no `-lhitrace_ndk.z`).

**Deploy/run:** `hdc file send main_combined /data/local/tmp/main_combined` then `hdc shell "/data/local/tmp/main_combined 2>&1; echo EXIT=\$?"`
