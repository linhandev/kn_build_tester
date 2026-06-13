# Repro: Linux Linker "Argument list too long" (ARG_MAX / E2BIG)

## The Bug

When Kotlin/Native links a Linux executable with static caches enabled (default),
each source file produces a separate cache `.a` file. All cache paths are passed
as individual command-line arguments to `ld.lld`. With enough source files, the
command line exceeds the OS `ARG_MAX` limit and `execve()` fails with `E2BIG`.

**Root cause:** `GccBasedLinker` in the Kotlin/Native compiler passes libraries
as raw arguments, unlike:
- `MacOSBasedLinker` — uses `-filelist` (fixed in KT-66061, commit 921c5eee428e)
- `OhosLinker` — uses `@file` response files (CPF fix)

`GccBasedLinker` was never updated to use `@file` response files despite
`ld.lld` and `ld.gold` both supporting them.

## Reproducing

```bash
# Generate source files (default: 200 files)
python3 generate-sources.py

# Trigger the bug on Linux (or cross-compile from macOS to Linux)
./gradlew linkDebugExecutableLinuxX64

# Verify macOS works fine (MacOSBasedLinker already uses -filelist)
./gradlew linkDebugExecutableMacosArm64
```

## Expected Behavior

| Target | Linker | Result |
|--------|--------|--------|
| `linuxX64` | `GccBasedLinker` (ld.lld) | ❌ `Argument list too long` / `E2BIG` |
| `macosArm64` | `MacOSBasedLinker` (ld64) | ✅ Uses `-filelist`, succeeds |

## Fix

Mirror the OhosLinker pattern in `GccBasedLinker`:
1. Write library paths to a temp file via `tempFiles.create("libraries")`
2. Pass `@/path/to/tempfile` instead of individual library arguments
3. `ld.lld` and `ld.gold` support `@file` natively

## Files

- `generate-sources.py` — creates N Kotlin source files + Main.kt
- `build.gradle.kts` — KMP project targeting linuxX64 + macosArm64
- `gradle.properties` — KT 2.4.0, static caches enabled (default)
