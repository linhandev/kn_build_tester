#!/bin/bash
# Reproduction script for ALI-38: Incremental build artifact size growth
# Version variant test: Kotlin 2.2.21-OH.0.1.0-06
# Compare against Round 1 baseline: Kotlin 2.2.21-0.2.0-12

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEMO_DIR="$SCRIPT_DIR/kmp-cmp-test-demo"
SO_PATH="composeApp/build/bin/ohosArm64/debugShared/libkn.so"
LLVM_BIN="$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin"

echo "=== ALI-38 Reproduction: Kotlin 2.2.21-OH.0.1.0-06 ==="
echo ""

# Clone demo repo if not present
if [ ! -d "$DEMO_DIR" ]; then
    echo "Cloning kmp-cmp-test-demo (branch IR003-kmp-debug)..."
    git clone -b IR003-kmp-debug https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git "$DEMO_DIR"
fi

cd "$DEMO_DIR"

# Switch Kotlin version to OH.0.1.0-06
echo "Switching Kotlin version to 2.2.21-OH.0.1.0-06..."
sed -i.bak 's/kotlin = "2.2.21-0.2.0-12"/kotlin = "2.2.21-OH.0.1.0-06"/' gradle/libs.versions.toml

# === Test 1: Clean build with incremental=true (no build cache) ===
echo ""
echo "=== Test 1: Clean build (incremental=true, --no-build-cache) ==="
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-build-cache --no-configuration-cache \
    -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -3
SIZE_CLEAN=$(stat -f "%z" "$SO_PATH")
echo "Clean build size: $SIZE_CLEAN bytes"

# === Test 2: Incremental rebuild (edit source, no build cache) ===
echo ""
echo "=== Test 2: Incremental rebuild #1 (no build cache) ==="
sed -i.bak 's/(HarmonyOS Native)/(HarmonyOS Native v2)/' \
    composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-build-cache --no-configuration-cache \
    -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -3
SIZE_INC1=$(stat -f "%z" "$SO_PATH")
echo "Incremental #1 size: $SIZE_INC1 bytes (delta: $((SIZE_INC1 - SIZE_CLEAN)))"

# === Test 3: Incremental rebuild (edit source, WITH build cache) ===
echo ""
echo "=== Test 3: Incremental rebuild #2 (WITH build cache) ==="
sed -i.bak 's/(HarmonyOS Native v2)/(HarmonyOS Native v3)/' \
    composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache \
    -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -3
SIZE_INC2=$(stat -f "%z" "$SO_PATH")
echo "Incremental #2 size: $SIZE_INC2 bytes (delta from clean: $((SIZE_INC2 - SIZE_CLEAN)))"

# === Test 4: Control build (cacheKind=none) ===
echo ""
echo "=== Test 4: Control build (cacheKind=none) ==="
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-build-cache --no-configuration-cache \
    -Pkotlin.native.cacheKind.ohosArm64=none \
    -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -3
SIZE_CONTROL=$(stat -f "%z" "$SO_PATH")
echo "Control build size: $SIZE_CONTROL bytes"

# === Summary ===
echo ""
echo "========================================="
echo "SUMMARY (Kotlin 2.2.21-OH.0.1.0-06)"
echo "========================================="
echo "Clean build (incremental):  $SIZE_CLEAN bytes"
echo "Incremental #1 (no cache):  $SIZE_INC1 bytes (growth: $((SIZE_INC1 - SIZE_CLEAN)))"
echo "Incremental #2 (w/ cache):  $SIZE_INC2 bytes (growth: $((SIZE_INC2 - SIZE_CLEAN)))"
echo "Control (cacheKind=none):   $SIZE_CONTROL bytes"
echo ""
PENALTY=$((SIZE_CLEAN - SIZE_CONTROL))
PCT=$(echo "scale=1; $PENALTY * 100 / $SIZE_CONTROL" | bc)
echo "Baseline penalty: $PENALTY bytes ($PCT%)"
echo ""
echo "=== Round 1 comparison (Kotlin 2.2.21-0.2.0-12) ==="
echo "Round 1 incremental: 255,481,768 bytes"
echo "Round 1 control:     140,007,552 bytes"
echo "Round 1 penalty:     115,474,216 bytes (82%)"

# === Binary section analysis ===
echo ""
echo "=== Binary Section Analysis ==="
if [ -x "$LLVM_BIN/llvm-readelf" ]; then
    echo "--- Control build sections ---"
    ./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-build-cache --no-configuration-cache \
        -Pkotlin.native.cacheKind.ohosArm64=none \
        -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -1
    $LLVM_BIN/llvm-readelf -S "$SO_PATH" 2>&1 | grep -E '\.(text|data\.rel\.ro|symtab|strtab|rodata)'
    CTRL_SYMS=$($LLVM_BIN/llvm-readelf -s "$SO_PATH" 2>&1 | grep -c "FUNC\|OBJECT\|NOTYPE" || true)
    echo "Control symbols: $CTRL_SYMS"

    echo ""
    echo "--- Incremental build sections ---"
    ./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-build-cache --no-configuration-cache \
        -Dohos.sdk.home="$HOME/Library/OpenHarmony/Sdk" 2>&1 | tail -1
    $LLVM_BIN/llvm-readelf -S "$SO_PATH" 2>&1 | grep -E '\.(text|data\.rel\.ro|symtab|strtab|rodata)'
    INC_SYMS=$($LLVM_BIN/llvm-readelf -s "$SO_PATH" 2>&1 | grep -c "FUNC\|OBJECT\|NOTYPE" || true)
    echo "Incremental symbols: $INC_SYMS"
else
    echo "LLVM tools not found at $LLVM_BIN - skipping section analysis"
fi
