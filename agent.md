# Agent Summary for kn_samples Project

## Project Purpose
This project is a sample demonstrating Kotlin Native integration with HarmonyOS. It showcases memory safety testing using Address Sanitizer (ASAN) to detect buffer overflows and other memory issues in native code. The project includes a Kotlin Native library that interacts with C code, integrated into a HarmonyOS application.

## Project Structure
- **harmonyApp/**: The main HarmonyOS application module, containing the app configuration, resources, and build scripts.
- **kotlinApp/**: A Kotlin Multiplatform module with:
  - `commonMain/`: Shared Kotlin code, including expect functions.
  - `ohosArm64Main/`: Platform-specific implementation for HarmonyOS ARM64, including ASAN test code.
  - `nativeInterop/`: C interop definitions.
- **c-caller/**: C source code that provides functions called from Kotlin, such as the overflow trigger.
- **Root files**: Gradle build scripts (`build.gradle.kts`, `settings.gradle.kts`), properties, and scripts for running the project.

## Runtime Workflow
1. The Gradle build system compiles the Kotlin Native code into a shared library (e.g., `libentry.so`).
2. The C code is compiled and linked with the Kotlin library.
3. The HarmonyOS app loads the native library and calls the Kotlin functions via C wrappers.
4. The ASAN test allocates memory, intentionally causes a buffer overflow, and checks for detection.
5. Output is logged, and ASAN reports any memory violations.

## Testing Instructions
To test the project after any code changes:
- Run the command: `./gradlew :kotlinApp:startHarmonyAppDebug`
- This builds the Kotlin app module and starts the HarmonyOS app in debug mode on a connected device or emulator.
- Monitor the logs for ASAN reports on memory issues.

## Additional Notes
- Ensure DevEco Studio or a compatible HarmonyOS development environment is set up.
- The project uses experimental APIs, so opt-in annotations are required.
- For memory management, native heap allocations are used for interop with C.
- Principle: Keep the project as simple as possible by removing unnecessary wrappers and complexities.
