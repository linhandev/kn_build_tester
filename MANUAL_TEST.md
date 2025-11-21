# Manual Partial Linkage Test

This document explains how to manually test Kotlin Native's partial linkage feature.

## What is Partial Linkage?

Partial linkage is a Kotlin/Native compiler feature that allows the linker to proceed with warnings when some symbols are missing from dependencies, rather than failing immediately. This is useful during library evolution and migration scenarios.

## Test Scenario

We have three modules:
- `dep-lib`: A library with an API
- `caller-lib`: A library that uses dep-lib's API  
- `ios-app`: An application that uses both libraries

## Incompatibilities Demonstrated

The incompatible version of `dep-lib` has these changes:
1. **Function REMOVED**: `greetUser(name: String)`
2. **Signature CHANGED**: `calculateSum(a, b)` → `calculateSum(a, b, c)`
3. **Class REMOVED**: `UserData`
4. **Function REMOVED**: `processUserData(data)`
5. **Function REMOVED**: `ConfigHelper.getConfigValue()`

## How to Test

### Step 1: Build Compatible Version

```bash
git restore .
./gradlew clean
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary
```

This builds both libraries with compatible APIs.

### Step 2: Switch to Incompatible dep-lib

```bash
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary
```

Now `dep-lib.klib` contains the incompatible version, but `caller-lib.klib` was compiled against the old version.

### Step 3: Test with Partial Linkage DISABLED

Modify `caller-lib/build.gradle.kts` to add:
```kotlin
linuxX64 {
    compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=disable")
}
```

Then try to recompile caller-lib:
```bash
./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64
```

**Result**: ❌ Build FAILS with errors:
```
e: Unresolved reference: greetUser
e: No value passed for parameter 'c'
e: Unresolved reference: UserData
e: Unresolved reference: processUserData
e: Unresolved reference: getConfigValue
```

### Step 4: Test with Partial Linkage ENABLED

Modify `caller-lib/build.gradle.kts` to add:
```kotlin
linuxX64 {
    compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=enable")
}
```

Then try to recompile caller-lib:
```bash
./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64
```

**Note**: In Kotlin 1.9.x, partial linkage still shows these as errors during source compilation because the source code explicitly references the missing APIs. Partial linkage is more effective when working with pre-compiled klibs where the source isn't recompiled.

## Observed Behavior

| Configuration | Build Result | Description |
|--------------|--------------|-------------|
| Partial Linkage DISABLED | ❌ FAILS | Immediate errors about missing/incompatible symbols |
| Partial Linkage ENABLED | ⚠️  May fail during source recompilation | Warnings when linking pre-compiled klibs |

## Key Insight

Partial linkage is most effective in scenarios where:
1. Libraries are distributed as pre-compiled klibs (not source)
2. A dependency klib is updated with breaking changes
3. The consuming klib isn't immediately recompiled from source

In typical Gradle builds where everything is compiled from source, you'll still see errors because the source code explicitly references the missing APIs. Partial linkage shines in binary distribution scenarios.

## For iOS Targets

The same principles apply to iOS targets (`iosArm64`, `iosSimulatorArm64`). On a macOS machine, you would use:
```bash
./gradlew :dep-lib:iosArm64MainKlibrary :caller-lib:iosArm64MainKlibrary
```

And configure partial linkage the same way in the build files.
