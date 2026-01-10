# Code Coverage Instructions for OHOS C/C++ Programs

This document provides instructions for setting up and generating code coverage reports for C/C++ programs targeting OHOS devices.

## Table of Contents
1. [Code Coverage Options](#code-coverage-options)
2. [Device Deployment with Coverage](#device-deployment-with-coverage)
3. [Coverage Report Generation](#coverage-report-generation)
4. [Common Issues and Solutions](#common-issues-and-solutions)

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
  -target <TARGET_ARCH> \
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
  -target <TARGET_ARCH> \
  -L<CLANG_LIB_DIR> \
  -resource-dir <RESOURCE_DIR> \
  -o executable
```

**Output Files:**
- `default.profraw`: Raw profiling data (generated at runtime)

## Device Deployment with Coverage

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

### Issue: Coverage files not generated
**Solution**: 
- Ensure executable exits normally (not killed)
- Check file permissions on device
- Verify coverage flags were used during compilation
- For GCOV, use `GCOV_PREFIX` and `GCOV_PREFIX_STRIP` environment variables

### Issue: Version mismatch in source-based coverage
**Solution**: Ensure LLVM versions match between device and host, or generate reports on the device.

## Example: Complete Workflow

```bash
# Complete workflow in one command:
./gcov.sh              # Build -> Deploy -> Generate Report
# or
./sourcebased.sh        # Build -> Deploy -> Generate Report

# View reports
cat main.cpp.gcov  # For GCOV
open coverage_report_sourcebased/index.html  # For source-based
```

## Additional Resources

- LLVM Code Coverage Documentation
- OHOS Native Development Documentation
