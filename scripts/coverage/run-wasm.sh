#!/bin/bash
# run-wasm.sh — Wasm (wasmJs) target coverage.
#
# FINDINGS (this is the empirical result of this survey):
#   1. Kotlin 2.4 DOES emit a wasm source map (.wasm.map) whose `sources` point at the .kt files
#      — so remapping wasm coverage back to .kt is *theoretically* possible.
#   2. BUT Node 24 has NO CLI flag to enable wasm code coverage (--experimental-wasm-code-coverage
#      was never exposed / removed). `node --help` confirms only --disable-wasm-trap-handler.
#   3. `NODE_V8_COVERAGE` therefore collects coverage ONLY for the .mjs JS glue, NOT for the
#      .wasm module's functions/blocks — so c8 cannot remap to .kt (the .mjs has no .kt mapping).
#   4. The only path to wasm block coverage is the V8 Inspector API (Profiler.startPreciseCoverage
#      with detailed=true), and even then no mainstream tool (c8/istanbul) consumes wasm source maps.
#
# Conclusion: Kotlin/Wasm has NO working, officially-supported coverage path as of Kotlin 2.4 / Node 24.
# This script verifies the source map exists and records the gap, rather than pretending to produce a report.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "Wasm target coverage (no JB integration, no working path — recording gap)"

print_env

OUT="$REPORT_ROOT/wasm"
mkdir -p "$OUT"

# 1. Build + run tests (verifies the toolchain works; V8 coverage of .mjs is a side effect).
log "Step 1/3: build + :library:wasmJsNodeTest"
gradle_run "$OUT/01-wasmJsNodeTest.log" :library:wasmJsNodeTest
ok "wasmJsNodeTest passed (toolchain OK)"

# 2. Locate the wasm source map and verify it maps to .kt.
log "Step 2/3: verify wasm source map presence and .kt sources"
WASM_PKG="$WT_ROOT/build/wasm/packages/multiplatform-library-template-library-test/kotlin"
WASM_MAP="$WASM_PKG/multiplatform-library-template-library-test.wasm.map"

if [ -f "$WASM_MAP" ]; then
  ok "wasm source map: $WASM_MAP"
  log "source map .kt sources:"
  python3 - "$WASM_MAP" <<'PY' | sed 's/^/    /'
import json, sys
d = json.load(open(sys.argv[1]))
srcs = [s for s in d.get('sources', []) if s.endswith('.kt')]
print(f"{len(srcs)} .kt sources mapped:")
for s in srcs: print(f"      {s}")
PY
else
  err "wasm source map NOT found at $WASM_MAP"
fi

# 3. Confirm Node has no wasm-coverage CLI flag.
log "Step 3/3: confirm Node has no wasm coverage flag"
if node --help 2>&1 | grep -qi 'wasm-code-coverage'; then
  ok "Node exposes a wasm coverage flag (unexpected — re-evaluate)"
else
  warn "Node $(node -v) has NO wasm coverage CLI flag — only --disable-wasm-trap-handler"
  warn "NODE_V8_COVERAGE collects .mjs glue only, not .wasm blocks → no .kt remap possible"
fi

banner "WASM DONE — NO working coverage path (source map exists, collection gap)"