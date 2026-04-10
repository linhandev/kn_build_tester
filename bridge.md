## 桥接 / 拦截 HarmonyOS NDK C 调用（示例：`OH_LOG_PrintMsg`）

本示例用 HiLog 的 **`OH_LOG_PrintMsg`**（`hilog/log.h`）：**固定参数** `(type, level, domain, tag, message)`，整段字符串、无 `printf` 式格式化。NDK 标注 **API 18+**；`minAPI` 不足时需换符号或升版本。

典型流程：**`-Wl,--wrap=OH_LOG_PrintMsg`** → 调用点进 **`__wrap_OH_LOG_PrintMsg`** → 自增计数、改写或附加信息后 **`__real_OH_LOG_PrintMsg(...)`** 交给系统库。`__wrap_` 与调用点须 **同一次链接** 进目标 `.so`（源文件、`.o` 或由 bitcode 链入的对象均可）。

### 为何「再做一个导出同名符号的应用 `.so`」通常不合适

**`libc2k.so`（Kotlin/Native）** 已 **`DT_NEEDED`** **`libhilog_ndk.z.so`**，动态加载器往往先绑定系统 HiLog；靠第二个应用 `.so` 重复导出同名符号抢解析 **不可靠**。

### CMake（本仓库）

见 **`harmonyApp/entry/src/main/cpp/CMakeLists.txt`**：

- `add_library(entry SHARED napi_init.cpp log_caller.c log_hook.c)`
- `target_link_options(entry PRIVATE "-Wl,--wrap=OH_LOG_PrintMsg")`
- `target_link_libraries(entry PUBLIC ... hilog_ndk.z)`

### 本示例在做什么

- **`log_hook.c`**：`__wrap_` 里 **`[wrap #序号] 原 message`** 再 **`__real_`**。
- **`log_caller.c`**：连续 **`LOG_HOOK_SAMPLE_LINES`** 次 **`OH_LOG_PrintMsg`**（tag **`LogHookTest`**）。
- **NAPI `testOhLogHook`**（**`napi_init.cpp`**）：校验计数增量是否等于 **`entry_log_hook_sample_line_count()`**。

### 自动化断言（给后续迭代 / Agent）

连接设备且存在 **`hdc`** 时，**`./scripts/build-and-check-crash.sh`** 在安装并拉起应用后会执行：

`ASSERT_LOG_HOOK_STRICT=1 ./scripts/assert-log-hook-device.sh`

脚本从 **`log_caller.c`** 读取 **`#define LOG_HOOK_SAMPLE_LINES`**，在 **`hilog -T LogHookTest`** 输出中要求至少该数量的 **`entry_caller line`** 与 **`[wrap #`**；不满足则 **exit 1**（用于抓回归）。无设备时跳过。

单独运行 **`./scripts/assert-log-hook-device.sh`**（未设 **`ASSERT_LOG_HOOK_STRICT=1`**）且 HiLog 里 **完全没有** 匹配行时 **exit 0 跳过**，避免未先装包/拉起应用时的误报。

修改样本行数时：**只改** `LOG_HOOK_SAMPLE_LINES`（**`entry_log_hook_sample_line_count()`** 已同源）。

### 其他非可变参数的 HiLog 入口

- **`OH_LOG_PrintMsg`**：本示例所用。
- **`OH_LOG_PrintMsgByLen`**：带长度字段（API 18+）。

### 附注：可变参数 API（实际 CAPI 中占比很小，仅作备忘）

少数接口是 **`Foo(...)`** 形式。绝大多数桥接场景仍是 **固定参数** + **`__real_`**，与本文主流程一致。若遇到 **可变参数** 且库内另有 **`FooV(..., va_list)`**，可在 `__wrap_` 里用 `va_start` / **`FooV`** / `va_end` 转发。若 **只有** `...` **没有** `v` 版，ISO C/C++ **不能** 可移植地把 `...` 转给 `__real_Foo`；应优先 **换拦截点**（更底层的非 `...` 符号），而不是在链接期硬 wrap。编译器扩展（如部分工具链的 `__builtin_va_arg_pack`）或 **libffi** 等仅作权宜，**OHOS BiSheng** 等环境往往不可用，**不要**当作默认方案。

### 文件速查

| 内容 | 路径 |
| --- | --- |
| CMake | `harmonyApp/entry/src/main/cpp/CMakeLists.txt` |
| `__wrap_` + `__real_` | `harmonyApp/entry/src/main/cpp/log_hook.c` |
| 多次调用 + `LOG_HOOK_SAMPLE_LINES` | `harmonyApp/entry/src/main/cpp/log_caller.c` |
| NAPI | `harmonyApp/entry/src/main/cpp/napi_init.cpp` |
| 设备 HiLog 断言 | `scripts/assert-log-hook-device.sh` |
