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
./scripts/coverage/run-android.sh   # needs ANDROID_HOME — stops and tells you what's missing
```

## Results matrix

The coverage **numbers** are not the point — all backends share the same common test code, so percentages are nearly identical and meaningless in isolation. What matters is whether each backend can **produce a report that maps coverage back to `.kt` source at line/function level and correctly flags uncovered code**. To prove that, `Calculator.kt` has an `uncalledUtility()` method that **no test calls** — every report must flag it as uncovered.

| Backend | Official? | Mechanism | Tool | Can it flag uncovered .kt code? |
|---|---|---|---|---|
| **jvm** | ✅ JB official | JaCoCo agent on build-JVM bytecode | Kover (`koverHtmlReportJvm`) | ✅ METHOD 6/7, source page 8 fc + 2 nc lines |
| **js** | ❌ no JB integration | V8 native coverage + source-map remap to .kt | c8 + `NODE_V8_COVERAGE` | ✅ Calculator % Funcs 75% (uncalledUtility flagged) + `Calculator.kt.html` per-file page |
| **wasmJs** | ❌ no JB integration | (would need V8 wasm coverage + wasm source-map) | — | ❌ no working path (Node 24 has no wasm-coverage flag) |
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
- **No working coverage path** as of Kotlin 2.4 / Node 24. The script records this honestly rather than faking a report.
- What exists: Kotlin emits a `.wasm.map` (source-map v3) whose `sources` correctly point at the `.kt` files — remapping is *theoretically* possible.
- What's missing: Node 24 exposes **no CLI flag** for wasm code coverage (`--experimental-wasm-code-coverage` is absent; `node --help` shows only `--disable-wasm-trap-handler`). `NODE_V8_COVERAGE` collects coverage for the `.mjs` JS glue only, **not** for `.wasm` function/block ranges — so c8 cannot remap to `.kt`.
- The only theoretical path is the V8 Inspector `Profiler.startPreciseCoverage{detailed:true}`, and even then no mainstream tool consumes wasm source maps. Left as a documented gap.

### android — `run-android.sh`
- Official path: **host unit tests run on the build JVM**, so Kover's JVM agent collects coverage directly (same engine as the jvm target — no device/emulator needed). The Android SDK is required only to **compile** `androidMain` (AGP needs `ANDROID_HOME` at configuration time).
- Tasks: `:library:testAndroidHostTest` → `:library:koverHtmlReportAndroid` / `:library:koverXmlReportAndroid`
  - **Gotcha:** under AGP 9's `com.android.kotlin.multiplatform.library`, the host-test task is `testAndroidHostTest` — **not** `testDebugUnitTest` (that name no longer exists).
- Requires `ANDROID_HOME` + `platforms;android-36` + `build-tools;36.0.0`. The script **stops** at the pre-flight if the SDK is missing and prints install instructions — per the user's "don't auto-skip, tell me what's missing" rule.
- Verified: LINE 91.3% (21/23) — same common code as jvm, identical numbers.
