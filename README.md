# Reproduction: splitBCfile=2 Compile Crash — Multi-Target (ALI-34)

## Issue

Compiling a Kotlin/Native project with `-Xbinary=splitBCfile=2` using CPF `2.2.21-0.3.0-04` crashes on `ohosArm64` but succeeds on all other targets (iosArm64, linuxX64, macosX64).

## Environment

- **OS**: macOS 26.4.1 (aarch64)
- **Java**: OpenJDK 21.0.10 (Temurin)
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **LLVM toolchain**: llvm-1914-aarch64-macos-dev-10

## Quick Start

```bash
git clone -b repro/ALI-34-splitBCfile2-multitarget https://github.com/linhandev/kn_samples.git
cd kn_samples

# Crashes (ohosArm64 only):
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon

# Succeeds (all other targets):
./gradlew clean :kotlinApp:linkDebugSharedIosArm64 --no-daemon
./gradlew clean :kotlinApp:linkDebugExecutableLinuxX64 --no-daemon
./gradlew clean :kotlinApp:linkDebugExecutableMacosX64 --no-daemon
```

## Multi-Target Results

| Target | Task | Result |
|---|---|---|
| ohosArm64 | `linkDebugSharedOhosArm64` | **BUILD FAILED** — `llvm-split` missing |
| iosArm64 | `linkDebugSharedIosArm64` | BUILD SUCCESSFUL |
| linuxX64 | `linkDebugExecutableLinuxX64` | BUILD SUCCESSFUL |
| macosX64 | `linkDebugExecutableMacosX64` | BUILD SUCCESSFUL |

## Expected Error (ohosArm64)

```
llvm-split command: ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split -j=2 -o=.../tmp_ori_part_ --preserve-locals .../tmp_ori.bc
e: Compilation failed: Failed to execute llvm-split: Cannot run program ".../llvm-split": Exec failed, error: 2 (No such file or directory)

e: java.lang.RuntimeException: Failed to execute llvm-split: Cannot run program ".../llvm-split": Exec failed, error: 2 (No such file or directory)
    at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
    at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:613)
    at org.jetbrains.kotlin.backend.konan.driver.phases.TopLevelPhasesKt.compileModule(TopLevelPhases.kt:417)
```

## Why Only OHOS_ARM64?

The `KonanConfig.splitBCfile` getter in the compiler source hardcodes `1u` for non-OHOS targets:

```kotlin
val splitBCfile: UInt
    get() = if (target == KonanTarget.OHOS_ARM64)
        configuration.get(BinaryOptions.splitBCfile) ?: 2u
    else 1u
```

Even when `-Xbinary=splitBCfile=2` is explicitly passed, the compiler ignores it for iosArm64/linuxX64/macosX64 and forces the serial (non-splitting) code path.

## Root Cause

1. `llvm-split` binary is **not shipped** in the `llvm-1914-aarch64-macos-dev-10` toolchain
2. `splitBCfile=2` triggers `runBitcodePostProcessingCoroutines()` which calls `splitBitcodeFile()` → invokes `llvm-split` via `ProcessBuilder`
3. The missing binary causes `IOException` → `RuntimeException` → compilation failure
4. The parallel code path is only reachable for `OHOS_ARM64` due to the hardcoded target check

## Reproducibility

CONSISTENT — reproduced on 2 consecutive ohosArm64 runs with identical errors.
