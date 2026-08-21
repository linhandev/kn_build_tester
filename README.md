# CExport+@CName vs @ExportedBridge

Minimal HAP on `bare`. One `libc2k.so`（默认 CExport V1）。ArkTS 导出场景通常是无参 register/init，本 demo 不测 String 编解码。

设备 `23E0123523000348`。入口：`kn_*_ping` / `kn_*_alloc` / `kn_*_throw`。

## 谁做 initRuntime / 线程态

| | `@CName`（CAdapter 壳） | `@ExportedBridge` stackmap **ON** | `@ExportedBridge` stackmap **OFF** |
|--|--|--|--|
| initRuntime / 线程态 | C 壳 `Kotlin_initRuntimeIfNeeded` + `ScopedRunnableState` | N2K trampoline：`kn_eb_*` → `kn_eb_*_kbridge`，stub 里 `SaveLastFrameAndStatus` | **无 trampoline**；Kotlin 函数自己 `needsRuntimeInit` + `switchToRunnable` |
| 本 demo 证据 | `alloc` 能分配；`pthread` 调 `ping` | dynsym 有 `kn_eb_*_kbridge` + `KotlinStubGV` | dynsym **没有** `_kbridge` / `KotlinStubGV` |

## 未捕获 Kotlin 异常出不出 N/K 边界

C 侧 `catch (...)` 住 = 出了边界。CName 杀进程也是结果。

EB 的 catch 放在 **worker pthread** 上（NAPI/JS 线程上 catch 会把 x28/线程态弄脏，回 ArkTS 直接 SIGSEGV）。

| | CName `kn_cname_throw` | EB `kn_eb_throw` |
|--|--|--|
| 机制 | 壳内 `catch` → `HandleCurrentExceptionWhenLeavingKotlinCode` → `std::terminate` | 异常 unwind 进 C |
| **ON** 1.1.0-01 | hilog `PASS ping alloc pthread ebCrossed` 后 `SIGABRT`，`Reason: kotlin.RuntimeException: cname`，栈 `kn_cname_throw` → `std::terminate` | worker 上 `catch (...)` 成功（否则到不了 PASS） |
| **OFF** 本地 dist | 同上：PASS 然后 `kn_cname_throw` → `terminate` | 同上：C catch 成功 |

OFF dist：`~/git/worktree/kotlin-cname-eb-stackmap-off`（`cpf/develop-2.2.21-OH` `e928f6a5df74`），`-Pkotlin.native.precise.stackmap=false :kotlin-native:bundle`。应用侧必须同关：`-Pkotlin.native.home=<dist> -PenableStackmap=false`。

## 跑法

精确栈 **ON**（发布版 dist，默认）：

```shell
export GRADLE_USER_HOME=~/git/worktree/kn_samples-cname-vs-eb-gradle_home
./gradlew startHarmonyAppDebug
```

精确栈 **OFF**（本地 matching dist）：

```shell
export GRADLE_USER_HOME=~/git/worktree/kn_samples-cname-vs-eb-gradle_home
./gradlew startHarmonyAppDebug \
  -Pkotlin.native.home=/path/to/kotlin-native/dist \
  -PenableStackmap=false
```

hilog tag `cname-eb` 先出 `PASS ping alloc pthread ebCrossed`，约 2.5s 后调 `kn_cname_throw`，进程 `SIGABRT`。
