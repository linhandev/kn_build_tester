# CExport+@CName vs @ExportedBridge

Minimal HAP on `bare`. Kotlin **2.2.21-1.1.0-01**. One `libc2k.so` (default CExport V1).

CAdapter (`@CName`) C 壳会做：`Kotlin_initRuntimeIfNeeded`、`ScopedRunnableState`、`char*`↔`String`、Kotlin 异常 `terminate`。

`@ExportedBridge` + stackmap：`Kotlin_N2KStub` → `SaveLastFrameAndStatus` 已经做了 **initRuntime + 切 Runnable**。Kotlin 函数体不再做这两件事。要补的是 **C ABI 编解码**（本 demo 的 `ebGreet`：`toKString`/`strdup`）。

| 点 | CName V1 谁做 | EB 谁做 | 本 demo |
|--|--|--|--|
| initRuntime | C 壳 | N2K stub | `alloc`：两边都能分配 String |
| 线程态 | C 壳 ScopedRunnableState | N2K stub | `pthread` 调 `add` |
| 栈图/N2K | C `_impl` ktstub | trampoline ktstub | 跟着上面两条 |
| 原语 | 透传 | 透传 | `add` |
| String↔char* | CAdapter | **EB 函数里补** | `greet` |
| Kotlin 里 catch | 两边一样 | 两边一样 | `caught`→7 |
| 未捕获异常出 C | C 壳 terminate | N2K unwind 到 C | 不在 HAP 里测（会杀进程） |

```shell
./gradlew startHarmonyAppDebug
```

页面 / hilog tag `cname-eb` 应为 `PASS add alloc greet caught pthread`。
