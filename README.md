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
