# Kotlin/Native Partial Linkage Demo Project

## Project Purpose
Demonstrates Kotlin/Native **partial linkage** behavior—how the compiler handles binary incompatibility between pre-compiled klibs. This is a testing/demonstration project, not production code.

## Architecture

Three-tier dependency chain designed to test ABI compatibility:
- **dep-lib** → dependency library (has 2 versions: compatible and broken)
- **caller-lib** → depends on dep-lib (compiled against compatible version, never recompiled)
- **app** → native executable/shared library depending on caller-lib

Key design: caller-lib is compiled once against dep-lib v1, then dep-lib is updated with breaking changes and published again. This simulates real-world binary incompatibility scenarios.

## Critical Workflow: The Test Dance

The [run.sh](run.sh) script orchestrates the demo workflow:

```bash
./run.sh  # Runs full demonstration sequence
```

**What it does:**
1. Publishes compatible dep-lib and caller-lib to mavenLocal
2. Swaps `DepLibrary.kt` ↔ `DepLibrary.broken` to introduce breaking changes
3. Republishes only dep-lib (caller-lib remains unchanged)
4. Tests app linking with different `-Xpartial-linkage` modes

**Never manually edit DepLibrary files**—use the swap mechanism in run.sh to switch between versions.

## Key Conventions

### File Swapping Pattern
- `DepLibrary.kt` = active version (always compilable)
- `DepLibrary.broken` = intentionally broken version with ABI incompatibilities
- run.sh manages swapping via `mv` commands to test different scenarios

### Partial Linkage Modes
Controlled via project property in [app/build.gradle.kts](app/build.gradle.kts):

```kotlin
-PpartialLinkMode=disable  # Fail fast on incompatibility
-PpartialLinkMode=enable   # Warn, allow runtime crashes
```

Default behavior (no flag) = partial linkage enabled with warnings.

### Breaking Changes in DepLibrary.broken
- **Removed class** `UserData` - caller-lib references this in `formatUser()`
- **Removed functions**: `greetUser`, `processUserData`, `ConfigHelper.getConfigValue` - caller-lib has calls to these
- **Changed function signatures**:
  - `calculateSum(a: Int, b: Int)` → `calculateSum(a: Int, b: Int, c: Int)` - added required parameter
  - `sendNotification(message: String)` → `sendNotification(message: String, priority: Int = 0)` - added parameter with default value
- **Added abstract methods** to interfaces/classes (`method2()`) - existing implementations become incomplete

**Key insight**: Adding parameters with default values breaks binary compatibility even though source code would compile. The pre-compiled caller-lib klib has no knowledge of the new signature.

## Build System

**Gradle modules**: Root project is `kn-partial-linkage-demo`
- Uses Kotlin 2.0.21-KBA-014 multiplatform plugin
- Targets macOS ARM64 only
- Custom Maven repos (Tencent mirrors in [settings.gradle.kts](settings.gradle.kts))
- Libraries publish to mavenLocal with group `com.example`, version `1.0.0`

**App module binaries** ([app/build.gradle.kts](app/build.gradle.kts)):
- `sharedLib` - dynamic library with configurable partial linkage
- `executable` - standalone binary with configurable partial linkage  
- `plCheck` - special build with `-Xpartial-linkage-loglevel=error` for testing

## Testing Binary Compatibility

**Gradle tasks:**
```bash
./gradlew :dep-lib:publishToMavenLocal   # Publish dep-lib to local Maven
./gradlew :app:linkDebugSharedMacosArm64 # Link app as shared library
./gradlew -PpartialLinkMode=disable :app:linkDebugSharedMacosArm64  # Force failure on incompatibility
```

**C interop test** ([c-caller/main.c](c-caller/main.c)):
Calls Kotlin code via C headers to verify runtime crashes when partial linkage allows broken builds through.

## Expected Behaviors

| Mode | Compile | Runtime |
|------|---------|---------|
| `-Xpartial-linkage=disable` | ❌ Fails with errors | N/A |
| `-Xpartial-linkage=enable` | ✅ Succeeds with warnings | ⚠️ Crashes on missing symbols |

**Key insight**: Partial linkage only helps with pre-compiled klibs. Source compilation always needs full symbol resolution.

## Anti-Patterns to Avoid
- Don't manually compile individual modules in isolation—use run.sh workflow
- Don't use partial linkage for production (always disable for release builds)
- Don't expect partial linkage to work with source-level compilation (it's for binary klibs only)
