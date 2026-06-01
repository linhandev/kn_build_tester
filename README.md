# ALI-74: splitBCfile=2 Compile Crash Reproduction

## Issue
Compiling a Kotlin/Native project with `-Xbinary=splitBCfile=2` using Konan CPF version `2.2.21-0.2.0-04` causes a build failure on **ohos_arm64** target because the host LLVM (`llvm-19-aarch64-macos-essentials-79`) does not include the `llvm-split` binary. The same failure occurs with `splitBCfile=0`. Only `splitBCfile=1` succeeds.

## Environment
- **Compiler**: kotlinc-native 2.2.21-0.2.0-04 (prebuilt macOS aarch64)
- **Host**: macOS 26.4.1 (arm64)
- **Host LLVM**: llvm-19-aarch64-macos-essentials-79 (missing llvm-split)
- **OHOS Toolchain LLVM**: llvm-19.1.7-aarch64-macos-ohos-2 (also missing llvm-split)
- **JRE**: Temurin 21.0.10+7

## Quick Start

### Gradle build:
```bash
# Test with splitBCfile=2 for ohosArm64 (should FAIL)
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=2 --no-daemon

# Test with splitBCfile=1 for ohosArm64 (should SUCCEED)
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=1 --no-daemon

# Test with splitBCfile=0 for ohosArm64 (should FAIL)
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=0 --no-daemon

# Test other targets with splitBCfile=2 (should all SUCCEED)
./gradlew :kotlinApp:linkDebugSharedOhosX64 -PsplitBCLevel=2 --no-daemon
./gradlew :kotlinApp:linkDebugSharedIosArm64 -PsplitBCLevel=2 --no-daemon
./gradlew :kotlinApp:linkDebugSharedMacosArm64 -PsplitBCLevel=2 --no-daemon
```

## Results Summary

| Target | splitBC=0 | splitBC=1 | splitBC=2 |
|--------|-----------|-----------|-----------|
| macosArm64 | ✅ OK | ✅ OK | ✅ OK |
| iosArm64 | ✅ OK | ✅ OK | ✅ OK |
| ohosArm64 | ❌ llvm-split missing | ✅ OK | ❌ llvm-split missing |
| ohosX64 | ✅ OK | ✅ OK | ✅ OK |

## Error Details

### splitBC=0 and splitBC=2 (ohosArm64)
```
llvm-split command: /Users/ohoskt/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-split -j=2 -o=... --preserve-locals ...
e: Compilation failed: Failed to execute llvm-split: Cannot run program "/Users/ohoskt/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-split": Exec failed, error: 2 (No such file or directory)
e: java.lang.RuntimeException: Failed to execute llvm-split: ...
    at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:322)
```

The `llvm-19-aarch64-macos-essentials-79` package contents:
```
clang, clang++, clang-19, clang-cache, ld.lld, lld, llvm-ar, llvm-cov, llvm-profdata
```
No `llvm-split` binary is present.

## Key Observations
- The crash is **specific to ohosArm64** — other targets (ohosX64, iosArm64, macosArm64) succeed with all splitBC levels
- `splitBC=1` is the **only working level** for ohosArm64 (llvm-split is not invoked)
- `splitBC=0` and `splitBC=2` both invoke `llvm-split` from the host LLVM, which is missing the binary
- The host LLVM is configured as `llvm.macos_arm64.user = llvm-19-aarch64-macos-essentials-79` (the "essentials" = reduced distribution)
- The OHOS toolchain LLVM (`llvm-19.1.7-aarch64-macos-ohos-2`) also lacks `llvm-split`
- The `-j=0` flag is used for splitBC=0, `-j=2` for splitBC=2 (number of split parts)

## Reproducibility
CONSISTENT — fails every time for ohosArm64 with splitBC=0 or splitBC=2.
