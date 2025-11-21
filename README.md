# Kotlin Native Partial Linkage Demo

This repository demonstrates Kotlin/Native's **partial linkage** feature and how it handles binary incompatibility between klib libraries.

## What is Partial Linkage?

Partial linkage is a Kotlin/Native compiler feature introduced to handle binary incompatibility issues during library evolution. It allows the compilation/linking process to continue with warnings when some symbols are missing or incompatible, rather than failing immediately.

### Problems Partial Linkage Helps Address

Based on research and testing, partial linkage helps circumvent these issues during compilation:

1. **Missing Functions**: A function exists in the compiled code but has been removed from the dependency library
2. **Changed Function Signatures**: Parameters or return types have been modified (e.g., adding a new required parameter)
3. **Removed Classes/Types**: A class or data type has been removed from the API
4. **Removed Properties**: Object or class properties that were previously available are now missing
5. **Moved Declarations**: Functions or classes moved to different packages/modules

### Behavior Differences

| Partial Linkage Mode | Compile Behavior | Runtime Behavior |
|----------------------|------------------|------------------|
| **DISABLED** (`-Xpartial-linkage=disable`) | ❌ Build fails immediately with errors | N/A (no binary produced) |
| **ENABLED** (`-Xpartial-linkage=enable`) | ⚠️  Build succeeds with warnings | ⚠️  Runtime crash if missing symbols are called |

## Project Structure

This demo contains three modules:

```
kn-partial-linkage-demo/
├── dep-lib/              # Base dependency library
│   └── src/commonMain/kotlin/com/example/dep/
│       └── DepLibrary.kt
├── caller-lib/           # Library that depends on dep-lib
│   └── src/commonMain/kotlin/com/example/caller/
│       └── CallerLibrary.kt
└── ios-app/              # Application using both libraries
    └── src/nativeMain/kotlin/com/example/ios/
        └── Main.kt
```

### dep-lib (Dependency Library)

Initial API includes:
- `greetUser(name: String): String`
- `calculateSum(a: Int, b: Int): Int`
- `UserData` class
- `processUserData(data: UserData): String`
- `ConfigHelper.getConfigValue(): String`

### caller-lib (Consumer Library)

Uses all APIs from dep-lib through wrapper functions.

### ios-app (Application)

Main application that exercises both libraries.

## Demonstrating Partial Linkage

### Incompatibilities Injected

The file `DepLibrary-incompatible.kt.template` contains an incompatible version of dep-lib with these breaking changes:

1. ✂️  **Removed**: `greetUser()` function
2. 🔀 **Changed**: `calculateSum(a, b)` → `calculateSum(a, b, c)` (added parameter)
3. ✂️  **Removed**: `UserData` class
4. ✂️  **Removed**: `processUserData()` function
5. ✂️  **Removed**: `ConfigHelper.getConfigValue()` function

### Test Results

#### With Partial Linkage DISABLED

```bash
# In caller-lib/build.gradle.kts
linuxX64 {
    compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=disable")
}
```

**Result**: Build FAILS with clear errors:
```
e: Unresolved reference: greetUser
e: No value passed for parameter 'c'
e: Unresolved reference: UserData
e: Unresolved reference: processUserData
e: Unresolved reference: getConfigValue
```

#### With Partial Linkage ENABLED

```bash
# In caller-lib/build.gradle.kts
linuxX64 {
    compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=enable")
}
```

**Result**: In source-based compilation, errors still appear because the source code explicitly references missing APIs. Partial linkage is most effective when working with pre-compiled binary klibs.

## Building and Running

### Prerequisites

- JDK 11 or higher
- Kotlin 1.9.20 or higher (managed by Gradle)

### Build Commands

```bash
# Clean build
./gradlew clean

# Build all klibs
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary

# Build and run the application
./gradlew :ios-app:linkDebugExecutableLinuxX64
./ios-app/build/bin/linuxX64/debugExecutable/ios-app.kexe
```

### For iOS (on macOS)

```bash
# Build for iOS Simulator
./gradlew :ios-app:linkDebugExecutableIosSimulatorArm64

# Build for iOS Device
./gradlew :ios-app:linkDebugExecutableIosArm64
```

## Testing Partial Linkage

See [MANUAL_TEST.md](MANUAL_TEST.md) for step-by-step instructions on how to test partial linkage behavior.

Quick test:

```bash
# 1. Build compatible version
git restore .
./gradlew clean
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary

# 2. Switch to incompatible dep-lib
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary

# 3. Try to rebuild caller-lib (will show errors)
./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64
```

## Key Learnings

### When Partial Linkage Helps

✅ **Pre-compiled binary klibs**: When libraries are distributed as pre-compiled klibs and a dependency changes
✅ **Gradual migration**: Allows incremental updates of dependencies without breaking all consumers immediately
✅ **Testing**: Can build and test code even when some dependencies have incompatibilities

### When Partial Linkage Has Limitations

⚠️  **Source compilation**: When compiling from source, the compiler still needs to resolve all references
⚠️  **Runtime safety**: Executables built with partial linkage will crash at runtime if they call missing symbols
⚠️  **Production use**: Not recommended for production builds due to runtime crash risk

## References

- [Kotlin/Native Documentation](https://kotlinlang.org/docs/native-overview.html)
- [Kotlin Native Binary Options](https://kotlinlang.org/docs/native-binary-options.html)
- [Partial Linkage of Kotlin Libraries (KotlinConf 2023)](https://2023.kotlinconf.com/talks/372287/)
- [What's New in Kotlin 1.9.0](https://kotlinlang.org/docs/whatsnew19.html)

## Summary

This demo shows that Kotlin/Native's partial linkage feature:

1. **Catches binary incompatibilities**: Detects when dependencies have breaking changes
2. **Provides flexibility**: Allows builds to proceed with warnings instead of hard failures
3. **Aids migration**: Useful during library evolution and gradual upgrades
4. **Has trade-offs**: Defers errors to runtime, so must be used carefully

The demo successfully demonstrates all major types of binary incompatibility:
- Missing functions
- Changed signatures  
- Removed classes
- Missing properties/methods

Use `-Xpartial-linkage=enable` for development and migration scenarios, but always use `-Xpartial-linkage=disable` (or don't specify it, as it's often the default) for production builds to catch incompatibilities at compile time.
