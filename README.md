# Reproduction: splitBCfile=2 Crash on OHOS arm64 (ALI-36)

## Issue

Compiling a Kotlin/Native project targeting ohosArm64 with `-Xbinary=splitBCfile=2` crashes because the `llvm-split` binary is missing from the LLVM toolchain bundle (`llvm-1914-aarch64-macos-dev-10`) shipped with konan CPF 2.2.21-0.3.0-04.

## Environment

- **OS**: macOS aarch64
- **Java**: OpenJDK 21
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **LLVM toolchain**: llvm-1914-aarch64-macos-dev-10
- **Target**: ohosArm64

## Reproduction Steps

```shell
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon
```

## Expected Error

```
llvm-split command: ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split -j=2 -o=... --preserve-locals ...
e: Compilation failed: Failed to execute llvm-split: Cannot run program
  "~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)

  at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
  at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:613)
```

## Control Test

To verify that `splitBCfile=1` works (uses a different code path that does not invoke `llvm-split`), edit `kotlinApp/build.gradle.kts` and change `-Xbinary=splitBCfile=2` to `-Xbinary=splitBCfile=1`, then re-run the build command above. The build should succeed.

## What's Changed from `bare`

- Added `freeCompilerArgs += "-Xbinary=splitBCfile=2"` to the `sharedLib` block in `kotlinApp/build.gradle.kts`
