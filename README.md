# Modular SO on-demand demo

证明：业务 SO（`libk2n` / `libn2k`）可在运行时 `dlopen` 按需加载，且 **不会** 通过 `libstd` 反向依赖把两个业务模块绑死。

## 结构

| 产物 | 角色 |
|------|------|
| `libruntime.so` | KN runtime（启动时由 `libentry` 拉起） |
| `libstd.so` | stdlib（启动时由 `libentry` 拉起） |
| `libk2n.so` | 业务模块 1：K→C（HiLog）+ C→K（`staticCFunction`） |
| `libn2k.so` | 业务模块 2：同上，**不依赖** `:k2n` |
| `libentry.so` | NAPI：`dlopen` 两个业务 SO；**只** `NEEDED` runtime+std |
| `libmainstub.so` | 满足 KN sharedLib 的 UND `main`（业务 SO 的 DT_NEEDED） |

工程模块：

- `:k2n` / `:n2k` — 业务 klib（互不依赖）
- `:kotlinApp` — 链接宿主（挂 4 个 `sharedLib` + `linkAnchor.kt`，不能删）
- `harmonyApp` — HAP + `dlopen` + 简单 UI

## 按需加载行为

1. 启动：只加载 `libentry` → `libruntime` + `libstd`
2. 首页 `aboutToAppear`：`dlopen("libk2n.so")` → `kn_k2n_run`
3. 点击屏幕：`dlopen("libn2k.so")` → `kn_n2k_run`

`moduleIncludes` 保证每个业务 SO 只打进自己的 klib，不会互相 `NEEDED`。

## 前置

- DevEco Studio（本机默认 `/Applications/DevEco-Studio.app`）
- 真机在线；默认钉死 `TARGET_ID=23E0123523000348`
- `local.properties` 指向带 StubGV pin 修复的 KN dist，例如：

```properties
kotlin.native.home=/Users/ohoskt/git/reference/kotlin/kotlin-native/dist
```

（`fix-gv-dep`：stdlib 不再 pin 业务侧 `ExportForCppRuntime` → `libstd` knbridge UND=0，否则启动时 `libstd` 重定位失败。）

## 一键验证

```bash
TARGET_ID=23E0123523000348 bash scripts/run.sh
```

检查项：进程存活、无新 faultlog、`libentry` 不链业务 SO、hilog 成功调用，以及 **`/proc/<pid>/maps`**：点屏前无 `libn2k.so`，点屏后才映射。
