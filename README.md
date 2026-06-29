# kn_sample

用 cpf 0.4（Kotlin 2.2.21-0.4.0-03）把 HarmonyOS 独有的 159 个 cinterop def 封装成一个 Maven klib，
给基于 2.3.20-HUAWEI 的消费者项目用，并通过 capi-demo 的 UI 自动化测试端到端验证。

## 仓库结构

| 目录 | 角色 | Kotlin | 说明 |
|---|---|---|---|
| `producer/` | 生产者 | `2.2.21-0.4.0-03` (cpf 0.4) | `hilog-klib` 模块：遍历 159 个 ohos-only def 跑 cinterop，发布 `com.example:hilog-klib:1.0-SNAPSHOT` 到仓库内 `m2/`（gitignore） |
| `consumer-bare/` | 简单消费者 | `2.3.20-HUAWEI` | 最小 demo：依赖 maven klib，调用 HiLog + Asset，验证编译/链接/运行 |
| `consumer-capi-demo/` | 复杂消费者 | `2.3.20-HUAWEI` | capi-demo 项目：9 模块 CAPI smoke test + `autotest.py` UI 自动化测试 |

## 工作流

```
producer (cpf 0.4)  ──publish──>  m2/com/example/hilog-klib  (仓库内，gitignore)
                                                    │
                              consumer-bare (HUAWEI) ┴ consumer-capi-demo (HUAWEI)
                                    implementation("com.example:hilog-klib:1.0-SNAPSHOT")
```

## 前置依赖（本机环境，非本仓库）

- **DevEco Studio** `/Applications/DevEco-Studio.app` — 提供 ohos 主 sysroot（基础 Kit .so stub）+ HAP 工具链（ohpm/hvigor/hdc）
- **cpf 0.4 KN dist** `~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03` — 生产者 `kotlin.native.home`
- **HMS sysroot** `~/.konan/dependencies/sysroot-hms-aarch64-6.0.2.640-02` — 扩展 Kit（AppGalleryKit/CANNKit/DeviceSecurityKit/...）的头 + .so stub，DevEco 不自带
- **devcloud Maven 凭据** — 消费者拉 2.3.20-HUAWEI 的 KGP，放 `local.properties`（git-ignored）

详见 `producer/SUMMARY.html`。

## .so 链接策略（c 方案）

klib 只声明 `linkerOpts=-lxxx`，不嵌 .so。消费者 link 时用自己 ohos sysroot（DevEco）的 .so stub，加 `-L<HMS sysroot>` 补扩展 Kit stub，`--as-needed` 让 NEEDED 只含实际 UND 的库，`-soname` 让 NEEDED 记裸名供设备 dlopen。运行时 .so 不进 HAP，按 soname 走设备 ROM 系统库。

唯一硬编码 gap：HMS sysroot 路径（生产者 `-I`、消费者 `-L` 共 3 处）。详见 SUMMARY.html「.so 链接策略」节。
