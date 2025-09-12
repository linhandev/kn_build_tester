# Kotlin Multiplatform Multi-Module Demo

This project demonstrates how to create a Kotlin Multiplatform application with multiple modules where:
- **mathlib**: Compiled as a **static library** (.a files) for native targets and JAR for JVM
- **stringlib**: Compiled as a **dynamic library** (.dylib/.so files) for native targets and JAR for JVM
- **app**: Application that uses both libraries

## Project Structure

```
kn-sample/
├── README.md
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project modules declaration
├── gradle.properties             # Gradle configuration (includes heap space settings)
├── run.sh                        # Demo script
├── mathlib/                      # Static library module
│   ├── build.gradle.kts         # Static library build configuration
│   └── src/
│       ├── commonMain/kotlin/   # 🌍 Shared code: MathUtils.kt, PlatformExpected.kt
│       ├── commonNative/kotlin/ # 🖥️ Native code: Platform.kt
│       └── jvmMain/kotlin/      # ☕ JVM code: Platform.kt
├── stringlib/                   # Dynamic library module
│   ├── build.gradle.kts         # Dynamic library build configuration  
│   └── src/
│       ├── commonMain/kotlin/   # 🌍 Shared code: StringUtils.kt, StringPlatformExpected.kt
│       ├── commonNative/kotlin/ # 🖥️ Native code: StringPlatform.kt
│       └── jvmMain/kotlin/      # ☕ JVM code: StringPlatform.kt
└── app/                         # Application module
    ├── build.gradle.kts         # App build configuration
    └── src/
        └── commonMain/kotlin/   # 🌍 Main.kt (uses both libraries)
            └── Main.kt          
```

## Modules

### 1. mathlib (Static Library Module)
- **Purpose**: Simple math function to demonstrate static library linking
- **Targets**: JVM, Linux x64, macOS x64, macOS ARM64
- **Outputs**: 
  - **JVM**: `mathlib-jvm.jar`
  - **Native**: `libmathlib.a` (static libraries)
- **Function**: `mathLibFunction(x: Int, y: Int): Int` - adds two numbers

### 2. stringlib (Dynamic Library Module)  
- **Purpose**: Simple string function to demonstrate dynamic library linking
- **Targets**: JVM, Linux x64, macOS x64, macOS ARM64
- **Outputs**: 
  - **JVM**: `stringlib-jvm.jar`
  - **Native**: `libstringlib.dylib/.so` (dynamic/shared libraries)
- **Function**: `stringLibFunction(text: String): String` - processes text

### 3. app (Application Module)
- **Purpose**: Demonstrates usage of both static and dynamic libraries
- **Dependencies**: Links against both mathlib (static) and stringlib (dynamic)
- **Output**: Calls functions from both libraries and shows platform information

## How App Depends on Libraries

The dependency configuration demonstrates Kotlin Multiplatform's automatic cross-module linking:

### In `app/build.gradle.kts`:
```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation(project(":mathlib"))    // Static library
            implementation(project(":stringlib"))  // Dynamic library
        }
    }
}
```

### What Happens Automatically:
1. **JVM→JVM**: App's JVM target depends on both libraries' JARs
2. **Native→Native**: App's native targets link against libraries (static + dynamic)
3. **Source Resolution**: Each platform gets the right source code via hierarchy

## Library Types Demonstrated

### Static Library (mathlib)
- **Build Config**: `staticLib()` in binaries block
- **Native Output**: `libmathlib.a` files
- **Linking**: Embedded into final executable at compile time
- **JVM Output**: Regular JAR file

### Dynamic Library (stringlib)  
- **Build Config**: `sharedLib()` in binaries block
- **Native Output**: `libstringlib.dylib` (macOS) / `libstringlib.so` (Linux)
- **Linking**: Loaded at runtime, must be available in library path
- **JVM Output**: Regular JAR file

## Technologies Used

- **Kotlin**: 2.2.0
- **Gradle**: 9.0.0 
- **Target Platforms**: JVM, Linux x64, macOS x64, macOS ARM64
- **Heap Configuration**: 4GB max heap for Kotlin/Native compilation

## Compilation Process

### 1. Static Library Compilation (mathlib)

```bash
./gradlew :mathlib:linkReleaseStaticNative
```

