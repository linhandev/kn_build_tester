# Kotlin Native GCOV Code Coverage Samples

```shell
./gradlew :switchLib:publishToMavenLocal --rerun-tasks

hdc uninstall com.example.nativecppdemo; ./gradlew startHarmonyAppDebug --rerun-tasks && \
sleep 2 && \
rm -rf gcov && \
hdc file recv /data/app/el2/100/base/com.example.nativecppdemo/files/gcov/ . && \
cd gcov && \
cp -f ../kotlinApp/**/**.gcno . && \
cp -f ../harmonyApp/entry/.cxx/**/*.gcno . && \
rm -rf temp && \
mkdir temp && \
python -m gcovr --html --html-details --output temp/coverage.html --root .. \
  --gcov-ignore-errors=source_not_found \
  --gcov-ignore-errors=output_error \
  --gcov-ignore-errors=no_working_dir_found . && \
cd ..

python -m gcovr --json --root .. \
  --gcov-ignore-errors=source_not_found \
  --gcov-ignore-errors=output_error \
  --gcov-ignore-errors=no_working_dir_found .

```

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
- `GCOVR_ERRORS_EXPLAINED.md` - Explanation of gcovr error types and when to ignore them

See `kotlin/README.md` for standalone demo details.
