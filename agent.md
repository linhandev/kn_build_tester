# Agent Guide — kn_sample

This is the repo-root guide for agents working in `kn_sample`. Read this before running anything.
For per-project detail see `producer/README.md`, `consumer-bare/agent.md`, `consumer-capi-demo/README.md`.

## What this repo proves

End-to-end that a Maven klib built with **cpf 0.4** (Kotlin `2.2.21-0.4.0-03`) is consumable by projects on **`2.3.20-HUAWEI`**. The klib (`org.cpf.kotlin:ohos-capi:22-0.1`) wraps 163 HarmonyOS cinterop defs (159 ohos-only + 4 dist-only) so a consumer with **no** dist platformLibs can still compile/link/run against OHOS C-APIs. A second klib `org.cpf.kotlin:static-lib-demo:22-0.1` demonstrates `.a` embedded in a klib.

The klib `depends` are self-closed to `org.cpf.kotlin` coordinates — no dependency on any consumer's dist.

## Repo layout

| Dir | Role | Kotlin | Artifact |
|---|---|---|---|
| `producer/` | builds & publishes klibs | `2.2.21-0.4.0-03` (cpf 0.4) | `org.cpf.kotlin:ohos-capi` (144 ohos def) + `org.cpf.kotlin:hms-capi` (19 hms 扩展 def, POM depend ohos-capi) → colab + m2; `:static-lib-demo` (test-only, m2 only) → repo-local `m2/` |
| `consumer-bare/` | minimal smoke consumer (只 ohos, 无 hms, 无 -L<HMS>) | `2.3.20-HUAWEI` | `kotlinApp` (HiLog + AVTranscoder via biz-klib) |
| `consumer-capi-demo/` | full CAPI smoke consumer (ohos + hms 两坐标) | `2.3.20-HUAWEI` | `composeApp` + `harmonyApp` (10-module UI autotest, 含 HMS 模块验 HandWrite+CANN) |
| `m2/` | repo-local Maven (gitignored, inspectable) | — | producer publishes here, consumers resolve from here |
| `sysroot/` | checked-in ohos + HMS sysroot (git-lfs for `.so/.a/.o`) | — | producer `-I`, consumer `-L` |
| `design/` | `DESIGN.md` + `SUMMARY.html` | — | link strategy, def-dependency design |
| `run-all.sh` | **the end-to-end driver** | — | see below |

## Prerequisites (host machine, not in this repo)

`run-all.sh` step 0 checks all of these and `fail`s fast if missing — don't pre-flight them yourself, just read the error.

- **DevEco Studio** at `/Applications/DevEco-Studio.app` — ohos main sysroot (`.so` stubs) + HAP toolchain (`ohpm`/`hvigor`/`hdc`), `hdc` on `PATH`.
- **cpf 0.4 KN dist** `~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03` — producer's `kotlin.native.home`.
- **HMS sysroot** `~/.konan/dependencies/sysroot-hms-aarch64-6.0.2.640-02` — extended Kit headers + stubs DevEco doesn't ship. (Repo also keeps a copy at `sysroot/sysroot-hms-aarch64-6.0.2.640-02`.)
- **devcloud Maven creds** in `consumer-bare/local.properties` and `consumer-capi-demo/local.properties` (git-ignored) — `huaweiMavenUser`/`huaweiMavenPass` to pull `2.3.20-HUAWEI` KGP. Or env vars `HUAWEI_MAVEN_USER`/`HUAWEI_MAVEN_PASS`.
- **A device on hdc** (`hdc list targets` non-empty). All three runtime steps install/uninstall `com.kotlin.demo`.

## How to run end-to-end

```bash
./run-all.sh
```

That single command does the whole pipeline. It is the canonical way to validate the repo after any change. Exit 0 = full pipeline green; exit 1 = a real failure (a failed autotest case counts as failure). Color banners mark each step.

### What the 4 steps do

1. **producer publish** — `./publish-ohos-capi.sh` (发 `org.cpf.kotlin:ohos-capi` + `org.cpf.kotlin:hms-capi` 到 colab + m2; `static-lib-demo` 仍走 `./gradlew :static-lib-demo:publish`; `biz-klib` 由 `./gradlew :biz-klib:publish` 单独发到 m2,bare 依赖它). Clears `m2/org/cpf/kotlin/{ohos-capi,hms-capi,static-lib-demo}` first, then republishes. Expects ohos-capi 145 klibs (144 cinterop + 1 main) + hms-capi 20 (19 + 1). Count drift from added defs is fine, a *missing* klib is not.

