# ALI-72: splitBCfile Crash Reproduction

## Issue
Compiling a Kotlin/Native project with `-Xbinary=splitBCfile=2` using konan CPF version `2.2.21-0.3.0-04` causes a SIGSEGV crash on **ohos_arm64** target. The crash also occurs with `splitBCfile=0`.

## Environment
- **Compiler**: kotlinc-native 2.2.21-0.3.0-04 (prebuilt macOS aarch64)
- **Host**: macOS 26.4.1 (arm64)
- **LLVM**: llvm-1914-aarch64-macos-dev-10
- **JRE**: Temurin 21.0.10+7

## Quick Start

### Direct kotlinc-native (simplest):
```bash
export KONAN_HOME=~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04
./test_splitbc.sh
```

### Gradle build:
```bash
# Test with splitBCfile=2 for ohosArm64
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=2 --no-daemon

# Test with splitBCfile=1 (should succeed)
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=1 --no-daemon

# Test with splitBCfile=0 (also crashes)
./gradlew :kotlinApp:linkDebugSharedOhosArm64 -PsplitBCLevel=0 --no-daemon
```

## Results Summary

| Target | splitBC=0 | splitBC=1 | splitBC=2 |
|--------|-----------|-----------|-----------|
| macos_arm64 | ✅ OK | ✅ OK | ✅ OK |
| ios_arm64 | ✅ OK | ✅ OK | ✅ OK |
| ohos_arm64 | ❌ llvm-split SIGSEGV (exit 139) | ✅ OK | ❌ JVM SIGSEGV in KotlinStubGenerator |
| ohos_x64 | ✅ OK | ✅ OK | ✅ OK |

## Crash Details

### splitBC=0 (ohos_arm64)
```
llvm-split command: .../llvm-split -j=0 -o=... --preserve-locals ...
llvm-split failed with exit code 139
```
Stack trace: `BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:596)`

### splitBC=2 (ohos_arm64)
```
# A fatal error has been detected by the Java Runtime Environment:
#  SIGSEGV (0xb) at pc=0x000000012ef6733c
# Problematic frame:
# C  [libllvmstubs.dylib+0xa7f33c]  llvm::KotlinStubGenerator::run(llvm::Module&, llvm::AnalysisManager<llvm::Module>&)+0x68
```
The crash occurs during the LLVM optimization pipeline in `KotlinStubGenerator::run`, called via `LlvmOptimizationPipeline.execute` → `LLVMRunPasses`.

## Key Observations
- The crash is **specific to ohos_arm64 target** — ohos_x64, ios_arm64, and macos_arm64 all succeed
- splitBC=1 is the **only working level** for ohos_arm64 (llvm-split is not invoked)
- splitBC=0 crashes in the `llvm-split` binary itself (exit 139/SIGSEGV)
- splitBC=2 crashes in `KotlinStubGenerator::run` inside `libllvmstubs.dylib` (SIGSEGV, kills JVM)
- Both crashes reproduce consistently (tested 2+ times each)
- The `'+fp' is not a recognized feature for this target` warning appears before the splitBC=2 crash
