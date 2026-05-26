# Reproduction: splitBCfile=2 → SIGSEGV in KotlinStubGenerator (ALI-39)

## Issue

Compiling a Kotlin/Native project targeting ohos arm64 with `-Xbinary=splitBCfile=2` (parallel optimization) causes a **SIGSEGV crash** in `libllvmstubs.dylib` → `llvm::KotlinStubGenerator::run()`, killing the Gradle daemon. This crash occurs **regardless of which LLVM toolchain is used** — both the default `llvm-1914-aarch64-macos-dev-10` and the `mpcore-llvm-19-aarch64-macos-dev-11` produce identical crashes.

## Environment

- **OS**: macOS 26.4.1 (aarch64, Mac16,9)
- **Java**: OpenJDK Temurin-21.0.10+7
- **Kotlin/Native CPF**: 2.2.21-0.3.0-04
- **Gradle**: 8.9

## Quick Start

```bash
git clone -b repro/ALI-39-mpcore-llvm11-crash https://github.com/linhandev/kn_samples.git
cd kn_samples

# Crashes with default LLVM (no swap needed):
./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-daemon --stacktrace

# Also crashes with mpcore LLVM swapped in:
# (see scripts/reproduce-ALI-39.sh for automated LLVM swap + build)
```

## Crash Details

```
#  SIGSEGV (0xb) at pc=0x0000000137a7f33c, pid=40386, tid=76563
#  Problematic frame:
#  C  [libllvmstubs.dylib+0xa7f33c]  llvm::KotlinStubGenerator::run(llvm::Module&, llvm::AnalysisManager<llvm::Module>&)+0x68

siginfo: si_signo: 11 (SIGSEGV), si_code: 2 (SEGV_ACCERR), si_addr: 0x0000000000000000

Native frames:
C  [libllvmstubs.dylib+0xa7f33c]  llvm::KotlinStubGenerator::run()+0x68
C  [libllvmstubs.dylib+0x1fe90dc]  llvm::PassManager<llvm::Module,...>::run()+0x1b0
C  [libllvmstubs.dylib+0xa3984c]  LLVMRunPasses+0x23c
C  [libllvmstubs.dylib+0xc428]     Java_llvm_llvm_kniBridge798+0x38
j  llvm.llvm.LLVMRunPasses(...)
j  org.jetbrains.kotlin.backend.konan.LlvmOptimizationPipeline.execute(...)
j  ...BitcodeKt.optimizationPipelinePass$lambda$0(...)
j  ...BitcodeKt$runBitcodePostProcessingCoroutines$...$1$jobs$1$1.invokeSuspend(...)
```

## Boundary Test Results

| Configuration | LLVM Toolchain | splitBCfile | Result |
|---|---|---|---|
| Default | llvm-1914-aarch64-macos-dev-10 | (none) | **BUILD SUCCESSFUL** |
| Default + split | llvm-1914-aarch64-macos-dev-10 | =2 | **SIGSEGV CRASH** |
| mpcore LLVM | mpcore-llvm-19-aarch64-macos-dev-11 | (none) | **BUILD SUCCESSFUL** |
| mpcore LLVM + split | mpcore-llvm-19-aarch64-macos-dev-11 | =2 | **SIGSEGV CRASH** |

**The LLVM swap is a red herring.** The crash is in `libllvmstubs.dylib` (KN compiler's bundled native library), not in the external LLVM toolchain.

## Root Cause Indicators

1. `llvm::KotlinStubGenerator::run()` dereferences a NULL pointer (`si_addr: 0x0`) when processing split bitcode modules
2. The crash occurs in `runBitcodePostProcessingCoroutines` — the parallel processing path activated by `splitBCfile=2`
3. After `llvm-split` divides the bitcode, `LLVMRunPasses` is called on each piece, and `KotlinStubGenerator` crashes on the split pieces
4. The bug is in the KN compiler's native code (`libllvmstubs.dylib`), not in the LLVM toolchain

## Reproducibility

CONSISTENT — crashes every time with `splitBCfile=2` on ohosArm64, regardless of LLVM version.
