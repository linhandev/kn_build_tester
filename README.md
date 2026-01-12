# Kotlin Native GCOV Code Coverage Samples

## Quick Start

### Standalone Executable
```bash
cd kotlin
./test_gcov.sh
```

### HarmonyOS App
```bash
./gradlew startHarmonyAppDebug
# Click "Run ASan Test" in app (calls __gcov_dump())
# Retrieve .gcda from /data/storage/el2/base/files/gcov-1/
```

## Project Structure

- `kotlin/` - Standalone executable demo (fully tested ✅)
- `kotlinApp/` - KMP library project
- `harmonyApp/` - HarmonyOS app integration
- `*.md` - Documentation guides

## Key Concepts

**Enable Coverage**: `-Xbinary=coverage=true -g`

**Files**:
- `.gcno` - Compile-time graph (next to output)
- `.gcda` - Runtime counters (controlled by GCOV_PREFIX)

**HarmonyOS Apps**: Must call `__gcov_dump()` manually (apps don't exit)

## Documentation

- `QUICK_START_GCOV.md` - 10-line HAP setup
- `GCOV_FLUSH_SOLUTION.md` - Why manual flush is needed
- `COMPLETE_ANSWER.md` - LLVM source analysis

See `kotlin/README.md` for standalone demo details.
