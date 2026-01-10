# OHOS C++ Code Coverage Demos

This repository demonstrates two code coverage methods for C++ programs targeting OHOS (OpenHarmony) devices:

1. **GCOV Coverage** (`gcov.sh`) - Traditional LLVM GCOV coverage
2. **Source-based Coverage** (`sourcebased.sh`) - Modern LLVM source-based coverage

## Quick Start

### GCOV Coverage Demo
```bash
./gcov.sh              # Build with GCOV coverage support
./deploy.sh gcov        # Deploy to OHOS device and run
./gcov.sh               # Run again to generate coverage report (after deploy)
```

### Source-based Coverage Demo
```bash
./sourcebased.sh        # Build with source-based coverage support
./deploy.sh sourcebased # Deploy to OHOS device and run
./sourcebased.sh        # Run again to generate coverage report (after deploy)
```

## Files

- `main.cpp` - Sample C++ program with if-else, switch, and function calls
- `gcov.sh` - Build script for GCOV coverage (IR -> .o -> exe) + report generation
- `sourcebased.sh` - Build script for source-based coverage (IR -> .o -> exe) + report generation
- `deploy.sh` - Common deployment script (accepts gcov|sourcebased)
- `AGENT_INSTRUCTIONS.md` - Comprehensive build instructions

## Build Process

Both demos use a three-stage build process with separate intermediate files:

### Stage 1: Emit LLVM IR with Instrumentation
The compiler emits LLVM IR (`.ll` file) with coverage instrumentation embedded:

**GCOV:**
```bash
clang++ -fprofile-arcs -ftest-coverage -emit-llvm -S main.cpp -o main_gcov.ll
```

**Source-based:**
```bash
clang++ -fprofile-instr-generate -fcoverage-mapping -emit-llvm -S main.cpp -o main_sourcebased.ll
```

The IR file contains instrumentation calls that will track code execution:
- **GCOV**: Arc profiling calls (`__llvm_gcov_*` functions)
- **Source-based**: Profile counter updates (`__llvm_profile_instrument_*`)

### Stage 2: Compile IR to Object File
The instrumented IR is compiled to an object file (`.o`):
```bash
# GCOV: clang++ -c main_gcov.ll -o main_gcov.o
# Source-based: clang++ -c main_sourcebased.ll -o main_sourcebased.o
```

### Stage 3: Link to Executable
The object file is linked with coverage runtime libraries:
```bash
# GCOV: clang++ main_gcov.o -o main_gcov
# Source-based: clang++ main_sourcebased.o -o main_sourcebased
```

## File Naming

Each coverage mode uses separate intermediate files and executables to avoid conflicts:

**GCOV mode:**
- IR: `main_gcov.ll`
- Object: `main_gcov.o`
- Executable: `main_gcov`
- Coverage data: `main_gcov.gcda`, `main_gcov.gcno`
- Report: `coverage_report_gcov/`

**Source-based mode:**
- IR: `main_sourcebased.ll`
- Object: `main_sourcebased.o`
- Executable: `main_sourcebased`
- Coverage data: `default_sourcebased.profraw`
- Report: `coverage_report_sourcebased/`

## LLVM IR Instrumentation Demonstration

The instrumentation is visible in the generated IR files. Here's what to look for:

### GCOV Instrumentation in IR

When you build with `gcov.sh`, the `main_gcov.ll` file will contain GCOV counter arrays:
```llvm
@__llvm_gcov_ctr = internal global [6 x i64] zeroinitializer
@__llvm_gcov_ctr.3 = internal global [3 x i64] zeroinitializer
```

These counters track arc execution frequencies. The instrumentation is embedded in the IR before code generation, demonstrating that coverage tracking happens at the LLVM IR level.

### Source-based Instrumentation in IR

When you build with `sourcebased.sh`, the `main_sourcebased.ll` file will contain:
```llvm
@__llvm_coverage_mapping = private constant { { i32, i32, i32, i32 }, [47 x i8] } ...
@__profd__Z9calculateiic = private global { i64, i64, i64*, i8*, i8*, i32, [2 x i16] } ...
```

The `__llvm_coverage_mapping` contains compressed source mapping data, and `__profd_*` entries track function-level coverage with profile counters. This shows instrumentation metadata embedded directly in the IR.

### Viewing the Instrumentation

To inspect the instrumentation:
```bash
# Build first
./gcov.sh
# or
./sourcebased.sh

# View the IR file
grep -E "__llvm_gcov|__llvm_coverage|__profd_" main_gcov.ll | head -20
# or for source-based:
grep -E "__llvm_gcov|__llvm_coverage|__profd_" main_sourcebased.ll | head -20
```

The IR files demonstrate that instrumentation is added at the LLVM IR level, before code generation, ensuring accurate coverage tracking.

## Sample Program

The `main.cpp` program includes:
- **If-else statements**: Command-line argument handling
- **Switch statement**: Calculator operations (+, -, *, /)
- **Function calls**: `calculate()` function with multiple branches

## Coverage Methods

### GCOV (Traditional)
- **Flags**: `-fprofile-arcs -ftest-coverage`
- **Output**: `.gcno` (graph) and `.gcda` (data) files
- **Tools**: `gcov`, `llvm-gcov`, `llvm-cov`

### Source-based (Modern)
- **Flags**: `-fprofile-instr-generate -fcoverage-mapping`
- **Output**: `default.profraw` file
- **Tools**: `llvm-profdata`, `llvm-cov`

## Prerequisites

- DevEco Studio installed
- LLVM toolchain (from Konan dependencies or DevEco Studio)
- OHOS device connected via USB
- `hdc` tool available

## Documentation

See [AGENT_INSTRUCTIONS.md](AGENT_INSTRUCTIONS.md) for detailed instructions on:
- Finding clang compiler location
- Locating sysroot and resource directories
- Building C/C++ programs for OHOS
- Deploying to devices using hdc
- Generating coverage reports

## Notes

- Paths in build scripts assume default DevEco Studio installation
- Adjust paths in build scripts if your installation differs
- Ensure OHOS device is connected before running `deploy.sh`
- Coverage data files are generated on the device during execution
- The LLVM IR files (`main_gcov.ll`, `main_sourcebased.ll`) demonstrate instrumentation at the IR level
- Each mode uses separate intermediate files and executables to avoid conflicts
- Report generation is integrated into the build scripts - run the build script again after deployment to generate reports
