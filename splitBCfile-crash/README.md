# ALI-33: splitBCfile=2 Compile Crash on OHOS arm64

## Issue

Compiling a Kotlin/Native project targeting OHOS arm64 with `-Xbinary=splitBCfile=2` crashes because the `llvm-split` binary is missing from the bundled LLVM distribution.

## Environment

- **OS**: macOS (Apple Silicon / aarch64)
- **Compiler**: konan cpf `2.2.21-0.3.0-04` (kotlin-native-prebuilt-macos-aarch64)
- **Target**: `ohos_arm64`
- **LLVM**: `llvm-1914-aarch64-macos-dev-10` (bundled in `~/.konan/dependencies/`)

## Prerequisites

Install `kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04` into `~/.konan/`.

## Reproduction Steps

```bash
cd splitBCfile-crash

# 1. Baseline — should succeed
./build.sh no-split

# 2. splitBCfile=1 — should succeed (no splitting needed)
./build.sh split1

# 3. splitBCfile=2 — CRASHES (llvm-split not found)
./build.sh split2

# Run all three:
./build.sh all
```

## Expected vs Actual

| Flag | Expected | Actual |
|---|---|---|
| (none) | Compiles successfully | Compiles successfully |
| `splitBCfile=1` | Compiles successfully | Compiles successfully |
| `splitBCfile=2` | Compiles with parallel bitcode optimization | **CRASH**: `llvm-split` not found |
| `splitBCfile=3` | Compiles with parallel bitcode optimization | **CRASH**: same error |

## Error Output (splitBCfile=2)

```
llvm-split command: ~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split -j=2 ...
error: compilation failed: Failed to execute llvm-split: Cannot run program
  "~/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin/llvm-split":
  Exec failed, error: 2 (No such file or directory)
```

Stack trace points to `Bitcode.kt:599` → `splitBitcodeFile-BzPDsQc`.

## Root Cause

The LLVM distribution `llvm-1914-aarch64-macos-dev-10` does not include the `llvm-split` binary. When `splitBCfile >= 2`, the compiler invokes `llvm-split` to partition the bitcode file for parallel optimization, but the tool is absent.
