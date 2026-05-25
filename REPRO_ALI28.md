# Reproduction: ALI-28 - splitBCfile Compiler Crash

## Issue
Kotlin Native compiler crashes when `-Xbinary=splitBCfile=2` is set in konan CPF version 2.2.21-0.3.0-04 targeting ohosArm64.

## Root Cause
The compiler attempts to invoke `llvm-split` binary which is missing from the konan dependencies at:
`~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split`

## Environment
- **OS**: macOS (aarch64)
- **Kotlin Version**: 2.2.21-0.3.0-04 (konan CPF)
- **Target**: ohosArm64
- **Gradle**: 8.9

## Quick Start

```bash
git clone -b repro/ALI-28-splitBCfile-crash https://github.com/linhandev/kn_samples.git
cd kn_samples
./gradlew :kotlinApp:linkDebugSharedOhosArm64
```

## Expected Behavior
Build should complete successfully.

## Actual Behavior
Build fails with:
```
e: Compilation failed: Failed to execute llvm-split: Cannot run program "/Users/ohoskt/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split": Exec failed, error: 2 (No such file or directory)
```

## Crash Boundary Testing

Tested different `splitBCfile` values to map the crash boundary:

| Value | Result | Notes |
|-------|--------|-------|
| 0 | ❌ CRASH | llvm-split invoked with `-j=0`, binary missing |
| 1 | ✅ SUCCESS | No llvm-split invocation (appears to disable splitting) |
| 2 | ❌ CRASH | llvm-split invoked with `-j=2`, binary missing |

**Conclusion**: Values 0 and 2 trigger the llvm-split code path, while value 1 appears to disable it or use a different code path that doesn't require the external binary.

## Configuration

The flag is set in `kotlinApp/build.gradle.kts`:
```kotlin
ohosArm64 {
    binaries {
        sharedLib {
            baseName = "c2k"
            freeCompilerArgs += "-Xadd-light-debug=enable"
            freeCompilerArgs += "-Xbinary=stripDebugInfoFromNativeLibs=false"
            freeCompilerArgs += "-Xbinary=splitBCfile=2"  // Change this value to test
        }
    }
}
```

## Stack Trace (Key Lines)
```
e: java.lang.RuntimeException: Failed to execute llvm-split: Cannot run program "/Users/ohoskt/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split": Exec failed, error: 2 (No such file or directory) 
	at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
	at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:613)
	at org.jetbrains.kotlin.backend.konan.driver.phases.TopLevelPhasesKt.compileModule(TopLevelPhases.kt:417)
```

## Reproducibility
**CONSISTENT** - Crashes every time with splitBCfile=0 or splitBCfile=2.
