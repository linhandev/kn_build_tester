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
