# knoi (场景3)

原样复制自 [KuiklyBase-components/knoi](https://github.com/Tencent-TDS/KuiklyBase-components) `knoi/`（depth-1 clone, HEAD）。

## commit1：原样复制 + 编译适配
- `knoi-processor/`：`AutoRegisterProcessor` 原版 `@CName(externName="com_tencent_tmm_knoi_initBridge"/"com_tencent_tmm_knoi_initEnv")`
- `example/sample/`：OHOS 产 `libkn.so`（V1 默认）
- `ohosApp/`：`knoi.cpp` dlsym 两个 @CName 名
- 编译器：腾讯 Kotlin fork `2.0.21-KBA-003`
- 编译适配：gradle wrapper 8.9（buildSrc kotlin-dsl 需 jvmTarget 21，gradle 8.0 不支持）；sample 加 `project(:knoi)` 依赖（用源码 runtime HEAD，替代 maven 0.0.4 旧 API）

bundleName `com.tencent.tmm.ohoslib`（原版）；编译验证时临时改 `com.kotlin.demo` + bare debug 签名（不随 demo commit）。

## commit2
见下一笔：KSP `@CName→@ExportedBridge` + `-Xbinary=cInterfaceMode=none`。

## commit2：改造（@CName→@ExportedBridge + cInterfaceMode=none）

### 改动
- `knoi-processor/.../AutoRegisterProcessor.kt`：`initialize` + `initEnvExport` 两处 `@CName(externName=)` → `@ExportedBridge("...")`
- `example/sample/build.gradle.kts`：`binaries.all` 加 `-Xbinary=cInterfaceMode=none` + `project(:knoi)` 源码依赖

### 改造方式
| 项 | V1（原版） | none（改造后） |
|--|--|--|
| CAdapter 头/`libkn_symbols()` | 生成 | 不生成 |
| 入口 initEnv/initBridge | `@CName` CAdapter 壳 | `@ExportedBridge` EB（V1/none 通用，knoi.cpp 仍 dlsym 同名） |
| 业务 NAPI（`@KNExport`+KSP bind+`staticCFunction`） | 不依赖 V1 | 同左 |

### 实测（OV 2026-08-21 同 demo 同改造同设备 23E0123523000348）
| 口径 | V1 | none | 差 |
|--|--|--|--|
| libkn.so | 8416728 B | 7917152 B | −499576 B (~488 KB) |
| `libkn_symbols` (dynsym T) | 有 | **无（DCE）** | — |
| 功能 `invoke(testStringReturnString,input)`→`inputforKMM` | PASS | PASS | — |
