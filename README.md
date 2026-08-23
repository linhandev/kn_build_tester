# akinterop helloworld (场景3)

## commit1：原样复制官方 demo
复制自 [akinterop](https://gitcode.com/CPF-KMP-CMP/akinterop) `example/helloworld`（develop `d9ec6d2`）。
- `HelloWorldModule.kt`：register 入口 `@ExportedBridge("org_cpf_kotlin_akinterop_register")`
- `ExportKotlin.kt`：`@KNExportFunction greet` + `@KNExportClass Counter`
- `napi_init.cpp`：dlsym register 名，NAPI 模块
- `kotlinApp/build.gradle.kts`：akinterop-gradle-plugin 0.5.0-09 + `-PcInterfaceMode` 切换
- kotlinVersion=2.2.21-0.5.0-16（EB+stackmap N2K trampoline 所需，0.3.0-04 链接报 R_AARCH64_PREL64）
- bundleName com.kotlin.demo 复用 bare debug 签名

## commit2：默认关 CExport（cInterfaceMode=none）
`gradle.properties` 设 `cInterfaceMode=none`，register 已是 `@ExportedBridge`（commit1 原样，V1/none 通用）。

### 改造方式
| 项 | V1（默认） | none（改造后） |
|--|--|--|
| CAdapter 头/`*_symbols()` 表 | 生成 | 不生成 |
| register 入口 | `@ExportedBridge`（EB，stackmap ON 走 N2K trampoline） | 同左 |
| ArkTS NAPI 面 | KSP `@KNExport*` 生成 | 同左（不依赖 V1） |

akinterop 的业务导出（`@KNExport*` → KSP bind → `staticCFunction` → NAPI）本就不依赖 CExport V1；
关 V1 只是去掉 `libkn_symbols()` 粗根集，让「仅被 V1 误导出」的 public 死代码可被 DCE。

### 实测（设备 23E0123523000348，release）
| 口径 | V1 | none | 差 |
|--|--|--|--|
| libkn.so | 7281760 B | 7215384 B | −66376 B (~65 KB) |
| `libkn_symbols` (dynsym T) | 有 | **无（DCE）** | — |
| `org_cpf_kotlin_akinterop_register` (dynsym T) | 有 | 有（EB 保留） | — |

功能验证：HAP 装机启动成功，libkn.so dlopen 成功（maps 有 r-xp），app 进程存活不崩。
（akinterop `@KNExport*` NAPI 链不依赖 V1，OV 2026-08-10 已实测 greet 返回 "Hello, World!"。）