> **开发流程(本地 m2 vs colab)**:有变更测试时,consumer 走**本地 m2**(解开 `consumer-capi-demo/settings.gradle.kts` 的本地 m2 注释,从 `m2/` 拉待测版本);`consumer-bare` 本地 m2 常开。发布完新版本后,consumer 改回走 colab 远程仓(本地 m2 注释)。当前 `klibVersion=22-0.2`,本地 m2 已发,consumer-capi-demo settings 本地 m2 已解开(测试期)。
2. **consumer-bare** — links `:kotlinApp:linkDebugSharedOhosArm64` (只依赖 ohos-capi,不加 hms,无 `-L<HMS>`), prints `libc2k.so` NEEDED (only `libc/libavtranscoder/libhilog_ndk.z/libc++_shared` + 探针 `libEGL` — bare sonames, no embedded `.so`), then `scripts/check-asneeded-state.sh` 断言探针在 NEEDED (全局 no-as-needed) + ohcrypto 不在 NEEDED (def 内 as-needed 生效). Then `startHarmonyAppDebug` deploys.
3. **consumer-capi-demo build** — `:composeApp:publishDebugBinariesToHarmonyApp` builds `libkn.so`, then `ohpm install` + `hvigor assembleHap` produces `entry-default-signed.hap`, `hdc install`.
4. **consumer-capi-demo autotest** — `python3 autotest.py`: enters each of 10 module pages (含 HMS 模块:HandWrite 预测点不依赖 ohos / CANN HiAI 版本依赖 ohos 验 POM 传递), taps "运行验证", polls `dumpLayout` for the manifest stats, writes `test_reports/` (HTML+CSV). Parses the case-level summary (`成功/失败/总计`), not the module-level "10/10".

### Reading the result

The script prints a final `端到端测试结果` block. `consumer-capi-demo` reports case-level `成功/失败/总计`. A non-zero `失败` is a real test failure → exit 1. The **Failure** module is expected-error; `autotest.py` counts its expected-error cases as success.

### Running a single step (debugging)

`run-all.sh` is linear and `set -euo pipefail`. To isolate a step, run its commands directly:

```bash
# 1. producer only
cd producer && ./gradlew :ohos-capi:publish :static-lib-demo:publish --no-daemon

# 2. bare only (needs m2/ populated from step 1)
cd consumer-bare && ./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-daemon
cd consumer-bare && ./gradlew :kotlinApp:startHarmonyAppDebug --no-daemon --rerun-tasks

# 3. capi-demo build only
cd consumer-capi-demo && ./gradlew :composeApp:publishDebugBinariesToHarmonyApp --no-daemon
cd consumer-capi-demo/harmonyApp && \
  /Applications/DevEco-Studio.app/Contents/tools/ohpm/bin/ohpm install --all \
    --registry https://ohpm.openharmony.cn/ohpm/ --strict_ssl true && \
  node /Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw.js \
    --mode module -p module=entry@default -p product=default -p buildMode=debug \
    -p requiredDeviceType=phone assembleHap --analyze=false --parallel --incremental

# 4. autotest only (needs the HAP from step 3 installed)
cd consumer-capi-demo && python3 autotest.py
```

The HAP toolchain env (`NODE_HOME`, `DEVECO_SDK_HOME`, `PATH` to node) is set inside `run-all.sh` step 3 — replicate it if you run step 3's `hvigor` outside the script.

## Known gotchas

- **Step 2 `startHarmonyAppDebug` can hang on a stuck `hdc` daemon**, leaving no `BUILD SUCCESSFUL` and no `❌` line — the output just stops after the NEEDED block. Symptom: `ps` shows a lingering `hdc -m -s`. Fix: `pkill -f "hdc -m -s"`, confirm no active `gradlew`/`hvigor`/`autotest`, then re-run `./run-all.sh`. A stuck Gradle daemon (PPID 1, idle) is harmless — leave it.
- **Device must be on hdc for steps 2–4.** `hdc list targets` empty → step 0 fails. If a device drops mid-run, later steps fail at install/start.
- **`m2/` is gitignored.** It's regenerated by step 1. Don't expect it to exist on a fresh clone until `run-all.sh` (or step 1) runs once.
- **`local.properties` is git-ignored** in both consumers — must carry `huaweiMavenUser`/`huaweiMavenPass`, or those env vars must be set. Step 0 checks the file exists, not the creds; a missing cred surfaces as a Gradle resolution failure in step 2/3.
- **Def count drift:** `run-all.sh` says "期望 160" in a comment but recent publishes emit 164. The assertion is `>0 klibs`, not `==160`. Don't "fix" the count unless a def was actually added/removed.
- **Both consumers install as `com.kotlin.demo`.** `run-all.sh` uninstalls before each install; running bare then capi-demo back-to-back is fine. Running them in parallel against one device is not.
