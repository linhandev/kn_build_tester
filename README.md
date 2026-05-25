# Reproduction: splitBCfile=2 Compilation Crash (ALI-24)

## Issue

Compiling a Kotlin/Native project targeting `ohosArm64` with `-Xbinary=splitBCfile=2` using konan CPF version `2.2.21-0.3.0-04` crashes because the `llvm-split` binary is missing from the LLVM toolchain bundle.

## Environment

- **OS**: macOS (aarch64)
- **Java**: OpenJDK 21.0.10 (Temurin)
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **LLVM toolchain**: llvm-1914-aarch64-macos-dev-10 (shipped with konan)
- **Target**: ohosArm64

## Quick Start

```bash
# Clone this branch
git clone -b repro/ALI-24-splitBCfile-crash https://github.com/linhandev/kn_samples.git
cd kn_samples

# Reproduce the crash (splitBCfile=2 is already configured in kotlinApp/build.gradle.kts)
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon
```

## Expected Error

```
e: Compilation failed: Failed to execute llvm-split: Cannot run program
  "<konan>/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)

  at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
```

## What Was Changed

Only one line was added to `kotlinApp/build.gradle.kts` (line 18):

```kotlin
freeCompilerArgs += "-Xbinary=splitBCfile=2"
```

## Boundary Tests

| Configuration | Result |
|---|---|
| No splitBCfile flag (default) | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=1` | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=2` | **BUILD FAILED** (crash) |

## Root Cause

The `splitBCfile=2` mode invokes the external `llvm-split` binary to split bitcode files for parallel processing. However, `llvm-split` is **not shipped** in the `llvm-1914-aarch64-macos-dev-10` toolchain bundle that konan downloads. The `splitBCfile=1` mode uses a different code path that does not require this external tool.

To verify:
```bash
ls ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/ | grep split
# (no output - llvm-split is missing)
```
