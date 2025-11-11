## 编译过程

1. 替换本地 Kotlin 仓库路径

全项目搜索 `/Volumes/disk/git/kmp/parallel-20/`，替换为本地kotlin仓库文件夹路径。共3处，在 setting.gradle.kts 和 gradle.properties 中

2. 配置 LLVM Split 工具路径

在 `build.gradle.kts` 文件中，设置 `llvmSplitPath` 为本地 patch 过的 llvm-split路径

3. kotlin代码打so

kotlin项目根目录执行

```
./gradlew publishReleaseBinariesToHarmonyApp
```

完成后用 DevEcoStudio 打开 harmonyApp 目录，sync，（如果真机执行添加签名，模拟器不需要），打包，发送真机或模拟器运行

## 运行现象与参数说明

`build.gradle.kts` 配置 
- splitBCfile 为 2 启用并行化，链接选项添加 -z now后，点击 Welcome 应用崩溃
- splitBCfile 为 1 或 不加链接选项 -z now，就只要没有同时开启并行化和-z now，点击首页 Welcome，显示正常切换成 Hello World，hilog正常打印`implementedFunction returns 1`。
