# Reproduction: KLIB resolver Could not find "org.jetbrains.kotlin.native.platform.ohos"

## Issue
When a klib's manifest declares a dependency on `org.jetbrains.kotlin.native.platform.ohos`, the cinterop tool fails with:
```
e: KLIB resolver: Could not find "org.jetbrains.kotlin.native.platform.ohos" in [search_paths]
```

## Environment
- Kotlin/Native: 2.2.21-0.3.0-05
- Target: ohos_arm64
- OS: macOS (tested on Apple Silicon)

## Root Cause
The klib manifest contains `depends=stdlib org.jetbrains.kotlin.native.platform.ohos`, but this aggregate platform library name doesn't exist as a resolvable klib file in the distribution. The distribution only contains `org.jetbrains.kotlin.native.platform.posix` in `$DIST/klib/platform/ohos_arm64/`.

The error occurs specifically in cinterop because it calls `libraryResolver()` without `resolveManifestDependenciesLenient = true`, causing strict resolution of all manifest dependencies.

## Reproduction Steps

### 1. Build the lib module (creates a klib)
```bash
cd lib
./gradlew :compileKotlinOhosArm64
```

### 2. Patch the manifest to add the bad dependency
```bash
sed -i.bak 's/^depends=stdlib$/depends=stdlib org.jetbrains.kotlin.native.platform.ohos/' \
  build/classes/kotlin/ohosArm64/main/klib/lib/default/manifest
```

### 3. Run cinterop with the patched klib
```bash
cd ../cinterop-test
KLIB_DIR=../lib/build/classes/kotlin/ohosArm64/main/klib/lib
DIST=$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-05

cat > /tmp/test.def << 'DEF'
headers = stdio.h
DEF

$DIST/bin/cinterop -target ohos_arm64 \
  -def /tmp/test.def \
  -l $KLIB_DIR \
  -o /tmp/output 2>&1
```

### Expected Error
```
e: KLIB resolver: Could not find "org.jetbrains.kotlin.native.platform.ohos" in [/Users/.../.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-05/klib/platform/ohos_arm64, ...]
```

## Verification
Restoring the manifest to `depends=stdlib` allows cinterop to succeed:
```bash
sed -i.bak 's/^depends=stdlib org.jetbrains.kotlin.native.platform.ohos$/depends=stdlib/' \
  ../lib/build/classes/kotlin/ohosArm64/main/klib/lib/default/manifest
```

## Key Observations
1. The compiler uses `resolveManifestDependenciesLenient = true` and only logs a warning (not fatal)
2. cinterop uses strict resolution (`resolveManifestDependenciesLenient = false`) and fails
3. The aggregate platform library name `org.jetbrains.kotlin.native.platform.ohos` is listed in `KonanConfig.kt:164-174` for `emitStdlib` mode but doesn't exist as a resolvable klib
4. The Gradle plugin doesn't pass user klibs to cinterop tasks, so the error doesn't occur in typical Gradle builds
