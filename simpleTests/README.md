# Kotlin Native GCOV Coverage Samples

## Quick Start

### Standalone Executable
```bash
./test_kexe.sh
```
Coverage report: `coverage/test_coverage.kt.gcov`

### C Driver + Kotlin .so
```bash
./test_so.sh
```
Coverage report: `coverage/c_driver_test.kt.gcov`

## Files

- `test_coverage.kt` - Standalone executable demo
- `c_driver_test.kt` - Kotlin library loaded by C driver
- `c_driver.cpp` - C program that loads Kotlin .so
- `test_kexe.sh` - Build and test standalone executable
- `test_so.sh` - Build and test C driver + Kotlin .so

## Output

All coverage files (`.gcov`, `.gcda`) are written to the `coverage/` folder.
