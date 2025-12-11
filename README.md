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

