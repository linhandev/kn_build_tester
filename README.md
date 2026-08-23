# akinterop helloworld (场景3)

复制自 [akinterop](https://gitcode.com/CPF-KMP-CMP/akinterop) 官方 `example/helloworld` demo
（reference clone `~/git/reference/akinterop` @ develop `d9ec6d2`）。

## 原样复制内容
- `kotlinApp/src/ohosArm64Main/kotlin/com/example/helloworld/`：HelloWorldModule.kt + ExportKotlin.kt
- `kotlinApp/build.gradle.kts`：akinterop-gradle-plugin 0.5.0-09 + 单 SO `libkn.so`
  + `-PcInterfaceMode` / `-PenableStackmap` 开关（akinterop 原版支持 V1/none 切换）
- `harmonyApp/entry/src/main/cpp/napi_init.cpp`：dlsym `org_cpf_kotlin_akinterop_register` NAPI 注册
- `harmonyApp/entry/src/main/ets/pages/Index.ets`：`import demo from 'libentry.so'` 调 `greet`/`Counter`

bundleName 用 bare 骨架的 `com.kotlin.demo`（复用本地 debug 签名证书，不随 demo commit）。

## 改造 commit
见下一笔 commit：默认 `cInterfaceMode=none` 关 CExport + V1/none 体积对比。
