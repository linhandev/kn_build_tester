# Reproduction: splitBCfile=2 Compile Crash (ALI-30)

## Issue

Compiling a Kotlin/Native project targeting `ohosArm64` with `-Xbinary=splitBCfile=2` using konan CPF version `2.2.21-0.3.0-04` crashes the Gradle daemon with a native SIGSEGV in `libllvmstubs.dylib`.

## Environment

- **OS**: macOS 26.4.1 (aarch64, Darwin 25.4.0)
- **Java**: OpenJDK 21.0.10 (Temurin-21.0.10+7-LTS)
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **LLVM toolchain**: llvm-1914-aarch64-macos-dev-10 (shipped with konan)
- **Target**: ohosArm64

## Quick Start

```bash
# Clone this branch
git clone -b repro/ALI-30-splitBCfile2-crash https://github.com/linhandev/kn_samples.git
cd kn_samples

# Reproduce the crash (splitBCfile=2 is configured in kotlinApp/build.gradle.kts)
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon
```

## Expected Error

The build prints the llvm-split command it intends to run, then the Gradle daemon crashes:

```
llvm-split command: ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split -j=2 -o=.../tmp_ori_part_ --preserve-locals .../tmp_ori.bc

JVM crash log found: file://.../hs_err_pidXXXXX.log

FAILURE: Build failed with an exception.
* What went wrong:
Gradle build daemon disappeared unexpectedly (it may have been killed or may have crashed)
```

The JVM crash log reveals a native SIGSEGV:

```
#  SIGSEGV (0xb) at pc=0x0000000135f6733c
# Problematic frame:
# C  [libllvmstubs.dylib+0xa7f33c]  llvm::KotlinStubGenerator::run(llvm::Module&, llvm::AnalysisManager<llvm::Module>&)+0x68
```

## What Was Changed

One line added to `kotlinApp/build.gradle.kts` (line 18):

```kotlin
freeCompilerArgs += "-Xbinary=splitBCfile=2"
```

## Boundary Tests

Run the boundary test script to verify all three modes:

```bash
./scripts/test-splitBCfile-boundary.sh
```

| Configuration | Result |
|---|---|
| No splitBCfile flag (default) | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=1` | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=2` | **BUILD FAILED** (daemon SIGSEGV crash) |

## llvm-split Binary Verification

```bash
ls ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/ | grep split
# (no output - llvm-split is NOT shipped in the toolchain)
```

## Root Cause Indicators

1. `llvm-split` binary is **missing** from the `llvm-1914-aarch64-macos-dev-10` toolchain bundle
2. `splitBCfile=2` triggers a code path that attempts to invoke `llvm-split` externally
3. The crash manifests as a SIGSEGV in `llvm::KotlinStubGenerator::run` within `libllvmstubs.dylib` during the LLVM optimization pipeline, suggesting the missing tool causes a fatal error in the native compiler infrastructure rather than a clean Java-level IOException
4. `splitBCfile=1` uses a different code path that does not require the external `llvm-split` binary

## Reproducibility

CONSISTENT - reproduced on 2 consecutive runs with identical crash signatures.
