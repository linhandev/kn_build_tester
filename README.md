# ALI-53 Reproduction: "Could not find org.jetbrains.kotlin.native.platform.ohos"

## Issue
Version 2.2.21-0.3.0-05 reportedly fails with: `Could not find "org.jetbrains.kotlin.native.platform.ohos"`

## Reproduction Result: COULD NOT REPRODUCE

After extensive testing, this error **does not reproduce** with a minimal K/N project using version 2.2.21-0.3.0-05 targeting OHOS arm64.

### What was tested

1. **Minimal single-module KMP project** with ohosArm64 + iosArm64 targets → BUILD SUCCESSFUL
2. **Multi-module project** (shared library + app depending on it) → BUILD SUCCESSFUL
3. **Platform.posix import** in ohosArm64Main → BUILD SUCCESSFUL
4. **kotlinx-coroutines dependency** → fails differently (no ohosArm64 variant in coroutines 1.10.2)
5. **cinterop commonization enabled** → BUILD SUCCESSFUL

### Key findings about the distribution

The `kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-05` distribution:
- ✅ Contains `ohos_arm64` platform klibs (`klib/platform/ohos_arm64/` — 172 entries)
- ✅ Contains `posix`, `linux`, `builtin` and all OHOS-specific klibs
- ✅ Contains klib caches for `ohos_arm64` (`klib/cache/ohos_arm64-*`)
- ❌ Does NOT contain an aggregate `org.jetbrains.kotlin.native.platform.ohos` klib (individual klibs only)

The compiler source (`KonanConfig.kt`) references `org.jetbrains.kotlin.native.platform.ohos` in `moduleIncludeOnly`, but this path is only activated when `emitStdlib=true` (a distribution-build option, not user-facing).

## How to run

```bash
git clone -b repro/ALI-53-ohos-platform-klib-missing https://github.com/linhandev/kn_samples.git
cd kn_samples
./gradlew :kotlinApp:linkDebugSharedOhosArm64
./gradlew :kotlinApp:linkDebugFrameworkIosArm64
```

Both commands complete successfully with BUILD SUCCESSFUL.

## Hypothesis

The original error likely requires one of:
- A specific external KMP library dependency that declares `org.jetbrains.kotlin.native.platform.ohos` as a transitive dependency
- A different host OS (Linux) where the distribution structure might differ
- A specific Gradle plugin configuration not captured in this minimal project
- The error may originate from a published library's metadata referencing this non-existent module coordinate
