#!/bin/bash
# Reproduction script for ALI-38: Incremental build artifact size growth on OHOS ARM64
# 
# This script:
# 1. Clones the demo project (IR003-kmp-debug branch)
# 2. Runs a clean build with kotlin.incremental.native=true, measures artifact
# 3. Makes a small source change and rebuilds incrementally, measures again
# 4. Runs a control build with kotlin.native.cacheKind.ohosArm64=none
# 5. Compares binary sections using llvm-readelf

set -euo pipefail

DEMO_REPO="https://gitcode.com/CPF-KMP-CMP/kmp-cmp-test-demo.git"
DEMO_BRANCH="IR003-kmp-debug"
WORK_DIR="$(pwd)/repro_work"
KONAN_LLVM="$HOME/.konan/dependencies/llvm-1914-aarch64-macos-dev-10/bin"

# Find llvm tools - try konan first, then system
LLVM_SIZE="${KONAN_LLVM}/llvm-size"
LLVM_READELF="${KONAN_LLVM}/llvm-readelf"
if [ ! -x "$LLVM_SIZE" ]; then
    LLVM_SIZE="llvm-size"
    LLVM_READELF="llvm-readelf"
fi

echo "=== ALI-38 Reproduction: Incremental Build Artifact Size Growth ==="
echo ""

# Step 0: Clone demo repo
if [ ! -d "$WORK_DIR" ]; then
    echo "[Step 0] Cloning demo repo..."
    git clone --branch "$DEMO_BRANCH" --depth 1 "$DEMO_REPO" "$WORK_DIR"
fi

cd "$WORK_DIR"

SOURCE_FILE="composeApp/src/ohosArm64Main/kotlin/com/example/testdemo/OhosEntryGreeting.kt"
ARTIFACT="composeApp/build/bin/ohosArm64/debugShared/libkn.so"

measure_size() {
    stat -f "%z" "$1" 2>/dev/null || stat -c "%s" "$1"
}

# ============================================================
# EXPERIMENT A: Incremental compilation enabled (default config)
# ============================================================
echo ""
echo "=== EXPERIMENT A: kotlin.incremental.native=true (default) ==="
echo ""

# A1: Clean build
echo "[A1] Clean build with incremental enabled..."
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache -q 2>/dev/null
SIZE_A_CLEAN=$(measure_size "$ARTIFACT")
cp "$ARTIFACT" /tmp/libkn_A_clean.so
echo "  Clean artifact size: $SIZE_A_CLEAN bytes ($(echo "scale=1; $SIZE_A_CLEAN / 1048576" | bc) MB)"

# A2: Make small change and rebuild incrementally
echo "[A2] Making small source change (string literal)..."
sed -i.bak 's/(HarmonyOS Native)/(HarmonyOS Native v2)/' "$SOURCE_FILE"
echo "  Rebuilding incrementally..."
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache -q 2>/dev/null
SIZE_A_INC1=$(measure_size "$ARTIFACT")
cp "$ARTIFACT" /tmp/libkn_A_inc1.so
echo "  Incremental #1 size: $SIZE_A_INC1 bytes ($(echo "scale=1; $SIZE_A_INC1 / 1048576" | bc) MB)"
GROWTH_A1=$((SIZE_A_INC1 - SIZE_A_CLEAN))
echo "  Growth: ${GROWTH_A1} bytes"

# A3: Second incremental change
echo "[A3] Making second source change..."
sed -i.bak 's/(HarmonyOS Native v2)/(HarmonyOS Native v3)/' "$SOURCE_FILE"
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache -q 2>/dev/null
SIZE_A_INC2=$(measure_size "$ARTIFACT")
echo "  Incremental #2 size: $SIZE_A_INC2 bytes"
GROWTH_A2=$((SIZE_A_INC2 - SIZE_A_INC1))
echo "  Additional growth: ${GROWTH_A2} bytes"

# Restore original
mv "${SOURCE_FILE}.bak" "$SOURCE_FILE" 2>/dev/null || true
sed -i.bak 's/(HarmonyOS Native v3)/(HarmonyOS Native)/' "$SOURCE_FILE"
rm -f "${SOURCE_FILE}.bak"

