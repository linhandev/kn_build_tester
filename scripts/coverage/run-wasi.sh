#!/bin/bash
# run-wasi.sh — WasmWasi target coverage.
#
# Same V8 coverage path as wasmJs: %DebugCollectWasmCoverage is V8-level and
# target-agnostic, so it collects wasmWasi block coverage the same way. The only
# differences from run-wasm.sh: the wasmWasi test mjs uses WASI (needs --allow-wasi)
# and exports startUnitTests instead of auto-calling it, so it needs
# wasm-wasi-collector.mjs. The remapper (wasm-remap.py) is shared unchanged.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "WasmWasi target coverage (Node 25 + %DebugCollectWasmCoverage + shared remapper)"

print_env

OUT="$REPORT_ROOT/wasi"
mkdir -p "$OUT"

# 1. Build the wasmWasi test executable (.wasm + .wasm.map + test mjs).
log "Step 1/4: build wasmWasi test executable"
gradle_run "$OUT/01-wasiBuild.log" :library:compileTestDevelopmentExecutableKotlinWasmWasi
ok "wasmWasi test executable built"

KDIR="$WT_ROOT/library/build/compileSync/wasmWasi/test/testDevelopmentExecutable/kotlin"
WASM="$KDIR/multiplatform-library-template-library-test.wasm"
WASM_MAP="$KDIR/multiplatform-library-template-library-test.wasm.map"
MJS="$KDIR/multiplatform-library-template-library-test.mjs"
[ -f "$WASM" ]     || { err "wasm binary not found: $WASM"; exit 1; }
[ -f "$WASM_MAP" ] || { err "wasm source map not found: $WASM_MAP"; exit 1; }

# 2. Locate Node 25.
log "Step 2/4: locate Node 25"
NODE25=""
for d in "$GRADLE_USER_HOME/nodejs" "$HOME/.gradle/nodejs"; do
  c=$(ls -d "$d"/node-v25.*-darwin-*/bin/node 2>/dev/null | head -1)
  [ -n "$c" ] && NODE25="$c" && break
done
[ -n "$NODE25" ] || { err "Node 25 not found (gradle nodejs cache)."; exit 1; }
ok "Node 25: $($NODE25 -v)"

# 3. Collect wasm block coverage. wasmWasi needs --allow-wasi (WASI is experimental in Node 25).
log "Step 3/4: collect wasm coverage (%DebugCollectWasmCoverage, WASI)"
COV_JSON="$OUT/wasi-cov.json"
"$NODE25" --allow-wasi --allow-natives-syntax --wasm-code-coverage --no-wasm-lazy-compilation \
  "$SCRIPT_DIR/wasm-wasi-collector.mjs" "$MJS" "$COV_JSON" > "$OUT/02-collector.log" 2>&1
tail -1 "$OUT/02-collector.log"
ok "coverage collected: $COV_JSON"

# 4. Remap to .kt source (shared remapper, auto-detects code section base).
log "Step 4/4: remap to .kt source"
python3 "$SCRIPT_DIR/wasm-remap.py" "$WASM_MAP" "$WASM" "$COV_JSON" --lcov "$OUT/wasi.lcov.info" \
  | tee "$OUT/03-remap.log"
ok "lcov report: $OUT/wasi.lcov.info"

banner "WASMWASI DONE — coverage works on Node 25 via %DebugCollectWasmCoverage + shared remapper ✅"
