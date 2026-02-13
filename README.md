# Kotlin/Native + C interop on OHOS

Demo: a **cinterop klib** (with a C static lib), a **Kotlin/Native** Gradle project that produces a shared library, and a **C driver** that calls it. OHOS (OpenHarmony) only.

## Project structure

```
kn_samples/
├── add/                         # 带静态库 cinterop 的 Kotlin 库
│   ├── build.gradle.kts         # ohosArm64、cinterop add 静态库，发布到 maven-repo
│   └── src/
│       ├── nativeInterop/add/
│       │   ├── add.h
│       │   ├── add.c            # 功能实现
│       │   ├── add.def          # cinterop 定义
│       │   └── libadd.a         # 构建脚本中用 clang 从 add.c 打出的静态库
│       └── nativeMain/kotlin/
│           └── PlaceHolder.kt   # 占位，否则构建配置写起来很麻烦
│
├── src/nativeMain/kotlin/       # 出 so 的 KN 工程，调用 add 里的 cinterop 库，@CName 导出接口给 c 调用
│   └── Add.kt                   # @CName("add_c_name")，内部调用 add 的 addcfun
│
├── c-caller/                    # C 驱动，调上面 KN 的 so
│   └── main.c                   # 链接 libc2k.so，调用 add_c_name()
│
├── maven-repo/                  # 仓库内 Maven（add 的 klib 发布到这方便查看）
├── build.gradle.kts             # 根工程：ohosArm64，依赖 add，产出 libc2k.so
├── settings.gradle.kts          # include("add")，maven-repo 放在仓库首位
└── run.sh                       # 构建静态库 → 发布 add klib → 构建 c2k → 构建 C 驱动 → 部署
```

## Key pieces

| Part | Role |
|------|-----|
| **add** | Kotlin Multiplatform library (OHOS only). Defines a **cinterop** that wraps the C static lib `libadd.a` (built from `add.c`). Publishes the resulting klib to `maven-repo/` so the root can depend on it. |
| **Root (c2k)** | Kotlin/Native project that `implementation("com.example:add-ohosarm64:1.0-SNAPSHOT")`, uses `add.addcfun`, and produces **libc2k.so** with an exported `add_c_name` for C. |
| **c-caller** | C executable that links to **libc2k.so**, includes the generated `libc2k_api.h`, and calls `add_c_name()`. |
| **run.sh** | Uses DevEco LLVM/sysroot to: build `libadd.a` → publish add to `maven-repo` → build `libc2k.so` → build the C driver → push to device and run. |

## Requirements

- DevEco Studio SDK (native sysroot + LLVM at `.../openharmony/native/`)
- Kotlin/Native (Gradle uses the Kotlin plugin and Konan)
- Connected OHOS device and `hdc` for deploy/run

## Run

```bash
./run.sh
```

This builds the static lib, publishes the add klib, builds the root shared library and the C driver, then deploys and runs on the device (e.g. prints `10 + 3 = 13` via `add_c_name(10, 3)`).
