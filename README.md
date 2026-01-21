# Kotlin Native Metadata Klib Demo

A minimal example demonstrating how metadata klib is used during compilation in a multi-project Kotlin Multiplatform setup.

## Project Structure

```
.
├── lib/                        # Library module (published to ./repo/)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/example/lib/Lib.kt
├── app/                        # App module (depends on published lib)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/example/app/App.kt
├── c-caller/                   # C code that calls Kotlin shared library
│   └── main.c
├── repo/                       # Local maven repository (gitignored)
├── build.gradle.kts            # Root build config
└── settings.gradle.kts         # Project settings
```

## How Metadata Klib Works

### 1. Publishing lib

When `lib` is published:

```bash
./gradlew :lib:publishAllPublicationsToLocalRepository
```

**Three types of artifacts are created in `./repo/`:**

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

Normal build chain for producing binaries:

```bash
./gradlew :app:linkAppDebugSharedOhosArm64 --dry-run
# :app:compileKotlinOhosArm64
# :app:linkAppDebugSharedOhosArm64
```

**The metadata klib is NOT used in this chain.** It uses only the platform klib:

```
compileKotlinOhosArm64 → uses lib-ohosarm64-1.0.0.klib (platform klib)
linkAppDebugSharedOhosArm64 → uses lib-ohosarm64-1.0.0.klib (platform klib)
```

### 4. When Metadata Klib IS Used

The metadata klib is used in these scenarios:

| When | Task/Tool | What Uses Metadata Klib |
|------|-----------|------------------------|
| **IDE linting** | IntelliJ/Android Studio | Continuous - as you type in commonMain |
| **Explicit metadata compile** | `compileCommonMainKotlinMetadata` | Only when manually run |
| **Publishing** | `allMetadataJar` | When publishing library |

**Key insight:** The `compileCommonMainKotlinMetadata` task is **NOT** part of the normal build chain. It's a separate task for:
- IDE code completion and navigation
- Error highlighting in commonMain code
- Validating API usage without compiling all platforms

### 5. Build Chain Comparison

**Normal build (produces binary):**
```
Source → compileKotlinOhosArm64 → linkAppDebugSharedOhosArm64 → libapp.so
              ↓                           ↓
         platform klib              platform klib
```

**Metadata compilation (for IDE/tooling):**
```
Source → compileCommonMainKotlinMetadata → metadata klib
              ↓
         metadata klib from dependencies
```

## Run Demo

```bash
./run.sh
```

This script:
1. Publishes lib to `./repo/`
2. Compiles app commonMain (shows metadata klib usage)
3. Builds app shared library for ohosArm64 (uses platform klib)
4. Builds c-caller
5. Deploys and runs on OHOS device

## Inspecting Artifacts

### View metadata klib declarations
```bash
unzip -p repo/com/example/lib/1.0.0/lib-1.0.0.jar commonMain/default/manifest
```

### View project structure metadata
```bash
unzip -p repo/com/example/lib/1.0.0/lib-1.0.0.jar META-INF/kotlin-project-structure-metadata.json
```

### Use klib tool to dump metadata
```bash
~/.konan/kotlin-native-*/bin/klib dump-metadata <path-to-klib>
```

## Key Tasks

| Task | Description |
|------|-------------|
| `compileCommonMainKotlinMetadata` | Compiles commonMain to metadata klib (for IDE/tooling) |
| `compileKotlinOhosArm64` | Compiles to platform klib for ohosArm64 |
| `linkAppDebugSharedOhosArm64` | Links shared library for ohosArm64 |
| `allMetadataJar` | Creates JAR with all metadata klibs |
| `publishAllPublicationsToLocalRepository` | Publishes all artifacts to ./repo/ |
