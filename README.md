# Reproduction: splitBCfile=2 Compile Crash (ALI-31)

## Issue

Compiling a Kotlin/Native project targeting `ohosArm64` with `-Xbinary=splitBCfile=2` using konan CPF version `2.2.21-0.3.0-04` crashes because the `llvm-split` binary is missing from the LLVM toolchain bundle.

## Environment

- **OS**: macOS (aarch64)
- **Java**: OpenJDK 21.0.10 (Temurin)
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **LLVM toolchain**: llvm-1914-aarch64-macos-dev-10 (shipped with konan)
- **Target**: ohosArm64

## Quick Start (Direct konanc — fastest)

```bash
git clone -b repro/ALI-31-splitBCfile2-crash https://github.com/linhandev/kn_samples.git
cd kn_samples

# Reproduce the crash directly with konanc (requires konan 2.2.21-0.3.0-04 installed)
~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04/bin/konanc \
  -target ohos_arm64 -produce dynamic \
  -o build/split2_lib \
  -Xbinary=splitBCfile=2 \
  kotlinApp/src/ohosArm64Main/kotlin/Main.kt
```

## Quick Start (Gradle)

```bash
git clone -b repro/ALI-31-splitBCfile2-crash https://github.com/linhandev/kn_samples.git
cd kn_samples
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon
```

## Expected Error

```
llvm-split command: ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split -j=2 -o=.../tmp_ori_part_ --preserve-locals .../tmp_ori.bc
error: compilation failed: Failed to execute llvm-split: Cannot run program
  "/Users/<user>/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)

  * Source files: Main.kt
  * Compiler version: 2.2.21-0.3.0-04
  * Output kind: DYNAMIC

exception: java.lang.RuntimeException: Failed to execute llvm-split: Cannot run program
  ".../.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)
    at o.j.k.backend.konan.driver.phases.BitcodeKt.splitBitcodeFile-BzPDsQc(Bitcode.kt:599)
    at o.j.k.backend.konan.driver.phases.BitcodeKt.runBitcodePostProcessingCoroutines(Bitcode.kt:613)
    at o.j.k.backend.konan.driver.phases.TopLevelPhasesKt.compileModule(TopLevelPhases.kt:417)
```

## Boundary Tests

| Configuration | Command | Result |
|---|---|---|
| No splitBCfile flag | `konanc -target ohos_arm64 -produce dynamic -o out Main.kt` | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=1` | `konanc ... -Xbinary=splitBCfile=1 ...` | BUILD SUCCESSFUL |
| `-Xbinary=splitBCfile=2` | `konanc ... -Xbinary=splitBCfile=2 ...` | **BUILD FAILED** (llvm-split not found) |

Run all three:

```bash
# Control: no flag (should succeed)
~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04/bin/konanc \
  -target ohos_arm64 -produce dynamic -o build/nosplit \
  kotlinApp/src/ohosArm64Main/kotlin/Main.kt

# Control: splitBCfile=1 (should succeed)
~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04/bin/konanc \
  -target ohos_arm64 -produce dynamic -o build/split1 \
  -Xbinary=splitBCfile=1 \
  kotlinApp/src/ohosArm64Main/kotlin/Main.kt

# Crash: splitBCfile=2 (should fail)
~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04/bin/konanc \
  -target ohos_arm64 -produce dynamic -o build/split2 \
  -Xbinary=splitBCfile=2 \
  kotlinApp/src/ohosArm64Main/kotlin/Main.kt
```

## llvm-split Binary Verification

```bash
ls ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/ | grep split
# (no output - llvm-split is NOT shipped in the toolchain)

# llvm-link IS present (used for the re-link step):
ls ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/ | grep llvm-link
# llvm-link
```

## Root Cause

The `splitBCfile` option controls parallel bitcode optimization in the Kotlin/Native compiler:

- **`splitBCfile=1`** (sequential): Uses `runBitcodePostProcessing()` — processes the entire bitcode module in a single pass. Does NOT require `llvm-split`.
- **`splitBCfile=2`** (parallel): Uses `runBitcodePostProcessingCoroutines()` which:
  1. Writes bitcode to a temp file
  2. Calls `llvm-split -j=2` to split it into 2 partitions
  3. Optimizes each partition in parallel using Kotlin coroutines (each with its own LLVM context)
  4. Links optimized partitions back with `llvm-link`
  5. Runs final LTO optimization

The crash occurs at step 2 because `llvm-split` is **not included** in the `llvm-1914-aarch64-macos-dev-10` toolchain bundle shipped with konan CPF 2.2.21-0.3.0-04.

For OHOS_ARM64 targets, the default value of `splitBCfile` is `2` (parallel), meaning this crash affects **all OHOS_ARM64 builds by default** unless the user explicitly sets `-Xbinary=splitBCfile=1`.

## Reproducibility

**CONSISTENT** — crashes every time with `splitBCfile=2`. Succeeds every time with `splitBCfile=1` or no flag.
