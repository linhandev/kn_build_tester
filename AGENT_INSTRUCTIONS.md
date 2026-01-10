# Agent Instructions: Building C/C++ Programs for OHOS (OpenHarmony)

This document provides comprehensive instructions for building C/C++ programs targeting OHOS devices, including code coverage setup.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Key Paths and Locations](#key-paths-and-locations)
3. [Build Process](#build-process)
4. [Code Coverage Options](#code-coverage-options)
5. [Device Deployment](#device-deployment)
6. [Coverage Report Generation](#coverage-report-generation)

## Prerequisites

- DevEco Studio installed (provides OHOS SDK)
- LLVM toolchain (typically comes with DevEco Studio or Konan dependencies)
- OHOS device connected via USB or network
- `hdc` tool available (OHOS Device Connector, similar to Android's `adb`)

## Key Paths and Locations

### Clang Compiler Location
```bash
~/.konan/dependencies/llvm-1201-macos-aarch64/bin/clang++
```
**Note**: Version number (1201) may vary. Check your `~/.konan/dependencies/` directory for the actual version.

### Sysroot Location
```bash
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot
```
**Note**: Path may vary based on DevEco Studio installation location. The sysroot contains system headers and libraries for OHOS.

### Resource Directory
```bash
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4
```
**Note**: Clang version (15.0.4) may vary. Check your DevEco Studio SDK directory.

### Clang Runtime Libraries
```bash
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4/lib/aarch64-linux-ohos
```
This directory contains runtime libraries that need to be linked with `-L` flag.

### Target Architecture
- **Target**: `aarch64-linux-ohos`
- **Architecture**: ARM64 (aarch64)
- **OS**: OpenHarmony (OHOS)

## Build Process

### Basic Build Command Structure

```bash
~/.konan/dependencies/llvm-1201-macos-aarch64/bin/clang++ \
  --sysroot /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot \
  -O3 \
  -fomit-frame-pointer \
  source.cpp \
  -target aarch64-linux-ohos \
  -L/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4/lib/aarch64-linux-ohos \
  -resource-dir /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/lib/clang/15.0.4 \
  -o output
```

### Multi-Stage Build (IR -> Object -> Executable)

For better control and debugging, split the build into stages:

#### Stage 1: Emit LLVM IR
```bash
clang++ \
  --sysroot <SYSROOT> \
  -emit-llvm -S \
  -O3 \
  -fomit-frame-pointer \
  source.cpp \
  -target aarch64-linux-ohos \
  -resource-dir <RESOURCE_DIR> \
  -o source.ll
```

#### Stage 2: Compile IR to Object File
```bash
clang++ \
  --sysroot <SYSROOT> \
  -c source.ll \
  -O3 \
  -fomit-frame-pointer \
  -target aarch64-linux-ohos \
  -resource-dir <RESOURCE_DIR> \
  -o source.o
```

#### Stage 3: Link Object to Executable
```bash
clang++ \
  --sysroot <SYSROOT> \
  -O3 \
  -fomit-frame-pointer \
  source.o \
  -target aarch64-linux-ohos \
  -L<CLANG_LIB_DIR> \
  -resource-dir <RESOURCE_DIR> \
  -o executable
```

### Important Compiler Flags

- `--sysroot <PATH>`: Specifies the system root directory
- `-target aarch64-linux-ohos`: Target architecture and OS
- `-resource-dir <PATH>`: Clang resource directory
- `-L<PATH>`: Library search path (for runtime libraries)
- `-O3`: Optimization level
- `-fomit-frame-pointer`: Omit frame pointer for smaller code

## Code Coverage Options

### Option 1: LLVM GCOV (Traditional)

**Build Flags:**
- `-fprofile-arcs`: Generate arc profiling information
- `-ftest-coverage`: Generate coverage data files (.gcda, .gcno)

**Example:**
```bash
clang++ \
  --sysroot <SYSROOT> \
  -fprofile-arcs -ftest-coverage \
  source.cpp \
  -target aarch64-linux-ohos \
  -L<CLANG_LIB_DIR> \
  -resource-dir <RESOURCE_DIR> \
  -o executable
```

**Output Files:**
- `source.gcno`: Graph file (generated at compile time)
- `source.gcda`: Data file (generated at runtime)

### Option 2: Source-based Code Coverage (LLVM)

**Build Flags:**
- `-fprofile-instr-generate`: Generate instrumentation for profiling
- `-fcoverage-mapping`: Generate coverage mapping information

**Example:**
```bash
clang++ \
  --sysroot <SYSROOT> \
  -fprofile-instr-generate -fcoverage-mapping \
  source.cpp \
  -target aarch64-linux-ohos \
  -L<CLANG_LIB_DIR> \
  -resource-dir <RESOURCE_DIR> \
  -o executable
```

**Output Files:**
- `default.profraw`: Raw profiling data (generated at runtime)

## Device Deployment

### Using HDC (OHOS Device Connector)

HDC is similar to Android's ADB. Common commands:

#### Check Device Connection
```bash
hdc list targets
```

#### Send File to Device
```bash
hdc file send <local_file> <device_path>
```

#### Receive File from Device
```bash
hdc file recv <device_path> <local_file>
```

#### Execute Shell Command
```bash
hdc shell <command>
```

#### Set Permissions
```bash
hdc shell chmod 777 <device_path>
```

### Deployment Workflow

1. **Build executable** (with coverage flags)
2. **Send to device:**
   ```bash
   hdc file send main /data/local/tmp/main
   ```
3. **Set permissions:**
   ```bash
   hdc shell chmod 777 /data/local/tmp/main
   ```
4. **Run executable:**
   ```bash
   hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main
   ```
   **Note**: Set `LD_LIBRARY_PATH` if you need to load libraries from a specific location.

5. **Retrieve coverage data:**
   - For GCOV: `hdc file recv /data/local/tmp/main.gcda ./main.gcda`
   - For Source-based: `hdc file recv /data/local/tmp/default.profraw ./default.profraw`

### GCOV Path Redirection

For GCOV coverage, use environment variables to control where coverage files are written:

```bash
GCOV_PREFIX=/data/local/tmp GCOV_PREFIX_STRIP=99 /data/local/tmp/main
```

- `GCOV_PREFIX`: Base path for coverage files
- `GCOV_PREFIX_STRIP`: Number of path components to strip from build path

## Coverage Report Generation

### GCOV Reports

**Using llvm-cov (recommended):**
```bash
llvm-cov gcov source.cpp \
  -format=html \
  -output-dir=coverage_report
```

**Using traditional gcov:**
```bash
gcov source.cpp
# Generates source.cpp.gcov
```

### Source-based Coverage Reports

1. **Merge profraw files:**
   ```bash
   llvm-profdata merge -sparse default.profraw -o default.profdata
   ```

2. **Generate HTML report:**
   ```bash
   llvm-cov show executable \
     -instr-profile=default.profdata \
     -format=html \
     -output-dir=coverage_report \
     source.cpp
   ```

3. **Generate text report:**
   ```bash
   llvm-cov show executable \
     -instr-profile=default.profdata \
     -format=text \
     source.cpp > coverage.txt
   ```

4. **Generate summary:**
   ```bash
   llvm-cov report executable \
     -instr-profile=default.profdata \
     source.cpp
   ```

## Common Issues and Solutions

### Issue: Cannot find sysroot
**Solution**: Verify DevEco Studio installation path and SDK location.

### Issue: Library linking errors
**Solution**: Ensure `-L` flag points to correct clang runtime library directory.

### Issue: Coverage files not generated
**Solution**: 
- Ensure executable exits normally (not killed)
- Check file permissions on device
- Verify coverage flags were used during compilation
- For GCOV, use `GCOV_PREFIX` and `GCOV_PREFIX_STRIP` environment variables

### Issue: HDC connection failed
**Solution**:
- Check USB connection or network settings
- Verify device is in developer mode
- Try `hdc kill` then `hdc start`

### Issue: Version mismatch in source-based coverage
**Solution**: Ensure LLVM versions match between device and host, or generate reports on the device.

## Example: Complete Workflow

```bash
# 1. Build with coverage
./gcov.sh
# or
./sourcebased.sh

# 2. Deploy to device
./deploy.sh gcov
# or
./deploy.sh sourcebased

# 3. Generate report (run build script again)
./gcov.sh
# or
./sourcebased.sh

# 4. View report
cat main.cpp.gcov  # For GCOV
open coverage_report_sourcebased/index.html  # For source-based
```

## Additional Resources

- OHOS Native Development Documentation
- LLVM Code Coverage Documentation
- DevEco Studio User Guide
