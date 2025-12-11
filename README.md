# Kotlin Native Compile Checks demo

This repo demonstrates various Kotlin/Native's **compile checks**, specifically how they handles binary incompatibility between klibs.

## Project Structure

This demo contains three modules:

```
kn-sample/
├── dep-lib/              # depended library
│   └── src/commonMain/kotlin/com/example/dep/
│       └── DepLibrary.kt # 2 versions, one with all the definitions, one breaking compatability
├── caller-lib/           # library that depends on dep-lib
│   └── src/commonMain/kotlin/com/example/caller/
│       └── CallerLibrary.kt
└── app/                 # application depending on caller-lib
    └── src/nativeMain/kotlin/com/example/macos/
        └── Main.kt
```

## Building and Running

Investigate [./run.sh](./run.sh) for detailed running process.

In essence

1. build compatable dep and caller klib with no issue
2. introduce a bunch of breaking changes in dep.klib, rebuild only dep.klib
3. build app without any pl setting to test the default behavior, build succes
4. build app with pl disabled, build fails
5. build app with pl enabled (default behavior), observe a bunch of warnings and app crashes during runtime

## Partial Linkage

Partial linkage is a Kotlin/Native compiler feature introduced to handle binary incompatibility issues during library evolution. It allows the compilation/linking process to continue with warnings when some symbols are missing or incompatible, rather than failing immediately. When partial linkage is enabled (by default it's enabled), the missing symbols would cause a log (default to warning level) during build, and cause a crash during runtime, instead of causing build failure.

- [Partial Linkage of Kotlin Libraries (KotlinConf 2023)](https://2023.kotlinconf.com/talks/372287/)
- [Kotlin 1.9.0 Release Notes](https://kotlinlang.org/docs/whatsnew19.html#library-linkage-in-kotlin-native)

### Problems Partial Linkage Helps Address

Partial linkage helps in circumventing a bunch of missing stuff issue during compilation:

1. **Missing Functions**: A function exists in the compiled code but has been removed from the dependency library
2. **Changed Function Signatures**: Parameters or return types have been modified (e.g., adding a new required parameter)
3. **Removed Classes/Types**: A class or data type has been removed from the API
4. **Removed Properties**: Object or class properties that were previously available are now missing
5. **Moved Declarations**: Functions or classes moved to different packages/modules
6. **Vtable Issue**: Abstract method isn't implemented in non-abstrct class

### Behavior Differences

| Partial Linkage Mode | Compile Behavior | Runtime Behavior |
|----------------------|------------------|------------------|
| **DISABLED** (`-Xpartial-linkage=disable`) | ❌ Build fails immediately with errors | N/A (no binary produced) |
| **ENABLED** (`-Xpartial-linkage=enable`) | ⚠️  Build succeeds with warnings | ⚠️  Runtime crash if missing symbols are called |


### Key Learnings

When Partial Linkage Helps:

✅ **Pre-compiled binary klibs**: When libraries are distributed as pre-compiled klibs and a dependency changes
✅ **Gradual migration**: Allows incremental updates of dependencies without breaking all consumers immediately
✅ **Testing**: Can build and test code even when some dependencies have incompatibilities

When Partial Linkage Has Limitations:

⚠️  **Source compilation**: When compiling from source, the compiler still needs to resolve all references. Pl can't circumvent anything in this case.

⚠️  **Runtime safety**: Executables built with partial  linkage will crash at runtime if they call the missing symbols

⚠️  **Production use**: Not recommended for production builds due to runtime crash risk

### Summary

Use `-Xpartial-linkage=enable` (deafult behavior) for development and migration scenarios, but always use `-Xpartial-linkage=disable` for production builds to catch incompatibilities at compile time.


## Result

```shell
./gradlew :app:linkPlCheckDebugExecutableMacosArm64

> Task :app:linkPlCheckDebugExecutableMacosArm64 FAILED
konanc -g -enable-assertions -Xinclude=/Volumes/disk/git/sample/kn-sample/app/build/classes/kotlin/macosArm64/main/klib/app.klib -library /Volumes/disk/git/kmp/baseline-20/kotlin-native/dist/klib/common/stdlib -library /Volumes/disk/git/sample/kn-sample/src-lib/build/classes/kotlin/macosArm64/main/klib/src-lib.klib -library /Volumes/disk/cache/gradle/caches/modules-2/files-2.1/com.example/caller-lib-macosarm64/1.0.0/575abdab32b30a73a07c10d77252dffd9ab10371/caller-lib.klib -library /Volumes/disk/cache/gradle/caches/modules-2/files-2.1/com.example/dep-lib-macosarm64/1.0.0/709f247177dd2c4c8a82f737c1d44e3a863eac7/dep-lib.klib -entry com.example.main -no-endorsed-libs -nostdlib -output /Volumes/disk/git/sample/kn-sample/app/build/bin/macosArm64/plCheckDebugExecutable/plCheck.kexe -produce program -target macos_arm64 -Xmulti-platform -produce header_cache -Xpartial-linkage=enable -Xpartial-linkage-loglevel=error -Xexternal-dependencies=/var/folders/5b/1lh1ghnd0lv25cddds31rzzc0000gn/T/kotlin-native-external-dependencies18333111502578060704.deps
w: Argument -produce is passed multiple times. Only the last value will be used: header_cache
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:39:1: Abstract function 'method2' is not implemented in non-abstract class 'Implementation'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:33:1: Abstract function 'method2' is not implemented in non-abstract class 'NonAbstract'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:13:16: Function 'greetUser' can not be called: No function found for symbol 'com.example.dep/greetUser|greetUser(kotlin.String){}[0]'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:18:16: Function 'calculateSum' can not be called: No function found for symbol 'com.example.dep/calculateSum|calculateSum(kotlin.Int;kotlin.Int){}[0]'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:24:32: Can not read value from variable 'userData': Variable uses unlinked class symbol 'com.example.dep/UserData|null[0]'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:24:16: Function 'processUserData' can not be called: No function found for symbol 'com.example.dep/processUserData|processUserData(com.example.dep.UserData){}[0]'
e: <com.example:caller-lib> @ /Volumes/disk/git/sample/kn-sample/caller-lib/src/commonMain/kotlin/com/example/caller/CallerLibrary.kt:29:78: Function 'getConfigValue' can not be called: No function found for symbol 'com.example.dep/ConfigHelper.getConfigValue|getConfigValue(){}[0]'
e: <missing declarations>: No class found for symbol 'com.example.dep/UserData|null[0]'
e: <missing declarations>: No constructor found for symbol 'com.example.dep/UserData.<init>|<init>(kotlin.String;kotlin.Int){}[0]'
e: <missing declarations>: No function found for symbol 'com.example.dep/greetUser|greetUser(kotlin.String){}[0]'
e: <missing declarations>: No function found for symbol 'com.example.dep/calculateSum|calculateSum(kotlin.Int;kotlin.Int){}[0]'
e: <missing declarations>: No function found for symbol 'com.example.dep/processUserData|processUserData(com.example.dep.UserData){}[0]'
e: <missing declarations>: No function found for symbol 'com.example.dep/ConfigHelper.getConfigValue|getConfigValue(){}[0]'
e: There are linkage errors reported by the partial linkage engine

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:linkPlCheckDebugExecutableMacosArm64'.
> Compilation finished with errors

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1s
4 actionable tasks: 4 executed
➜  kn-sample git:(pl) ✗ 
```