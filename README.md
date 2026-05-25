# Reproduction: ALI-32 - splitBCfile=2 Compiler Crash

## Issue

Kotlin/Native compiler crashes when `-Xbinary=splitBCfile=2` is set in konan CPF version 2.2.21-0.3.0-04 targeting ohosArm64. The compiler attempts to invoke `llvm-split` which is missing from the konan LLVM dependencies.

## Environment

- **OS**: macOS (aarch64)
- **Kotlin Version**: 2.2.21-0.3.0-04 (konan CPF)
- **Target**: ohosArm64
- **Gradle**: 8.9
- **Java**: OpenJDK 21.0.10

## Quick Start

```bash
git clone -b repro/ALI-32-splitBCfile-crash https://github.com/linhandev/kn_samples.git
cd kn_samples
./gradlew :kotlinApp:linkDebugSharedOhosArm64
```

## Expected Behavior

Build should complete successfully, producing a shared library for ohosArm64.

## Actual Behavior

Build fails with:

```
e: Compilation failed: Failed to execute llvm-split: Cannot run program
"/Users/<user>/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
Exec failed, error: 2 (No such file or directory)
```

## Key Stack Trace

```
e: java.lang.RuntimeException: Failed to execute llvm-split: Cannot run program
  ".../.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)
    at o.j.k.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
    at o.j.k.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:613)
    at o.j.k.backend.konan.driver.phases.TopLevelPhasesKt.compileModule(TopLevelPhases.kt:417)
```

## Configuration

The flag is set in `kotlinApp/build.gradle.kts`:

```kotlin
ohosArm64 {
    binaries {
        sharedLib {
            baseName = "splitbc_repro"
            freeCompilerArgs += "-Xbinary=splitBCfile=2"  // Triggers the crash
        }
    }
}
```

## Verification: Build Without the Flag

To verify the build succeeds without `splitBCfile=2`, comment out the flag in `kotlinApp/build.gradle.kts`:

```kotlin
// freeCompilerArgs += "-Xbinary=splitBCfile=2"
```

Then run:

```bash
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64
```

Result: **BUILD SUCCESSFUL**

## Root Cause

The `llvm-split` binary is not included in the konan LLVM dependency bundle at:
`~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/`

When `splitBCfile=2` is set, the compiler's bitcode post-processing phase (`Bitcode.kt:599`) attempts to execute `llvm-split -j=2` to split the bitcode file for parallel optimization, but the binary does not exist.

## Reproducibility

**CONSISTENT** - Crashes every time with `splitBCfile=2`. Succeeds every time without the flag.
