#!/bin/bash
# run-wasm.sh — Wasm (wasmJs) target coverage.
#
# Unlike JVM/Android (Kover) and JS (c8), wasmJs has NO off-the-shelf coverage path:
# c8/istanbul do not consume wasm source maps. This script drives the full flow that
# DOES work on Node 25+:
#   build → %DebugCollectWasmCoverage (Node 25 --wasm-code-coverage) → custom remapper → per-.kt report
#
# Caveat: the Kotlin/Wasm source map is sparse (function-granularity), so line-level
# resolution is coarse; file-level and function-level coverage are accurate.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "Wasm target coverage (Node 25 + %DebugCollectWasmCoverage + custom remapper)"

print_env

OUT="$REPORT_ROOT/wasm"
mkdir -p "$OUT"

# 1. Build the wasm test package (produces .wasm + .wasm.map + runUnitTests.mjs).
log "Step 1/4: build wasmJs test executable"
gradle_run "$OUT/01-wasmJsBuild.log" :library:wasmJsNodeTest
ok "wasmJs test package built"

TEST_PKG="$WT_ROOT/build/wasm/packages/multiplatform-library-template-library-test/kotlin"
WASM="$TEST_PKG/multiplatform-library-template-library-test.wasm"
WASM_MAP="$TEST_PKG/multiplatform-library-template-library-test.wasm.map"
[ -f "$WASM" ] || { err "wasm binary not found: $WASM"; exit 1; }
[ -f "$WASM_MAP" ] || { err "wasm source map not found: $WASM_MAP"; exit 1; }

# 2. Locate Node 25 (pinned in library/build.gradle.kts via the nodejs plugin).
log "Step 2/4: locate Node 25"
NODE25=""
for d in "$GRADLE_USER_HOME/nodejs" "$HOME/.gradle/nodejs"; do
  c=$(ls -d "$d"/node-v25.*-darwin-*/bin/node 2>/dev/null | head -1)
  [ -n "$c" ] && NODE25="$c" && break
done
[ -n "$NODE25" ] || { err "Node 25 not found (gradle nodejs cache). Build wasmJsNodeTest with the pinned version=25.x first."; exit 1; }
ok "Node 25: $($NODE25 -v)"

# 3. Collect wasm block coverage via %DebugCollectWasmCoverage.
log "Step 3/4: collect wasm coverage (%DebugCollectWasmCoverage)"
COV_JSON="$OUT/wasm-cov.json"
"$NODE25" --allow-natives-syntax --wasm-code-coverage --no-wasm-lazy-compilation \
  "$SCRIPT_DIR/wasm-kotlin-collector.mjs" "$TEST_PKG" "$COV_JSON" > "$OUT/02-collector.log" 2>&1
tail -1 "$OUT/02-collector.log"
ok "coverage collected: $COV_JSON"

# 4. Remap to .kt source.
log "Step 4/4: remap to .kt source"
python3 "$SCRIPT_DIR/wasm-remap.py" "$WASM_MAP" "$WASM" "$COV_JSON" --lcov "$OUT/wasm.lcov.info" \
  | tee "$OUT/03-remap.log"
ok "lcov report: $OUT/wasm.lcov.info"

banner "WASM DONE — coverage works on Node 25 via %DebugCollectWasmCoverage + custom remapper ✅"
