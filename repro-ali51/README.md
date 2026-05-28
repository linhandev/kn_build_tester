# ALI-51 Reproduction: Public @Composable fails to link on OHOS arm64

## Issue
Kotlin 2.2.21-0.2.0-05 fails to link `@Composable` functions with `public` visibility when building OHOS arm64 shared libraries.

## Error
```
e: Compilation failed: No file for /App|App(){}[0]
e: java.lang.IllegalStateException: No file for /App|App(){}[0]
    at KonanPartialModuleDeserializer.getFileNameOf
    at ExternalDeclarationFileNameProvider.getExternalDeclarationFileName
    at DependenciesTrackerImpl.add
    at CAdapterCodegen.buildCAdapter
```

## Environment
- Kotlin: 2.2.21-0.2.0-05
- Compose Multiplatform: 1.9.2-0.2.0-03
- Gradle: 8.13
- Host: macOS aarch64

## Quick Start
```bash
git clone -b repro/ali51-public-composable-fail https://github.com/linhandev/kn_samples.git
cd kn_samples/repro-ali51

# FAILS - public @Composable on OHOS arm64
./gradlew clean linkDebugSharedOhosArm64

# WORKS - iOS arm64 compile
./gradlew compileKotlinIosArm64

# WORKAROUND: change public to internal in shared/src/commonMain/kotlin/App.kt
# Then: ./gradlew clean linkDebugSharedOhosArm64  -> succeeds
```
