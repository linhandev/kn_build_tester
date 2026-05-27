# C Export Demo for OHOS Target

This project demonstrates how to export Kotlin/Native symbols for C to call when targeting OHOS. It covers three scenarios and documents the actual behavior of `@CName` annotations.

## Key Finding

**`@CName` annotations only work in the module that produces the binary (sharedLib/staticLib).** Functions annotated with `@CName` in dependency modules (subprojects or klibs) are NOT exported in the generated header or binary.

## Project Structure

```
cexport-demo/
├── app/                         # Main module producing binary (Scenario 1)
│   └── src/ohosArm64Main/kotlin/AppExports.kt
├── lib-subproject/              # Dependency subproject (Scenario 2)
│   └── src/ohosArm64Main/kotlin/SubprojectExports.kt
└── lib-klib/                    # Klib dependency (Scenario 3)
    └── src/ohosArm64Main/kotlin/KlibExports.kt
```

## Test Results

### Scenario 1: Top-level Gradle Subproject ✅ WORKS
- **What**: Functions with `@CName` in the main module that produces the binary
- **Result**: Functions are exported in both header and binary
- **Example**: `cexport_app_hello()`, `cexport_app_add()`

### Scenario 2: Submodule Dependency ❌ DOES NOT WORK
- **What**: Functions with `@CName` in a dependency subproject
- **Result**: Functions are NOT exported (not in header, not in binary as C symbols)
- **Workaround**: Create bridge functions in the main module (see below)

### Scenario 3: Klib Dependency ❌ DOES NOT WORK
- **What**: Functions with `@CName` in a klib dependency
- **Result**: Functions are NOT exported (not in header, not in binary as C symbols)
- **Workaround**: Create bridge functions in the main module (see below)

## Evidence

### Generated Header (`libcexport_api.h`)
```c
// Only exports from app module (Scenario 1) are present:
extern libcexport_KInt cexport_app_add(libcexport_KInt a, libcexport_KInt b);
extern const char* cexport_app_hello();

// Bridge functions (workaround for Scenarios 2 & 3):
extern const char* cexport_bridge_concat(const char* a, const char* b);
extern const char* cexport_bridge_greet(const char* name);
extern libcexport_KInt cexport_bridge_multiply(libcexport_KInt a, libcexport_KInt b);
extern const char* cexport_bridge_version();

// NOT present (even though they have @CName in dependency modules):
// - cexport_subproject_greet()
// - cexport_subproject_multiply()
// - cexport_klib_version()
// - cexport_klib_concat()
```

### Binary Symbols
```bash
$ nm libcexport.so | grep " T cexport"
00000000000f6eb0 T cexport_app_add
00000000000f6fc4 T cexport_app_hello
00000000000f7110 T cexport_bridge_concat
00000000000f731c T cexport_bridge_greet
00000000000f74c8 T cexport_bridge_multiply
00000000000f75dc T cexport_bridge_version
```

Note: The dependency module functions DO exist in the binary with their Kotlin names (as local symbols), but NOT with their `@CName` C names:
```bash
$ nm libcexport.so | grep "kfun.*cexport"
00000000000a1a04 t kfun:com.example.cexport.klib#klibConcat(...)
00000000000a1994 t kfun:com.example.cexport.klib#klibVersion(...)
00000000000a17e4 t kfun:com.example.cexport.subproject#subprojectGreet(...)
00000000000a1914 t kfun:com.example.cexport.subproject#subprojectMultiply(...)
```

## Building

### Prerequisites
- CPF Kotlin/Native compiler with OHOS support (version 2.2.21-0.3.0-04)
- OHOS SDK sysroot (auto-downloaded by Kotlin/Native)
- Gradle 8.9

### Build Commands

```bash
# Build shared library (produces libcexport.so + libcexport_api.h)
./gradlew :app:linkDebugSharedOhosArm64

# Build static library (produces libcexport.a + libcexport_api.h)
./gradlew :app:linkDebugStaticOhosArm64

# Build both
./gradlew :app:linkDebugOhosArm64
```

## Recommended Approach: Bridge Functions

Since `@CName` only works in the binary-producing module, use bridge/wrapper functions:

### In dependency module (`lib-subproject/.../SubprojectExports.kt`):
```kotlin
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package com.example.cexport.subproject

import kotlin.native.CName

// This @CName will NOT be exported
@CName("cexport_subproject_greet")
fun subprojectGreet(name: String): String = "Hello, $name!"
```

### In main module (`app/.../AppExports.kt`):
```kotlin
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package com.example.cexport.app

import kotlin.native.CName
import com.example.cexport.subproject.subprojectGreet

// Bridge function - this @CName WILL be exported
@CName("cexport_bridge_greet")
fun bridgeGreet(name: String): String = subprojectGreet(name)
```

## Configuration

### Main Module (`app/build.gradle.kts`)
```kotlin
kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "cexport"
            }
            staticLib {
                baseName = "cexport"
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                implementation(project(":lib-subproject"))
                implementation(project(":lib-klib"))
            }
        }
    }
}
```

### Dependency Modules
Dependency modules don't need special binary configuration:
```kotlin
kotlin {
    ohosArm64()
}
```

## Using from C/C++

```c
#include "libcexport_api.h"

int main() {
    // Direct exports from app module
    const char* msg = cexport_app_hello();
    int sum = cexport_app_add(2, 3);
    
    // Bridge functions calling into dependency modules
    const char* greet = cexport_bridge_greet("World");
    int product = cexport_bridge_multiply(4, 5);
    const char* ver = cexport_bridge_version();
    const char* concat = cexport_bridge_concat("a", "b");
    
    return 0;
}
```

## Technical Details

### Why `@CName` Doesn't Work in Dependencies

The Kotlin/Native compiler processes `@CName` annotations during the link step when producing the final binary. It only scans the source code of the binary-producing module, not the compiled klibs from dependencies.

The dependency module code IS linked into the final binary (you can see the `kfun:*` symbols), but the C adapter generation (`CAdapterGenerator`) only creates C bindings for `@CName` functions in the top-level module.

### Dead-Code Elimination

If dependency module functions are not referenced from the binary-producing module, they will be dead-code eliminated. The bridge functions ensure the dependency code is retained.

### Annotation Location

`@CName` is in the `kotlin.native` package and requires:
```kotlin
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
import kotlin.native.CName
```

## Summary

| Scenario | `@CName` in Dependency | Exported? | Workaround |
|----------|------------------------|-----------|------------|
| Top-level module | N/A (is the binary module) | ✅ Yes | None needed |
| Subproject dependency | Yes | ❌ No | Bridge function in main module |
| Klib dependency | Yes | ❌ No | Bridge function in main module |

**Recommendation**: Place all `@CName` exports in the module that produces the binary. Use bridge functions to expose functionality from dependency modules.
