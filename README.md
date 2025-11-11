## 一、编译过程

### 1. 替换本地 Kotlin 仓库路径

在 `settings.gradle.kts` 中修改 Maven 仓库地址为本地 Kotlin 项目构建产物路径：

```kotlin
maven("/Volumes/disk/git/kmp/parallel-20/build/repo")
```

同时在 `gradle.properties` 文件中设置 Kotlin/Native 的本地目录：

```kotlin
kotlin.native.home=/Volumes/disk/git/kmp/parallel-20/kotlin-native/dist
```

该路径应指向你本地构建完成的 Kotlin/Native目录。

### 2. 配置 LLVM Split 工具路径

在 `build.gradle.kts` 文件中，设置 `llvmSplitPath` 为你本地已 patch 过的 LLVM 分割工具路径。例如：

```kotlin
binaryOption("llvmSplitPath", "/Volumes/disk/git/llvm/third_party_llvm-project/build-used-02/bin/llvm-split")
```

此路径对应 LLVM 源码编译后的 `llvm-split` 可执行文件，用于控制 Bitcode 文件拆分逻辑。

### 3. 构建并发布至 Harmony 平台

在项目根目录的终端中执行以下命令：

```
./gradlew publishReleaseBinariesToHarmonyApp
```

随后打开 **DevEco-Studio**，在 IDE 中运行应用，将 App 发布至 HarmonyOS 手机进行测试。



## 二、运行现象与参数说明

在 `build.gradle.kts` 文件中可通过如下配置控制 Bitcode 拆分策略：

```
binaryOption("splitBCfile", "2")
```

| 参数值            | 启动表现                       | 说明                                       |
| ----------------- | ------------------------------ | ------------------------------------------ |
| `splitBCfile = 1` | 点击首页文字，正常切换至下一屏 | 应用运行稳定，无异常                       |
| `splitBCfile = 2` | 点击首页文字后触发崩溃         | 疑似 LLVM 拆分文件在链接或符号合并阶段异常 |

