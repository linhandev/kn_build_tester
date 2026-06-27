# Plan: build & publish HiLog cinterop klib (cpf 0.4) for the HUAWEI consumer

## Goal
Produce a klib `com.example:hilog-klib:1.0-SNAPSHOT` (package `platform.PerformanceAnalysisKit.HiLog`,
symbols identical to cpf 0.4's platformLib HiLog) built with **cpf 0.4** (`2.2.21-0.4.0-03`), published
to **maven local**. It is consumed by the **2.3.20-HUAWEI** project in the `kn_samples-bare` repo.

This repo (`kn_samples-ohos-def`) becomes the **producer only**. The consumer work happens in
`kn_samples-bare`.

## Repo role split
- `kn_samples-ohos-def` (HERE) = producer, Kotlin = cpf 0.4 (`2.2.21-0.4.0-03`)
- `kn_samples-bare` (other repo) = consumer, Kotlin = `2.3.20-HUAWEI`, depends on the published klib

## Steps (in this repo)

### 1. Move cpf 0.4 dist ohos_arm64 def/klib to sibling `_backup`, empty original dirs
Target dist: `~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03`
- `konan/platformDef/ohos_arm64/*.def` (170 files) → `konan/platformDef/ohos_arm64_backup/`
- `klib/platform/ohos_arm64/*` (170 klib dirs) → `klib/platform/ohos_arm64_backup/`
- Keep `ohos_arm64/` dirs as empty placeholders. Leave `ohos_x64` untouched.
Done BEFORE build so cpf 0.4 builds the HiLog klib from the project's own def, not its built-in one.

### 2. `gradle.properties`
- `kotlinVersion=2.2.21-0.4.0-03` (was `2.3.20-HUAWEI`)
- Add `kotlin.native.home=/Users/ohoskt/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03`
  (cpf 0.4's KN embeddable jar has no Maven coordinate; KGP loads it via kotlin.native.home)

### 3. `settings.gradle.kts`
- `include("hilog-klib")` (replace `include("kotlinApp")`)
- Keep the Huawei devcloud repo block + add `mavenLocal()` for KGP resolution fallback
  (cpf 0.4 KGP is in gradle cache; KN embeddable via kotlin.native.home)

### 4. Delete `kotlinApp/` (including `nativeInterop/ohosArm64/HiLog.def`)
Per user: "当前仓库里的 hilog def 干掉".

### 5. New module `hilog-klib/`
- `hilog-klib/build.gradle.kts`:
  - `plugins { kotlin("multiplatform"); `maven-publish` }`
  - `group = "com.example"`, `version = "1.0-SNAPSHOT"`, base archivesName = `hilog-klib`
  - `kotlin { ohosArm64 { compilations { val main by getting { cinterops { create("HiLog") } } } } }`
    (default defFile lookup: `nativeInterop/ohosArm64/HiLog.def`)
  - NO `binaries { sharedLib }` — we publish a klib, not a .so
  - `publishing { publications { create<MavenPublication>("ohosArm64") { from(components["ohosArm64Api"]) // or the klib component } } }`
    (use the KMP 2.2.21 klib publication component name, verify at impl time)
- `hilog-klib/nativeInterop/ohosArm64/HiLog.def` = cpf 0.4 original verbatim:
  ```
  package = platform.PerformanceAnalysisKit.HiLog
  headers = hilog/log.h
  headerFilter = hilog/log.h
  linkerOpts = -lhilog_ndk.z
  language = C++
  compilerOpts = -std=c++17
  enableUndefinedApiProtection = true
  ```
  (cpf 0.4's konan.properties resolves sysroot/llvm automatically; no -I/-L needed)

### 6. Build & publish
`./gradlew :hilog-klib:publishToMavenLocal`
Verify `~/.m2/repository/com/example/hilog-klib/1.0-SNAPSHOT/` has the `.klib` (+ pom).

### 7. Verify symbol parity with cpf 0.4 platformLib HiLog
Compare the published klib's linkdata vs the moved-aside `org.jetbrains.kotlin.native.platform.HiLog`
to confirm `OH_LOG_Print` etc. are present.

## Consumer (in kn_samples-bare, after producer publishes) — separate step
- `kotlinApp/build.gradle.kts`: drop `cinterops { hiLog }`, drop `nativeInterop/`, add
  `dependencies { ohosArm64MainImplementation("com.example:hilog-klib:1.0-SNAPSHOT") }`
- `settings.gradle.kts` already has `mavenLocal()` and stays on `2.3.20-HUAWEI`.
- Run `startHarmonyAppDebug` to validate the HUAWEI project links the cpf-0.4-built klib.
- ABI note: klib `abi_version=2.2.0` read by `2.3.20` compiler — same major (2.x), supported;
  verify at link time.

## Risks / open items
- KN embeddable loading: relies on `kotlin.native.home`. If KGP still tries to resolve the
  `kotlin-native-compiler-embeddable:2.2.21-0.4.0-03` Maven dep (404), may need to exclude it or
  preinstall into maven local. Verify at first build.
- KMP 2.2.21 klib publication component name (`ohosArm64Api` vs `klib` vs `ohosArm64`) — confirm DSL.
- Moving dist files is destructive; `_backup` dirs are the recovery path.
