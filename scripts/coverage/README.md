# KMP Backend Coverage Scripts

One script per backend. Each runs the full flow: **build → run tests → generate report**, then prints the report path.
All scripts source `_common.sh` which sets the isolated `GRADLE_USER_HOME` next to the worktree.

Project: Kotlin **2.4.0**, Kover **0.9.8**, AGP **9**, Gradle **9.1**. Based on the official `Kotlin/multiplatform-library-template`.

## Run

```bash
cd ~/git/worktree/kn_samples-kmp-coverage-survey
./scripts/coverage/run-jvm.sh
./scripts/coverage/run-js.sh
./scripts/coverage/run-wasm.sh
./scripts/coverage/run-wasi.sh
./scripts/coverage/run-android.sh   # needs ANDROID_HOME — stops and tells you what's missing
```

## Results matrix

The coverage **numbers** are not the point — all backends share the same common test code, so percentages are nearly identical and meaningless in isolation. What matters is whether each backend can **produce a report that maps coverage back to `.kt` source at line/function level and correctly flags uncovered code**. To prove that, `Calculator.kt` has an `uncalledUtility()` method that **no test calls** — every report must flag it as uncovered.

| Backend | Official? | Mechanism | Tool | Can it flag uncovered .kt code? |
|---|---|---|---|---|
| **jvm** | ✅ JB official | JaCoCo agent on build-JVM bytecode | Kover (`koverHtmlReportJvm`) | ✅ METHOD 6/7, source page 8 fc + 2 nc lines |
| **js** | ❌ no JB integration | V8 native coverage + source-map remap to .kt | c8 + `NODE_V8_COVERAGE` | ✅ Calculator % Funcs 75% (uncalledUtility flagged) + `Calculator.kt.html` per-file page |
| **wasmJs** | ❌ no JB integration | `%DebugCollectWasmCoverage` (Node 25 `--wasm-code-coverage`) + custom remapper via `.wasm.map` | Node 25 + `wasm-remap.py` | ✅ file/function-level accurate (Calculator.kt 27/34); line-level coarse (sparse source map) |
| **wasmWasi** | ❌ no JB integration | same V8 path as wasmJs; WASI test mjs needs `--allow-wasi` + `startUnitTests()` entry | Node 25 + `wasm-remap.py` (shared) | ✅ Calculator.kt 29/39 (74.4%); same line-level caveat as wasmJs |
| **android** | ✅ JB official | Kover JVM agent on host unit test (runs on build JVM) | Kover (`koverHtmlReportAndroid`) | ✅ same as jvm: METHOD 6/7, source page fc/nc lines |

## Per-backend notes

### jvm — `run-jvm.sh`
- Task: `:library:jvmTest` → `:library:koverHtmlReportJvm` / `:library:koverXmlReportJvm`
- Report: `library/build/reports/kover/htmlJvm/index.html`, `reportJvm.xml`
- **Gotcha:** the all-target `koverHtmlReport` task configures the Android target too and fails without `ANDROID_HOME`. Use the `*Jvm`-specific tasks to avoid that dependency.
- Verified: LINE 91.3% (21/23), BRANCH 100% — the 2 uncovered lines are an intentionally-untested branch.

### js — `run-js.sh`
- No JB coverage integration. Path: `NODE_V8_COVERAGE` → V8 writes byte-offset coverage on node exit → **c8** consumes it and uses the compiler-generated source map to remap `.js` coverage back onto `.kt`.
- Report: `build/reports/coverage/js/c8/index.html` (+ lcov)
- **Key finding:** contrary to older reports, source-map remap to `.kt` **works** on Kotlin 2.4 + c8 11. `Calculator.kt` / `CustomFibi.kt` / `fibiprops.js.kt` all appear with correct coverage.
- **Gotchas (all required):**
  1. Gradle does **not** forward arbitrary env vars to the forked node process. The `testTask { environment("NODE_V8_COVERAGE", ...) }` block in `library/build.gradle.kts` is mandatory.
  2. `c8` resolves source maps relative to **cwd** — the script runs c8 from the worktree root, not from `$PWD`.
  3. `c8 report` defaults `--clean=true`; pass `--clean false` so it doesn't wipe the temp dir before reading.
  4. Node writes coverage asynchronously on graceful exit; the script waits for the file count to stabilize.

### wasmJs — `run-wasm.sh`
- wasmJs has **no off-the-shelf coverage path** — c8/istanbul do not consume wasm source maps. This script drives a working custom flow on **Node 25+**.
- Flow: build wasmJs test → `%DebugCollectWasmCoverage()` (Node 25 `--wasm-code-coverage --allow-natives-syntax --no-wasm-lazy-compilation`) → `wasm-remap.py` joins coverage ranges with `.wasm.map` → per-`.kt` report (+ lcov).
- Why Node 25: Node 24 has **no** `--wasm-code-coverage` flag (`NODE_V8_COVERAGE` collects `.mjs` glue only, not `.wasm` blocks). Node 25 exposes it. `library/build.gradle.kts` pins `version = "25.0.0"` on the wasmJs `nodejs {}` block so gradle downloads it.
- Verified: 30476 wasm block ranges collected (2071 covered / 28405 uncovered). Remap output: `Calculator.kt 27/34 (79.4%)`, `CalculatorTest.kt 37/40 (92.5%)`; `uncalledUtility()` correctly flagged uncovered.
- **Caveat — line-level resolution is coarse:** the Kotlin/Wasm `.wasm.map` is sparse (~128 segments, function-granularity), so uncovered ranges get attributed to the nearest preceding mapped line, not precisely the source line. File-level and function-level coverage are accurate. A denser (DWARF line-program) source map would fix line-level.

### wasmWasi — `run-wasi.sh`
- Same V8 coverage path as wasmJs — `%DebugCollectWasmCoverage` is V8-level and target-agnostic. Shares `wasm-remap.py` unchanged.
- Only two differences from wasmJs:
  1. The wasmWasi test mjs sets up WASI (needs `--allow-wasi`, since WASI is experimental in Node 25) and exports `startUnitTests` **without** auto-calling it. `wasm-wasi-collector.mjs` imports it and calls `startUnitTests()` before collecting.
  2. wasmWasi emits `.wasm.map` **by default** — the `sourceMap`/`sourceMapEmbedSources` compiler options are wasmJs-specific and rejected by wasmWasi, so no extra config is needed.
- Verified on Kotlin 2.4.0 (no daemon workaround needed): 30541 ranges (2654 covered). Remap: `Calculator.kt 29/39 (74.4%)`, `CalculatorTest.kt 35/36 (97.2%)`.
- Same line-level caveat as wasmJs (sparse source map).

### android — `run-android.sh`
- Official path: **host unit tests run on the build JVM**, so Kover's JVM agent collects coverage directly (same engine as the jvm target — no device/emulator needed). The Android SDK is required only to **compile** `androidMain` (AGP needs `ANDROID_HOME` at configuration time).
- Tasks: `:library:testAndroidHostTest` → `:library:koverHtmlReportAndroid` / `:library:koverXmlReportAndroid`
  - **Gotcha:** under AGP 9's `com.android.kotlin.multiplatform.library`, the host-test task is `testAndroidHostTest` — **not** `testDebugUnitTest` (that name no longer exists).
- Requires `ANDROID_HOME` + `platforms;android-36` + `build-tools;36.0.0`. The script **stops** at the pre-flight if the SDK is missing and prints install instructions — per the user's "don't auto-skip, tell me what's missing" rule.
- Verified: LINE 91.3% (21/23) — same common code as jvm, identical numbers.
