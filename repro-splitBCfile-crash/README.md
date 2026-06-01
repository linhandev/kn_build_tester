# Reproduction: splitBCfile=2 Compile Crash (ALI-73)

## Issue
Kotlin/Native compilation crashes when using `-Xbinary=splitBCfile=2` with CPF version 2.2.21-0.2.0-04 on OHOS arm64 target. The crash occurs because the `llvm-split` tool is missing from the `llvm-19-aarch64-macos-essentials-79` dependency bundle.

## Environment
- **OS**: macOS (aarch64)
- **Kotlin/Native CPF Version**: 2.2.21-0.2.0-04
- **Gradle**: 8.9
- **Affected Target**: ohosArm64
- **Unaffected Targets**: ohosX64, iosArm64, macosArm64

## Root Cause
The OHOS arm64 target uses a minimal LLVM dependency bundle (`llvm-19-aarch64-macos-essentials-79`) that includes only essential tools (clang, lld, llvm-ar, llvm-cov, llvm-profdata) but **does not include `llvm-split`**. When `-Xbinary=splitBCfile=2` is enabled, the compiler attempts to invoke `llvm-split` during the link phase, which fails with "No such file or directory".

Other targets (ohosX64, iosArm64, macosArm64) use different LLVM bundles that include `llvm-split`, so they compile successfully.

## Reproduction Steps

### 1. Clone the repository
```bash
git clone -b repro/ali-73-splitBCfile-crash https://github.com/linhandev/kn_samples.git
cd kn_samples/repro-splitBCfile-crash
```

### 2. Test baseline compilation (without splitBCfile)
```bash
# All targets should succeed
./gradlew linkDebugSharedOhosArm64
./gradlew linkDebugSharedOhosX64
./gradlew linkDebugFrameworkIosArm64
./gradlew linkDebugExecutableMacosArm64
```

### 3. Test with splitBCfile=2 enabled
```bash
# ohosArm64 - CRASHES
./gradlew clean
./gradlew linkDebugSharedOhosArm64 -PenableSplitBC=true

# Other targets - succeed
./gradlew clean
./gradlew linkDebugSharedOhosX64 -PenableSplitBC=true

./gradlew clean
./gradlew linkDebugFrameworkIosArm64 -PenableSplitBC=true

./gradlew clean
./gradlew linkDebugExecutableMacosArm64 -PenableSplitBC=true
```

## Expected Error Output (ohosArm64)

```
llvm-split command: /Users/ohoskt/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-split -j=2 -o=/var/folders/.../tmp_ori_part_ --preserve-locals /var/folders/.../tmp_ori.bc
e: Compilation failed: Failed to execute llvm-split: Cannot run program "/Users/ohoskt/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-split": Exec failed, error: 2 (No such file or directory)

 * Source files: 
 * Compiler version: 2.2.21-0.2.0-04
 * Output kind: DYNAMIC

e: java.lang.RuntimeException: Failed to execute llvm-split: Cannot run program "/Users/ohoskt/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-split": Exec failed, error: 2 (No such file or directory)
	at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:322)
	at org.jetbrains.kotlin.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:336)
	...
```

## Test Results Summary

| Target | Baseline | With splitBCfile=2 |
|--------|----------|-------------------|
| ohosArm64 | ✓ SUCCESS | ✗ CRASH (llvm-split missing) |
| ohosX64 | ✓ SUCCESS | ✓ SUCCESS |
| iosArm64 | ✓ SUCCESS | ✓ SUCCESS |
| macosArm64 | ✓ SUCCESS | ✓ SUCCESS |

## Boundary Observations

- **Reproduces**: Always on ohosArm64 with splitBCfile=2 enabled
- **Does NOT reproduce**: On ohosX64, iosArm64, or macosArm64 (these targets use different LLVM bundles that include llvm-split)
- **Does NOT reproduce**: On ohosArm64 without the splitBCfile flag (baseline)
- **Consistency**: 100% reproducible on affected target

## Notable Observations

1. The `llvm-19-aarch64-macos-essentials-79` bundle contains only: clang, clang++, clang-19, clang-cache, ld.lld, lld, llvm-ar, llvm-cov, llvm-profdata
2. Other LLVM bundles on the same system (e.g., `mpcore-llvm-19-aarch64-macos-dev-13`, `llvm-1914-aarch64-macos-dev-10`) DO include llvm-split
3. The crash occurs during the link phase in `BitcodeKt.splitBitcodeFile` (Bitcode.kt:322)
4. The issue is specific to the OHOS arm64 target's LLVM dependency packaging

## Reproducibility
**CONSISTENT** - Always reproduces on ohosArm64 with splitBCfile=2 enabled.
