# Kotlin Native Metadata Klib Demo

A minimal example demonstrating how metadata klib is used during compilation in a multi-project Kotlin Multiplatform setup.

## Project Structure

```
.
├── lib/                        # Library module (published to mavenLocal)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/example/lib/Lib.kt
├── app/                        # App module (depends on published lib)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/example/app/App.kt
├── c-caller/                   # C code that calls Kotlin shared library
│   └── main.c
├── build.gradle.kts            # Root build config
└── settings.gradle.kts         # Project settings
```

## How Metadata Klib Works

### 1. Publishing lib

When `lib` is published to mavenLocal:

```bash
./gradlew :lib:publishToMavenLocal
```

**Three types of artifacts are created:**

| Artifact | Purpose |
|----------|---------|
| `lib-1.0.0.jar` | **Metadata JAR** containing the metadata klib for commonMain |
| `lib-ohosarm64-1.0.0.klib` | **Platform klib** for ohosArm64 (contains IR + platform code) |
| `lib-iosarm64-1.0.0.klib` | **Platform klib** for iosArm64 |

### 2. Metadata JAR Structure

```
lib-1.0.0.jar
├── META-INF/
│   └── kotlin-project-structure-metadata.json   # Source set hierarchy
└── commonMain/
    └── default/
        ├── manifest                              # Klib metadata
        ├── linkdata/
        │   ├── module                           # Module info
        │   └── package_com.example.lib/
        │       └── 0_lib.knm                    # Serialized declarations
        └── targets/                             # Target markers
```

The `.knm` file contains **serialized Kotlin declarations** (classes, functions, etc.) - NOT actual compiled code.

### 3. When app compiles

```bash
./gradlew :app:compileKotlinOhosArm64
```

Gradle does the following:

1. **Resolves dependency** `com.example:lib:1.0.0`
2. **For commonMain compilation**: Uses the **metadata klib** from `lib-1.0.0.jar` to understand lib's API
3. **For platform compilation**: Uses the **platform klib** `lib-ohosarm64-1.0.0.klib` which has actual IR

### 4. Why Metadata Klib Matters

| Stage | What's Used | Why |
|-------|-------------|-----|
| **IDE/Analysis** | Metadata klib | Fast API resolution without compiling all platforms |
| **commonMain compile** | Metadata klib | Validates API usage in common code |
| **Platform compile** | Platform klib | Links actual IR for final binary |
| **Final link** | Platform klib | Produces .so/.framework with real code |

## Build & Run

### 1. Publish lib
```bash
./gradlew :lib:publishToMavenLocal
```

### 2. Build app shared library for OHOS
```bash
./gradlew :app:linkAppDebugSharedOhosArm64
```

### 3. Build C caller
```bash
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang \
  --sysroot /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot \
  -target aarch64-linux-ohos \
  -I app/build/bin/ohosArm64/appDebugShared \
  c-caller/main.c \
  -L app/build/bin/ohosArm64/appDebugShared \
  -lapp \
  -o c-caller/main
```

### 4. Deploy and run on OHOS device
```bash
hdc file send c-caller/main /data/local/tmp/main
hdc file send app/build/bin/ohosArm64/appDebugShared/libapp.so /data/local/tmp/libapp.so
hdc shell chmod 777 /data/local/tmp/main
hdc shell "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/main"
```

Expected output:
```
C Caller: Starting Kotlin demo...

C Caller: Calling runDemo() directly:
Created user: User(name=Alice, age=30)
Greeting: Hello, Alice! You are 30 years old.
Lib info: lib v1.0.0
Calculator: 10 + 20 = 30, 5 * 6 = 30

C Caller: Demo finished.
```

## Inspecting Metadata

### View metadata klib declarations
```bash
# Extract and inspect the metadata
unzip -p ~/.m2/repository/com/example/lib/1.0.0/lib-1.0.0.jar commonMain/default/manifest
```

### View project structure metadata
```bash
unzip -p ~/.m2/repository/com/example/lib/1.0.0/lib-1.0.0.jar META-INF/kotlin-project-structure-metadata.json
```

### Use klib tool to dump metadata
```bash
# If you have kotlin-native installed
~/.konan/kotlin-native-*/bin/klib dump-metadata <path-to-klib>
```

## Key Tasks

| Task | Description |
|------|-------------|
| `compileCommonMainKotlinMetadata` | Compiles commonMain to metadata klib |
| `compileKotlinOhosArm64` | Compiles to platform klib for ohosArm64 |
| `linkAppDebugSharedOhosArm64` | Links shared library for ohosArm64 |
| `allMetadataJar` | Creates JAR with all metadata klibs |
| `publishToMavenLocal` | Publishes all artifacts to ~/.m2 |