**Output**: `mathlib/build/bin/native/releaseStatic/libmathlib.a`

### 2. Dynamic Library Compilation (stringlib)

```bash  
./gradlew :stringlib:linkReleaseSharedNative
```

**Output**: `stringlib/build/bin/native/releaseShared/libstringlib.dylib`

### 3. Application Compilation (app)

```bash
./gradlew :app:linkReleaseExecutableNative  
```

Links against both static and dynamic libraries.
**Output**: `app/build/bin/native/releaseExecutable/app.kexe`

Builds both modules in the correct dependency order.

## How to Build and Run

### Prerequisites

- Java 8 or higher
- Gradle (or use the included wrapper)
- Linux environment (for running the executable)

### Build Steps

1. **Clone and navigate to the project**:
   ```bash
   git clone <repository-url>
   cd kn-sample
   ```

2. **Build the entire project** (all targets):
   ```bash
   ./gradlew build
   ```

## Quick Start

### Using the Demo Script
```bash
# Run both native and JVM versions automatically  
bash run.sh
```

### Manual Build and Run

1. **Build everything**:
   ```bash
   ./gradlew build
   ```

2. **Run Native** (auto-detects platform):
   ```bash
   # macOS ARM64
   ./app/build/bin/macosArm/releaseExecutable/app.kexe
   
   # macOS x64  
   ./app/build/bin/macos/releaseExecutable/app.kexe
   
   # Linux x64
   ./app/build/bin/native/releaseExecutable/app.kexe
   ```

3. **Run JVM**:
   ```bash
   java -cp "app/build/classes/kotlin/jvm/main:mathlib/build/libs/mathlib-jvm.jar:stringlib/build/libs/stringlib-jvm.jar:$KOTLIN_STDLIB" MainKt
   ```

## Expected Output

When running the application, you should see:

```
=== Kotlin Multi-Module Demo ===
Running on: [JVM (Java 21.0.8) | Native (Linux/macOS)]
String platform: [JVM (Dynamic JAR) | Native (Dynamic Library)]
Called mathLibAddFunction from static library
Result from static library: 8
Called stringLibFunction from dynamic library  
Result from dynamic library: Processed: Hello from app!
=== Demo completed ===
```

The output shows:
- **Platform detection** from both libraries
- **Static library call** (mathlib) - embedded in executable
- **Dynamic library call** (stringlib) - loaded at runtime
- **Cross-module dependencies** working correctly

## Key Learning Points

### 1. Static vs Dynamic Libraries
- **Static Library (mathlib)**: Code embedded in final executable, no runtime dependencies
- **Dynamic Library (stringlib)**: Loaded at runtime, smaller executable but needs library files

### 2. Automatic Dependency Resolution
- Gradle handles build order automatically based on dependencies
- Both libraries built before application that depends on them
- JVM gets JARs, Native gets appropriate library files

### 3. Multiplatform Module Architecture
- Single codebase supports JVM + multiple native platforms
- `expect`/`actual` pattern for platform-specific code
- Source set hierarchy shares code efficiently

### 4. Build Configuration Benefits
- Heap space configuration prevents out-of-memory errors
- Configuration cache speeds up repeated builds
- Parallel builds improve performance

## Build Artifacts

After a successful build, you'll find:

**mathlib (Static Library):**
- **JVM**: `mathlib/build/libs/mathlib-jvm.jar`
- **Native**: `mathlib/build/bin/{platform}/releaseStatic/libmathlib.a`

**stringlib (Dynamic Library):**
- **JVM**: `stringlib/build/libs/stringlib-jvm.jar`  
- **Native**: `stringlib/build/bin/{platform}/releaseShared/libstringlib.{dylib|so}`

**app (Application):**
- **JVM**: `app/build/classes/kotlin/jvm/main/` (class files)
- **Native**: `app/build/bin/{platform}/releaseExecutable/app.kexe`

Where `{platform}` is: `native` (Linux x64), `macos` (macOS x64), or `macosArm` (macOS ARM64).

## Extending to Other Targets

To add support for other platforms, modify the build scripts:

```kotlin
kotlin {
    linuxX64("linux")
    macosX64("macos") 
    macosArm64("macosArm")
    mingwX64("windows")
}
```

This demonstrates the power of Kotlin Multiplatform for creating modular native applications with both static and dynamic library linking.
