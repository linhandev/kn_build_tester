# ALI-52: splitBCfile=0 Behavior Test

Minimal KMP demo to test the `splitBCfile=0` compiler option on the latest CPF Kotlin version.

## Environment

- **Kotlin Version**: `2.2.21-0.3.0-06` (latest from `maven.eazytec-cloud.com`)
- **Targets**: OHOS arm64 (dynamic lib), iOS arm64 (framework)
- **Host**: macOS arm64

## What This Tests

The `splitBCfile` option controls how Kotlin/Native splits LLVM bitcode files during compilation:
- **Default (unset)**: Kotlin/Native uses its default splitting behavior
- **`splitBCfile=0`**: Explicitly disables bitcode splitting

This project compares both behaviors to verify whether `splitBCfile=0` causes any issues.

## Quick Start

```bash
# Run the full comparison test (default vs splitBCfile=0)
./scripts/test-splitBCfile0.sh

# Or build individual configurations manually:

# Default behavior (no splitBCfile option)
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon

# With splitBCfile=0
./gradlew clean :kotlinApp:linkDebugSharedOhosArm64 --no-daemon -PsplitBC=0

# iOS arm64 framework - default
./gradlew clean :kotlinApp:linkDebugFrameworkIosArm64 --no-daemon

# iOS arm64 framework - with splitBCfile=0
./gradlew clean :kotlinApp:linkDebugFrameworkIosArm64 --no-daemon -PsplitBC=0
```

## Expected Results

Both configurations should compile successfully. Any crash or error with `splitBCfile=0` would indicate a regression.

## Project Structure

```
├── settings.gradle.kts    # Plugin + dependency repos (CPF Maven)
├── gradle.properties      # kotlinVersion=2.2.21-0.3.0-06
├── kotlinApp/
│   ├── build.gradle.kts   # OHOS arm64 sharedLib + iOS arm64 framework
│   └── src/
│       ├── ohosArm64Main/kotlin/hello.kt
│       └── iosArm64Main/kotlin/hello.kt
└── scripts/
    └── test-splitBCfile0.sh  # Automated comparison test
```
