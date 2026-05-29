# Reproduction: compose-multiplatform 1.9.2-0.2.0-01 Build Failure (ALI-54)

## Issue
compose-multiplatform version `1.9.2-0.2.0-01` fails to build a project compared to version `1.9.2-0.2.1`.

## Environment
- macOS (Darwin arm64)
- Kotlin/Native 2.2.21-0.3.0-04
- Gradle 8.9
- Compose Multiplatform plugin version tested: 1.9.2-0.2.0-01 vs 1.9.2-0.2.1

## Scenario 1: Plugin resolution without custom Maven repo

If the `pluginManagement` block does NOT include `maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")`, the plugin cannot be resolved:

```
# Edit settings.gradle.kts: remove the eazytec maven line from pluginManagement.repositories
./gradlew :composeApp:tasks
```

**Expected error:**
```
Plugin [id: 'org.jetbrains.compose', version: '1.9.2-0.2.0-01', apply: false] was not found in any of the following sources:
- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Included Builds (No included builds contain this plugin)
- Plugin Repositories (could not resolve plugin artifact 'org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:1.9.2-0.2.0-01')
```

This same error occurs for version `1.9.2-0.2.1` — neither is published to the Gradle Plugin Portal.

## Scenario 2: Full build with custom Maven repo (both versions)

With the custom maven repo configured (default `settings.gradle.kts`), both versions compile and link successfully.

### Build with version 1.9.2-0.2.0-01 (current configuration):
```bash
./gradlew :composeApp:linkDebugSharedOhosArm64
# BUILD SUCCESSFUL — but with ~50 informational warnings about unlinked OHOS NAPI symbols
```

### Switch to version 1.9.2-0.2.1:
```bash
# Edit build.gradle.kts: change version "1.9.2-0.2.0-01" to "1.9.2-0.2.1"
./gradlew clean :composeApp:linkDebugSharedOhosArm64
# BUILD SUCCESSFUL — zero warnings
```

## Key Observations

1. **Plugin resolution**: Neither version is available on standard Gradle Plugin Portal. Both require the custom maven repo `https://maven.eazytec-cloud.com/nexus/repository/maven-public`.

2. **OHOS NAPI warnings in 1.9.2-0.2.0-01**: The link step produces ~50 info-level warnings like:
   ```
   i: <org.jetbrains.compose.ui:ui> @ .../JsEnv.kt:246:25: Function 'napi_create_function' can not be called: No function found for symbol 'platform.ohos/napi_create_function|...'
   ```
   These indicate the compose-ui klib was compiled against a Kotlin/Native distribution with different OHOS platform definitions than what's available at link time.

3. **No warnings in 1.9.2-0.2.1**: The same build produces zero such warnings.

4. **Dependency version differences**:
   - `1.9.2-0.2.0-01` uses: `atomicfu:0.31.0-OH-001`, `kotlinx-coroutines-core:1.10.2-OH-103`, `lifecycle:2.9.4-OH.0.1.2-15`
   - `1.9.2-0.2.1` uses: `atomicfu:0.31.0-0.2.0`, `kotlinx-coroutines-core:1.10.2-0.2.0`, `lifecycle:2.9.4-0.2.1`

## Reproduce Script

```bash
# Test 1: Plugin resolution failure (remove custom repo)
./gradlew :composeApp:tasks 2>&1 | head -20

# Test 2: Build with broken version (has NAPI warnings)
./gradlew :composeApp:linkDebugSharedOhosArm64 2>&1 | grep -c "^i:"
# Expected: ~50 warnings

# Test 3: Build with working version (clean, no warnings)
# Edit build.gradle.kts version to "1.9.2-0.2.1"
./gradlew clean :composeApp:linkDebugSharedOhosArm64 2>&1 | grep -c "^i:"
# Expected: 0 warnings
```
