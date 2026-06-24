#!/bin/bash
# run-js.sh — JS target coverage via V8 native coverage + c8 + source-map remap.
# JetBrains has NO official coverage integration for JS. This is the standard Node-ecosystem path:
#   NODE_V8_COVERAGE lets V8 write byte-offset coverage on exit → c8 consumes it,
#   uses the compiler-generated source map to remap .js coverage back onto .kt source.
# Full flow: build → jsNodeTest (with V8 coverage) → c8 HTML/lcov report.
#
# NOTE: source-map remap to .kt is the known weak point — this script records whether it works.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "JS target coverage (V8 + c8 — community, no JB integration)"

print_env

OUT="$REPORT_ROOT/js"
mkdir -p "$OUT"

command -v node >/dev/null || { err "node not found"; exit 1; }
command -v npx >/dev/null || { err "npx not found"; exit 1; }

# 1. Build + run tests with V8 coverage collection enabled.
log "Step 1/3: build + :library:jsNodeTest with NODE_V8_COVERAGE"
RAW_DIR="$WT_ROOT/library/build/v8-coverage-js"
rm -rf "$RAW_DIR"; mkdir -p "$RAW_DIR"

# Gradle's jsNodeTest spawns node; the build.gradle.kts testTask block propagates NODE_V8_COVERAGE
# to the forked node process (Gradle does not forward arbitrary env vars automatically).
export NODE_V8_COVERAGE="$RAW_DIR"
# Force a clean re-run so the node process actually executes with the env var set this run.
gradle_run "$OUT/01-jsNodeTest.log" :library:clean :library:jsNodeTest
ok "jsNodeTest passed"
# Node writes V8 coverage asynchronously on graceful exit; wait until files stabilize.
sync
# poll up to ~10s for at least one coverage file to stop growing in count
for _ in 1 2 3 4 5 6 7 8 9 10; do
  n=$(ls "$RAW_DIR" 2>/dev/null | wc -l | tr -d ' ')
  sleep 1
  n2=$(ls "$RAW_DIR" 2>/dev/null | wc -l | tr -d ' ')
  [ "$n" = "$n2" ] && [ "$n" -gt 0 ] && break
done
log "V8 raw coverage files: $(ls "$RAW_DIR" 2>/dev/null | wc -l | tr -d ' ')"

# 2. Feed V8 coverage to c8, remap via compiler-generated source map back to .kt.
log "Step 2/3: c8 report (V8 coverage → HTML/lcov, source-map remap to .kt)"
C8="$WT_ROOT/node_modules/.bin/c8"
[ -x "$C8" ] || { err "c8 not installed. Run: npm install --save-dev c8"; exit 1; }
# c8 resolves source maps relative to cwd, so run from the worktree root.
( cd "$WT_ROOT" && "$C8" report \
  --temp-directory "$RAW_DIR" \
  --clean false \
  --reporter html \
  --reporter lcov \
  --reporter text \
  --report-dir "$OUT/c8" \
  > "$OUT/02-c8report.log" 2>&1 ) || {
    warn "c8 report returned non-zero — see $OUT/02-c8report.log"
    tail -20 "$OUT/02-c8report.log" >&2
  }

log "Step 3/3: summarize"
HTML="$OUT/c8/index.html"
if [ -f "$HTML" ]; then
  ok "HTML report: file://$HTML"
else
  err "HTML report not generated. Inspect raw V8 coverage: $RAW_DIR"
fi

# Show the remapped .kt rows (our sources only) so coverage is visible without opening the report.
log "Remapped coverage (our .kt sources, remapped from V8 via source map):"
grep -E 'Calculator|CustomFibi|fibiprops|All files|File|% Stmts' "$OUT/02-c8report.log" | head -8 | sed 's/^/    /' || true

banner "JS DONE — V8 + c8 + source-map remap to .kt"
