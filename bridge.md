## 桥接 / 拦截 HarmonyOS NDK C 调用（示例：`OH_LOG_Print`）

本示例演示如何用 **wrap（包装）** 方式拦截系统 C API：先执行你的逻辑（例如计数），再 **转发** 到真实实现。NDK 中的符号名是 **`OH_LOG_Print`**（见 `hilog/log.h`），不是 `oh_log_print`。

### 为何「再做一个导出同名符号的应用 `.so`」通常不合适

在另一个动态库里实现 `OH_LOG_Print`，并在链接时把它排在 `libhilog_ndk.z.so` 之前，在本工程里 **不可靠**：**`libc2k.so`（Kotlin/Native 产物）** 的 **`DT_NEEDED`** 里已经包含 **`libhilog_ndk.z.so`**。动态加载器往往会先把 `OH_LOG_Print` 绑定到 **系统** HiLog，你的额外 `.so` 很难在运行时靠「重复符号」抢在前面，因此这种插桩方式多数情况下 **会失败**。

### 推荐做法：在链接 **同一份** `libentry.so` 时使用链接器 wrap

类 GNU 的链接器（OHOS 原生构建所用工具链通常支持）提供 **`-Wl,--wrap=符号名`**。在 **生成包含调用点的那个共享库**（此处为 `libentry.so`）时传入该选项，会把对 `symbol` 的调用改写成对 **`__wrap_symbol`** 的调用。把 `__wrap_symbol` 放在 **静态库** 里并链进 **同一个** `.so`，wrap 作用在 **链接这一侧**；若做成单独的 `.so`，wrap 行为往往对不上调用方。

在 **`harmonyApp/entry/src/main/cpp/CMakeLists.txt`** 中典型配置为：

- 使用 **静态** hook 库：`add_library(loghook STATIC log_hook.c)`。
- 传入 wrap 选项：`target_link_options(entry PRIVATE "-Wl,--wrap=OH_LOG_Print")`。
- 把静态库链进 `entry`：`target_link_libraries(entry PRIVATE loghook)`。
- 仍要链接 **`hilog_ndk.z`**，以便转发到真实 HiLog：`target_link_libraries(entry PUBLIC ... hilog_ndk.z)`。

### 实现要点：计数 + 继续正常打日志

在 C 里无法把可变参数 `...` **可移植地** 直接转发给 `__real_OH_LOG_Print`；需要带 **`va_list`** 的入口。NDK 提供 **`OH_LOG_VPrint`**，适合作为桥梁：先自增计数器，再对 `fmt` 使用 `va_start` / `va_end`，调用 **`OH_LOG_VPrint(..., fmt, ap)`**，与 `OH_LOG_Print` 走同一套 HiLog 管线。

实现见 **`harmonyApp/entry/src/main/cpp/log_hook.c`**（`__wrap_OH_LOG_Print`）与 **`log_caller.c`**（普通调用 `OH_LOG_Print`，经链接器改写后进入 wrap）。

ArkTS 侧可通过 `libentry.so` 上的 **`testOhLogHook`** 做简单校验（NAPI 注册在 **`napi_init.cpp`**）。

### 官方支持的替代方案（不做符号包装）

若只需观察或处理格式化后的日志，可使用 NDK 文档中的 **`OH_LOG_SetCallback`**（回调方式；无需 `LD_PRELOAD` 或 `--wrap`）。在 API 级别与策略允许时，更适合作为偏「生产向」的拦截手段。

### 文件速查

| 内容 | 路径 |
| --- | --- |
| CMake：wrap、静态 `loghook`、`hilog_ndk.z` | `harmonyApp/entry/src/main/cpp/CMakeLists.txt` |
| `__wrap_OH_LOG_Print` 与 `OH_LOG_VPrint` 转发 | `harmonyApp/entry/src/main/cpp/log_hook.c` |
| 调用点 | `harmonyApp/entry/src/main/cpp/log_caller.c` |
| NAPI 测试导出 | `harmonyApp/entry/src/main/cpp/napi_init.cpp` |
