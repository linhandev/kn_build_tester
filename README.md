# ALI-46 Reproduction: Public @Composable Functions Fail to Compile on OHOS arm64

## Issue
Public `@Composable` functions fail during the Kotlin/Native link phase with error:
```
No file for /PublicGreeting|PublicGreeting(kotlin.String){}[0]
```

Private and internal `@Composable` functions compile and link successfully.

## Environment
- **Kotlin**: 2.2.21-0.2.0-05 (CPF fork)
- **Compose Multiplatform**: 1.9.2-0.3.0-05 (CPF fork)
- **Gradle**: 9.0.0
- **JDK**: 21.0.10 (Temurin)
- **Target**: OHOS arm64 (sharedLib)
- **Note**: iOS arm64 (framework) does NOT reproduce the issue

## Hypothesis
The Compose compiler plugin generates synthetic IR declarations for public @Composable functions that lack proper file associations in the Kotlin/Native serialized module. During the C adapter codegen phase (used for sharedLib but not framework), the `DependenciesTrackerImpl` fails to resolve the file origin for these synthetic declarations, causing an `IllegalStateException`.

## Reproduction Steps

1. Clone this repository and checkout the reproduction branch:
   ```bash
   git clone https://github.com/linhandev/kn_samples.git
   cd kn_samples
   git checkout repro/ALI-46-public-composable-compile-fail
   ```

2. Build the OHOS arm64 shared library:
   ```bash
   ./gradlew :kotlinApp:linkDebugSharedOhosArm64
   ```

3. Observe the compilation failure during the link phase.

## Expected Behavior
The build should succeed, producing a shared library with the public @Composable function exported.

## Actual Behavior
The link phase fails with:
```
e: Compilation failed: No file for /PublicGreeting|PublicGreeting(kotlin.String){}[0]
e: java.lang.IllegalStateException: No file for /PublicGreeting|PublicGreeting(kotlin.String){}[0]
    at org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer.getFileNameOf(KonanPartialModuleDeserializer.kt:90)
    at org.jetbrains.kotlin.backend.konan.serialization.ExternalDeclarationFileNameProvider.getExternalDeclarationFileName(ExternalDeclarationFileNameProvider.kt:31)
    at org.jetbrains.kotlin.backend.konan.DependenciesTrackerImpl.add$lambda$1(DependenciesTracker.kt:110)
    at org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen.buildCAdapter(CAdapterCodegen.kt:58)
    ...
```

## Boundary Observations

### Works (no error):
- **Private @Composable**: `@Composable private fun PrivateGreeting(name: String)`
- **Internal @Composable**: `@Composable internal fun InternalGreeting(name: String)`
- **iOS arm64 framework**: Same public @Composable compiles successfully when targeting iOS arm64 with framework binary type

### Fails:
- **Public @Composable on OHOS arm64 sharedLib**: The exact scenario described in the issue

## Key Files

- `kotlinApp/src/commonMain/kotlin/ComposableVisibility.kt` — Test code with public/private/internal @Composable functions
- `kotlinApp/build.gradle.kts` — Build configuration with OHOS arm64 and iOS arm64 targets
- `gradle.properties` — Kotlin and Compose version configuration

## Stack Trace (Key Excerpt)

```
at org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer.getFileNameOf(KonanPartialModuleDeserializer.kt:90)
at org.jetbrains.kotlin.backend.konan.serialization.ExternalDeclarationFileNameProvider.getExternalDeclarationFileName(ExternalDeclarationFileNameProvider.kt:31)
at org.jetbrains.kotlin.backend.konan.DependenciesTrackerImpl.add$lambda$1(DependenciesTracker.kt:110)
at org.jetbrains.kotlin.backend.konan.DependenciesTrackerImpl.computeFileOrigin(DependenciesTracker.kt:127)
at org.jetbrains.kotlin.backend.konan.DependenciesTrackerImpl.add(DependenciesTracker.kt:109)
at org.jetbrains.kotlin.backend.konan.DependenciesTrackerImpl.add(DependenciesTracker.kt:102)
at org.jetbrains.kotlin.backend.konan.llvm.CodegenLlvmHelpers.externalFunction$backend_native(ContextUtils.kt:377)
at org.jetbrains.kotlin.backend.konan.llvm.ContextUtils.getLlvmFunctionOrNull(ContextUtils.kt:203)
at org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator.llvmFunctionOrNull(CodeGenerator.kt:38)
at org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator.llvmFunction(CodeGenerator.kt:34)
at org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen.buildCAdapter(CAdapterCodegen.kt:58)
```

The error originates in `CAdapterCodegen.buildCAdapter`, which is called when generating C adapters for shared libraries. This explains why iOS frameworks (which don't use C adapters) are unaffected.
