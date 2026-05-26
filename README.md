# ALI-38: Incremental Build Artifact Size Growth (OHOS ARM64)

## Issue

When `kotlin.incremental.native=true` is enabled, the OHOS ARM64 shared library (`libkn.so`) grows in size after incremental rebuilds. The control build (`kotlin.native.cacheKind.ohosArm64=none`) produces deterministic, non-growing artifacts.

## Environment

- **Kotlin**: `2.2.21-0.2.0-12` (CPF fork)
- **Gradle**: 8.14.3
- **Target**: ohosArm64 (shared library `libkn.so`)
- **Demo repo**: `https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git` branch `IR003-kmp-debug`

## Quick Start

```bash
chmod +x reproduce.sh
./reproduce.sh
```

The script clones the demo project, runs both experiments, and prints a comparison table.

## Manual Steps

### 1. Clone and build with incremental enabled

```bash
git clone --branch IR003-kmp-debug https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git
cd kmp-cmp-test-demo

# Clean build (gradle.properties already has kotlin.incremental.native=true)
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache
stat -f "%z" composeApp/build/bin/ohosArm64/debugShared/libkn.so
# => 255481768 bytes

# Make a small change
sed -i '' 's/(HarmonyOS Native)/(HarmonyOS Native v2)/' \
  composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt

# Incremental rebuild
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache
stat -f "%z" composeApp/build/bin/ohosArm64/debugShared/libkn.so
# => 255481784 bytes (+16 bytes)
```

### 2. Control build (cache disabled)

```bash
# Revert change
sed -i '' 's/(HarmonyOS Native v2)/(HarmonyOS Native)/' \
  composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt

# Clean build with cache disabled
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache \
  -Pkotlin.native.cacheKind.ohosArm64=none
stat -f "%z" composeApp/build/bin/ohosArm64/debugShared/libkn.so
# => 140007552 bytes

# Same change, rebuild
sed -i '' 's/(HarmonyOS Native)/(HarmonyOS Native v2)/' \
  composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt

./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache \
  -Pkotlin.native.cacheKind.ohosArm64=none
stat -f "%z" composeApp/build/bin/ohosArm64/debugShared/libkn.so
# => 140007552 bytes (0 bytes growth)
```

## Expected Output

| Configuration | Clean Build | After 1 Edit | Growth |
|---|---|---|---|
| `kotlin.incremental.native=true` | 255,481,768 B | 255,481,784 B | +16 B |
| `cacheKind.ohosArm64=none` | 140,007,552 B | 140,007,552 B | 0 B |

The incremental baseline is also **82% larger** than the control (255 MB vs 140 MB).
