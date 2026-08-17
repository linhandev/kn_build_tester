# IS-87 Reproduction: Feature-set matching triggers aggressive KLIB runtime inlining

## Issue

Setting OHOS arm64 target CPU features to `+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a`
causes the LLVM codegen step (`clang++ -O3`) to take 2.5x longer than without it,
turning a ~10 minute release build into a 26+ minute build.

**Key finding**: `+fix-cortex-a53-835769` alone is NOT the trigger (10m 39s, same as
baseline). The slowdown occurs only when the main compilation's feature set **matches**
the feature set baked into the KLIB dependencies (stdlib, compose runtime, etc.).
When the feature sets match, the LLVM backend performs more aggressive inlining of
`alwaysinline` KLIB runtime functions at every call site, causing compile time explosion.

## Environment

- **Host**: macOS arm64 (Apple M-series)
- **Kotlin/Native prebuilt**: `2.2.21-0.5.0-14` (CPF 2.2.21 series)
- **LLVM**: CPF LLVM 19 (`cpf-llvm-19-aarch64-macos-dev-19`)
- **Demo repo**: `kmp-cmp-test-demo`, `Performance` branch
- **Build task**: `linkReleaseSharedOhosArm64` (release shared library)
- **CPU features location**: `~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.5.0-14/konan/konan.properties`

## Build Results

| Feature set in konan.properties | Matches KLIB? | Build time | clang++ RSS |
|---|---|---|---|
| `+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a` (issue) | **YES** | **26m 7s** | 5-7 GB |
| `+fp-armv8,+neon,+reserve-x28,+v8a` (no fix-cortex) | No | 10m 38s | 0.5-3 GB |
| `+aes,+fp,+neon,+sha2,+asimd,+reserve-x28,+fix-cortex-a53-835769` (default) | No | 10m 14s | 0.5-3 GB |
| `+fix-cortex-a53-835769` (alone) | No | 10m 39s | 0.5-3 GB |

The slowdown occurs ONLY when the main compilation's features match the KLIB's features.

## Default vs Issue CPU features

```
# Default (konan.properties 2.2.21-0.5.0-14):
targetCpu.ohos_arm64 = cortex-a57
targetCpuFeatures.ohos_arm64 = +aes,+fp,+neon,+sha2,+asimd,+reserve-x28,+fix-cortex-a53-835769

# Issue (user-set):
targetCpuFeatures.ohos_arm64 = +fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a
```

## Root Cause: Feature-set matching + alwaysinline KLIB runtime

### Clang++ command line (no -mcpu/-mattr)

The clang++ invocation passes NO `-mcpu` or `-mattr` flags — CPU features come entirely
from the LLVM bitcode's per-function attributes:

```
clang++ -cc1 -emit-obj -mllvm -enable-compressed-bitmap-stackmap=true \
  -mllvm -global-isel=0 -mllvm -enable-kotlin-stub-generator=true \
  -x ir -triple aarch64-linux-ohos -O3 -ffunction-sections \
  -mrelocation-model pic out.bc -o libkn.so.o
```

### Two target attribute sets in the bitcode

The combined bitcode (`out.bc`, ~60 MB) contains functions with two different
target attribute sets:

1. **KLIB functions** (stdlib, compose runtime, etc.):
   ```
   target-cpu="generic"
   target-features="+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a"
   ```
   These features are baked into the KLIBs at build time and do NOT change when
   you modify `konan.properties`.

2. **Main compilation functions** (user code):
   ```
   target-cpu="cortex-a57"
   target-features=<from konan.properties>
   ```

### alwaysinline KLIB runtime functions

The bitcode contains **210 `alwaysinline` function references** across 21 attribute
groups — all using `target-cpu="generic"` with the issue's feature set. These are
KLIB runtime functions (memory management, null checks, type checks, etc.) that
MUST be inlined at every call site by the LLVM optimizer at `-O3`.

### Feature-set matching triggers more inlining

When the main compilation's features **match** the KLIB's features, the LLVM backend
can inline `alwaysinline` functions without target attribute reconciliation, leading
to more aggressive inlining and code bloat. When the features **don't match**, the
LLVM backend must reconcile attributes, which limits or changes the inlining behavior.

This explains why:
- `+fix-cortex-a53-835769` alone (10m 39s) is fast — doesn't match KLIB's full feature set
- Default konan features (10m 14s) are fast — has `+aes,+sha2,+asimd` that KLIB lacks
- Issue features (26m 7s) are slow — exact match with KLIB's features
- Removing `+fix-cortex-a53-835769` (10m 38s) is fast — breaks the match

## Reproduction Steps

```bash
# Clone this reproduction branch
git clone -b repro/is87-fix-cortex-a53-slow-codegen https://github.com/linhandev/kn_samples.git
cd kn_samples/repro

# Option A: Full issue feature set (slow, ~26 min)
./reproduce.sh issue

# Option B: Without +fix-cortex-a53-835769 (fast, ~10 min)
./reproduce.sh no-fix

# Option C: +fix-cortex-a53-835769 alone (fast, ~10 min)
./reproduce.sh fix-only

# Restore default konan.properties
./patch-konan-properties.sh default
```

## Inspecting the Bitcode

To verify the two target attribute sets during a build:

```bash
# Find the temp bitcode (build must be running)
BC=$(find /var/folders -name "out.bc" -newer /tmp 2>/dev/null | head -1)

# Disassemble and check target attributes
LLVM_DIS=~/.konan/dependencies/cpf-llvm-19-aarch64-macos-dev-19/bin/llvm-dis
$LLVM_DIS "$BC" -o /tmp/out.ll

# KLIB functions (generic CPU + issue features)
grep 'target-cpu.*generic.*target-features.*v8a' /tmp/out.ll | head -5

# Main functions (cortex-a57 + konan.properties features)
grep 'target-cpu.*cortex-a57' /tmp/out.ll | head -5

# Count alwaysinline functions
grep -c "alwaysinline" /tmp/out.ll
```

## Boundary Observations

- **Reproduces consistently**: the 26-minute build time is stable across runs
- **CPU-bound**: clang++ sustains 100% CPU — not I/O wait or lock contention
- **Memory-intensive**: RSS peaks at 5-7 GB with matching features vs 0.5-3 GB without
- **Feature-set dependent**: NOT any single feature — the SET must match the KLIB's
- **KLIB features are immutable**: changing konan.properties only affects the main
  compilation functions; KLIB functions retain their original target attributes
