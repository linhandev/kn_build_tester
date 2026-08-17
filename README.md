# IS-87 Reproduction: `+fix-cortex-a53-835769` causes slow OHOS arm64 release build

## Issue

Setting OHOS arm64 target CPU features to include `+fix-cortex-a53-835769` causes the
LLVM codegen step (`clang++ -O3`) to take 2-3x longer than without it, turning a ~10
minute release build into a 26+ minute build. With the full issue feature set
(`+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a`), the build takes **26m 7s**.
Removing `+fix-cortex-a53-835769` drops it to **10m 38s**, nearly matching the baseline
of **10m 14s**.

## Environment

- **Host**: macOS arm64 (Apple M-series)
- **Kotlin/Native prebuilt**: `2.2.21-0.5.0-14` (CPF 2.2.21 series)
- **LLVM**: CPF LLVM 19 (`cpf-llvm-19-aarch64-macos-dev-19`)
- **Demo repo**: `kmp-cmp-test-demo`, `Performance` branch
- **Build task**: `linkReleaseSharedOhosArm64` (release shared library)
- **CPU features location**: `~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.5.0-14/konan/konan.properties`

## Default vs Issue CPU features

```
# Default (konan.properties 2.2.21-0.5.0-14):
targetCpu.ohos_arm64 = cortex-a57
targetCpuFeatures.ohos_arm64 = +aes,+fp,+neon,+sha2,+asimd,+reserve-x28,+fix-cortex-a53-835769

# Issue (user-set):
targetCpuFeatures.ohos_arm64 = +fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a
```

## Reproduction Steps

```bash
# Clone this reproduction branch
git clone -b repro/is87-fix-cortex-a53-slow-codegen https://github.com/linhandev/kn_samples.git
cd kn_samples/repro

# Option A: Full issue feature set (slow, ~26 min)
./reproduce.sh issue

# Option B: Without +fix-cortex-a53-835769 (fast, ~10 min)
./reproduce.sh no-fix

# Option C: +fix-cortex-a53-835769 alone (isolating the trigger)
./reproduce.sh fix-only

# Restore default konan.properties
./patch-konan-properties.sh default
```

## Expected Results

| Feature set | Build time | clang++ CPU | clang++ RSS |
|---|---|---|---|
| `+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a` (issue) | **26m 7s** | 100% | 5-7 GB |
| `+fp-armv8,+neon,+reserve-x28,+v8a` (no fix) | 10m 38s | 100% | 0.5-3 GB |
| Default konan features (baseline) | 10m 14s | 100% | 0.5-3 GB |
| `+fix-cortex-a53-835769` (alone) | _in progress_ | 100% | 5.8 GB |

## Evidence

The slowdown occurs during the LLVM codegen phase. The `clang++` process compiles
`out.bc` (LLVM bitcode produced by Kotlin/Native) to `libkn.so.o` with `-O3`:

```
/Users/ohoskt/.konan/dependencies/cpf-llvm-19-aarch64-macos-dev-19/bin/clang++ \
  -cc1 -emit-obj -mllvm -enable-compressed-bitmap-stackmap=true \
  -mllvm -global-isel=0 -mllvm -enable-kotlin-stub-generator=true \
  -x ir -triple aarch64-linux-ohos -O3 -ffunction-sections \
  -mrelocation-model pic \
  /var/folders/.../konan_temp.../out.bc \
  -o /var/folders/.../konan_temp.../libkn.so.o
```

With `+fix-cortex-a53-835769`, clang++ sustains 100% CPU and peaks at 5-7 GB RSS,
consuming the majority of the 26-minute build time. Without it, clang++ peaks at
0.5-3 GB RSS and completes in about 4-5 minutes (out of the total 10-minute build).

## Boundary Observations

- **Reproduces consistently**: the 26-minute build time is stable across multiple runs
- **CPU-bound**: clang++ runs at 100% CPU — this is not an I/O wait or lock contention
- **Memory-intensive**: RSS peaks at 5-7 GB with `+fix-cortex-a53-835769` vs 0.5-3 GB without
- **Default features also include `+fix-cortex-a53-835769`**: the default konan.properties
  for 2.2.21-0.5.0-14 has `+fix-cortex-a53-835769` but builds in 10m 14s. The slowdown
  appears to require the *combination* of `+fix-cortex-a53-835769` with the issue's
  specific feature set (which lacks `+aes`, `+sha2`, `+asimd` but adds `+fp-armv8`, `+v8a`)

## Notable Observations

- `+fix-cortex-a53-835769` is a Cortex-A53 erratum 835769 workaround that patches certain
  load/store instruction sequences. The LLVM backend applies it during codegen, and
  the resulting instruction rewriting may trigger a pathological case at `-O3`
- The `clangFlags.ohos_arm64` in konan.properties includes `-mllvm -global-isel=0`,
  which disables GlobalISel. The erratum fix may interact with the default instruction
  selector path in a way that causes excessive compilation time
- The issue's feature set uses `+fp-armv8` and `+v8a` instead of the default's `+fp`
  and `+asimd`. These may enable different instruction patterns that trigger more
  erratum fix insertions
