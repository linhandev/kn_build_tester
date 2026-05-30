# ALI-62 LLVM Exit Code 139 Reproduction Attempt

## Issue
LLVM exit code 139 (SIGSEGV) during `linkDebugSharedOhosArm64` with KMP 7.0.0.220

## Environment
- macOS arm64 (Apple Silicon)
- Kotlin/Native CPF versions tested:
  - `2.2.21-0.4.0-01`
  - `2.2.21-0.3.0-06`
  - `2.2.21-EZ.0.2.0-15`

## Project Structure
Minimal KMP project targeting OHOS arm64 with shared library output.

## Reproduction Steps

1. Clone this branch:
   ```bash
   git clone -b repro/ALI-62-llvm-exit-139-debug-link https://github.com/linhandev/kn_samples.git
   cd kn_samples
   ```

2. Run the debug link task:
   ```bash
   ./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-configuration-cache
   ```

3. Expected: LLVM crash with exit code 139
4. Actual: BUILD SUCCESSFUL

## Results

All tested versions completed successfully without LLVM crash:
- `2.2.21-0.4.0-01`: BUILD SUCCESSFUL
- `2.2.21-0.3.0-06`: BUILD SUCCESSFUL  
- `2.2.21-EZ.0.2.0-15`: BUILD SUCCESSFUL

## Observations

The LLVM exit code 139 crash could NOT be reproduced with:
- Simple KMP project (single Kotlin source file)
- Available CPF Kotlin versions from Maven repository
- Debug shared library linking for OHOS arm64

## Hypotheses

The crash may require:
1. The exact "7.0.0.220" version (not found in CPF Maven repo)
2. A more complex project (e.g., Compose Multiplatform with larger codebase)
3. Specific code patterns or dependencies that trigger the LLVM bug
4. Different build configuration or compiler flags

## Next Steps

To reproduce this issue, we need:
- The exact Kotlin version used by "3rd-Framework-KMP 7.0.0.220"
- Or a sample project that exhibits the crash
- Or more details about the code patterns that trigger the issue
