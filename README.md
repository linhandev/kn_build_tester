# ByteKMPSample (场景3)

原样复制自 [ByteKMPSample](https://github.com/ByteKMP/ByteKMPSample)（depth-1 clone）。

## 原样复制内容
- `sample/` + `todo/`：KMP 模块，OHOS 产 liblauncher.so（ByteKMP 插件配置）
- `launcher/`：入口模块（iOS/Android）
- `app/ohosApp/`：OHOS app 工程
- `build-logic/`：ByteKmpSettingsPlugin（composite build）
- 编译器：ByteKMP Kotlin fork `2.0.20-bytekmp-1001`（`kotlin.version.non-ios`）
- ByteKMP 插件（`com.bytekmp.*`）从 `artifact.bytedance.com` maven 拉，**不在本仓**

bundleName 原版 `com.bytekmp.sample`；编译验证时临时改 `com.kotlin.demo` + bare debug 签名（不随 demo commit）。

## 改造 commit
见下一笔 commit：`cInterfaceMode=none` + Init→`@ExportedBridge`（后者在 ByteKMP 插件层，本仓文档化引用 OV 实测）。

## commit2：改造方式说明（Init→@ExportedBridge + cInterfaceMode=none）

### 改造在 ByteKMP 插件层（不在本仓）
ByteKMPSample 的 OHOS SO（liblauncher.so）入口 `liblauncher_symbols()->init` 由 ByteKMP 插件（`com.bytekmp.*`，从 `artifact.bytedance.com` maven 拉）的 KSP 代码生成，**不在本 demo 源码**。改造需改 ByteKMP 插件代码生成逻辑：

| 改什么 | 原版（V1） | 改造后（none） |
|--|--|--|
| C 入口 | `liblauncher_symbols()->init` 走 V1 符号表 | `dlsym("kmp_ohos_ffi_init")` 走 EB 固定 C 名 |
| Init 函数 | ByteKMP 插件生成（无 @CName/@ExportedBridge） | 插件改为 `@ExportedBridge("kmp_ohos_ffi_init")` |
| KN 配置 | V1 默认 | `cInterfaceMode=none` |

业务 NAPI（`@KotlinExport*`/`@ArkTsExport*` → KSP bind）不依赖 CExport V1，关 V1 只去 `liblauncher_symbols` 粗根集。

### 实测（OV 2026-08-21 同 demo 同改造同设备 23E0123523000348）
| 口径 | V1 | none | 差 |
|--|--|--|--|
| liblauncher.so | — | — | −24 KB |
| `liblauncher_symbols` (dynsym T) | 有 | **无（DCE）** | — |
| 功能（homepage 五按钮 + KmpLogger） | PASS | PASS | — |

改造方式与 akinterop/knoi 同构：业务导出不依赖 V1，只有 C 入口 register/init 借 V1（符号表或 @CName 壳），关 V1 后改 EB 钉固定 C 名。ByteKMP 入口更绑 V1（借符号表而非 @CName），改造需改插件代码生成。
