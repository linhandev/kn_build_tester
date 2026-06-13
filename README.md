# Repro: Linux Linker "Argument list too long" (ARG_MAX / E2BIG)

## The Bug

When Kotlin/Native links a Linux executable with incremental compilation enabled,
each source file produces a per-file static cache `.a` file. All cache paths are
passed as individual command-line arguments to `ld.lld`. With enough source files
(~15000), the command line exceeds the OS `ARG_MAX` limit (2MB on Linux) and
`execve()` fails with `E2BIG`.

**Root cause:** `GccBasedLinker` in the Kotlin/Native compiler passes libraries
as raw arguments, unlike:
- `MacOSBasedLinker` — uses `-filelist` (fixed in KT-66061)
- `OhosLinker` — uses `@file` response files (CPF fix)

`GccBasedLinker` was never updated to use `@file` response files despite
`ld.lld` and `ld.gold` both supporting them.

## Reproducing

```bash
# 1. Generate 15000 source files (not committed to git)
python3 generate-sources.py

# 2. Build for Linux — triggers the bug
./gradlew linkDebugExecutableLinuxX64
# Expected: "Argument list too long" / E2BIG from ld.lld

# 3. Build for macOS — succeeds (MacOSBasedLinker uses -filelist)
./gradlew linkDebugExecutableMacosArm64
```

## How It Works

With `kotlin.incremental.native=true` + `kotlin.native.cacheOrchestration=compiler`:
1. Each `.kt` source file gets compiled into a per-file static cache `.a` file
2. All cache `.a` paths are passed as individual arguments to the linker
3. With 15000 source files → **24801 cache `.a` files** × ~170 bytes/path = **3.97MB** > 2MB ARG_MAX → `execve()` fails with error 7

## Confirmed Behavior (KT 2.4.0)

| Target | Linker | Result |
|--------|--------|--------|
| `linuxX64` | `GccBasedLinker` (ld.lld) | ❌ `java.io.IOException: Exec failed, error: 7 (Argument list too long)` |
| `macosArm64` | `MacOSBasedLinker` (ld64) | ✅ Uses `-filelist`, succeeds (Sum = 124750) |

## Fix

Mirror the OhosLinker pattern in `GccBasedLinker`:
1. Write library paths to a temp file via `tempFiles.create("libraries")`
2. Pass `@/path/to/tempfile` instead of individual library arguments
3. `ld.lld` and `ld.gold` support `@file` natively

## Files

- `generate-sources.py` — creates N Kotlin source files + Main.kt (default: 15000)
- `build.gradle.kts` — KMP project targeting linuxX64 + macosArm64
- `gradle.properties` — KT 2.4.0, incremental compilation enabled
- `src/commonMain/kotlin/generated/` — generated files (gitignored)
