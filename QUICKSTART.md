# Quick Start Guide

This is a quick reference for running the Kotlin Native Partial Linkage demo.

## Prerequisites

- JDK 11+ (Gradle will download Kotlin automatically)
- On macOS: Can build iOS targets
- On Linux: Can build Linux targets (used for testing)

## 1. Run the Demo (Easiest)

```bash
./RUN_DEMO.sh
```

This automated script will:
1. ✅ Build compatible versions of all libraries
2. ✅ Run the working application
3. ✅ Introduce 5 types of binary incompatibilities
4. ✅ Test with partial linkage DISABLED (shows errors)
5. ✅ Test with partial linkage ENABLED (shows behavior)
6. ✅ Restore project to working state

**Duration**: ~2-3 minutes

## 2. Build and Run Manually

```bash
# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Run the application
./ios-app/build/bin/linuxX64/debugExecutable/ios-app.kexe
```

**Expected output:**
```
=== Kotlin Native Partial Linkage Demo ===
Test 1: Welcome User
  Result: Hello, Alice!
...
=== All tests completed successfully! ===
```

## 3. Test Partial Linkage Manually

See [MANUAL_TEST.md](MANUAL_TEST.md) for detailed step-by-step instructions.

Quick steps:
```bash
# 1. Build compatible version
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary

# 2. Switch to incompatible dep-lib
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary

# 3. Try to rebuild caller-lib (will show errors)
./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64
```

## 4. For iOS Targets

On a macOS machine:

```bash
# Build for iOS Simulator (Apple Silicon)
./gradlew :ios-app:linkDebugExecutableIosSimulatorArm64

# Build for iOS Device (Apple Silicon)
./gradlew :ios-app:linkDebugExecutableIosArm64
```

## What to Expect

### Working Version

✅ All tests pass
✅ Application runs successfully
✅ Clean build output

### After Introducing Incompatibilities

❌ **With `-Xpartial-linkage=disable`**: Build fails with clear errors
⚠️  **With `-Xpartial-linkage=enable`**: Errors still appear in source compilation

## Incompatibilities Demonstrated

1. **Removed Function**: `greetUser()` deleted from API
2. **Changed Signature**: `calculateSum(a, b)` becomes `calculateSum(a, b, c)`
3. **Removed Class**: `UserData` class deleted
4. **Removed Function**: `processUserData()` deleted
5. **Removed Method**: `ConfigHelper.getConfigValue()` deleted

## Error Examples

When incompatibilities are introduced:

```
e: Unresolved reference: greetUser
e: No value passed for parameter 'c'
e: Unresolved reference: UserData
e: Unresolved reference: processUserData
e: Unresolved reference: getConfigValue
```

## Documentation

- **README.md** - Comprehensive guide and background
- **MANUAL_TEST.md** - Step-by-step testing procedures
- **RESULTS.md** - Actual test results and analysis
- **QUICKSTART.md** - This file!

## Troubleshooting

### "Kotlin/Native targets cannot be built"

This is expected on Linux machines. The demo uses `linuxX64` target for testing on Linux, which demonstrates the same principles as iOS.

### Build failures

If you get unexpected build failures:

```bash
# Reset to clean state
git restore .
./gradlew clean
./gradlew build
```

### Gradle issues

```bash
# Refresh Gradle
./gradlew --refresh-dependencies
./gradlew clean build
```

## Next Steps

1. Read [README.md](README.md) for detailed explanation
2. Review [RESULTS.md](RESULTS.md) for test outcomes
3. Experiment with your own incompatibilities
4. Try on iOS targets if on macOS

## Key Takeaways

✅ Partial linkage detects binary incompatibilities
✅ Provides clear error messages for debugging
✅ Most effective with pre-compiled binary klibs
✅ Useful for API evolution and migration
✅ Should use `-Xpartial-linkage=disable` for production