# ============================================================
# EXPERIMENT B: Control - cacheKind=none
# ============================================================
echo ""
echo "=== EXPERIMENT B: kotlin.native.cacheKind.ohosArm64=none (control) ==="
echo ""

# B1: Clean build
echo "[B1] Clean build with cache disabled..."
./gradlew clean :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache -Pkotlin.native.cacheKind.ohosArm64=none -q 2>/dev/null
SIZE_B_CLEAN=$(measure_size "$ARTIFACT")
cp "$ARTIFACT" /tmp/libkn_B_clean.so
echo "  Clean artifact size: $SIZE_B_CLEAN bytes ($(echo "scale=1; $SIZE_B_CLEAN / 1048576" | bc) MB)"

# B2: Same small change, rebuild
echo "[B2] Making same source change..."
sed -i.bak 's/(HarmonyOS Native)/(HarmonyOS Native v2)/' "$SOURCE_FILE"
./gradlew :composeApp:linkDebugSharedOhosArm64 --no-configuration-cache --no-build-cache -Pkotlin.native.cacheKind.ohosArm64=none -q 2>/dev/null
SIZE_B_INC=$(measure_size "$ARTIFACT")
echo "  Incremental size: $SIZE_B_INC bytes"
GROWTH_B=$((SIZE_B_INC - SIZE_B_CLEAN))
echo "  Growth: ${GROWTH_B} bytes"

# Restore
mv "${SOURCE_FILE}.bak" "$SOURCE_FILE" 2>/dev/null || true
sed -i.bak 's/(HarmonyOS Native v2)/(HarmonyOS Native)/' "$SOURCE_FILE"
rm -f "${SOURCE_FILE}.bak"

# ============================================================
# ANALYSIS: Compare binary sections
# ============================================================
echo ""
echo "=== BINARY SECTION ANALYSIS ==="
echo ""

echo "--- Section sizes (llvm-size) ---"
echo "Incremental clean:"
$LLVM_SIZE /tmp/libkn_A_clean.so
echo "Incremental #1:"
$LLVM_SIZE /tmp/libkn_A_inc1.so
echo "Control (no cache):"
$LLVM_SIZE /tmp/libkn_B_clean.so

echo ""
echo "--- Section header comparison (key sections) ---"
echo "Extracting section sizes from llvm-readelf..."

extract_section_size() {
    local file=$1
    local section=$2
    $LLVM_READELF --sections "$file" 2>/dev/null | grep "\.$section" | awk '{print $6}'
}

for section in text rodata data data.rel.ro debug_info debug_str debug_line symtab strtab; do
    S_A_CLEAN=$(extract_section_size /tmp/libkn_A_clean.so "$section")
    S_A_INC=$(extract_section_size /tmp/libkn_A_inc1.so "$section")
    S_B_CLEAN=$(extract_section_size /tmp/libkn_B_clean.so "$section")
    echo "  .$section: incr_clean=$S_A_CLEAN  incr_inc1=$S_A_INC  control=$S_B_CLEAN"
done

# ============================================================
# SUMMARY
# ============================================================
echo ""
echo "=== SUMMARY ==="
echo ""
echo "Incremental mode (kotlin.incremental.native=true):"
echo "  Clean:       $SIZE_A_CLEAN bytes"
echo "  After edit:  $SIZE_A_INC1 bytes (growth: ${GROWTH_A1} bytes)"
echo ""
echo "Control (cacheKind.ohosArm64=none):"
echo "  Clean:       $SIZE_B_CLEAN bytes"
echo "  After edit:  $SIZE_B_INC bytes (growth: ${GROWTH_B} bytes)"
echo ""
echo "Baseline size difference (incremental vs control): $((SIZE_A_CLEAN - SIZE_B_CLEAN)) bytes ($(echo "scale=1; ($SIZE_A_CLEAN - SIZE_B_CLEAN) * 100 / $SIZE_B_CLEAN" | bc)% larger)"
echo ""
echo "Done. Artifact copies saved in /tmp/libkn_*.so for further analysis."
