#!/bin/bash
# _common.sh — shared helpers for coverage scripts.
# Source this from run-<backend>.sh: `source "$(dirname "$0")/_common.sh"`
#
# Each backend script does: build → run tests → generate report, then prints the report path.

set -uo pipefail

# Resolve worktree + isolated gradle home regardless of where the script is invoked from.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
export GRADLE_USER_HOME="$(dirname "$WT_ROOT")/$(basename "$WT_ROOT")-gradle_home"

GRADLE="./gradlew --no-daemon --console=plain"
REPORT_ROOT="$WT_ROOT/build/reports/coverage"
mkdir -p "$REPORT_ROOT"

# Versions under test (single source of truth in gradle/libs.versions.toml).
read_kover_versions() {
  python3 - "$WT_ROOT/gradle/libs.versions.toml" <<'PY'
import re, sys
for line in open(sys.argv[1]):
    m = re.match(r'^(kotlin|kover)\s*=\s*"([^"]+)"', line)
    if m: print(f"{m.group(1)}={m.group(2)}")
PY
}
while IFS='=' read -r k v; do
  case "$k" in
    kotlin) KOTLIN_VERSION="$v" ;;
    kover)  KOVER_VERSION="$v" ;;
  esac
done < <(read_kover_versions)

log()  { printf '\033[1;34m[coverage]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ok]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }
err()  { printf '\033[1;31m[err]\033[0m %s\n' "$*"; }

banner() {
  printf '\n\033[1;36m══════════════════════════════════════════════════════\n'
  printf '  %s\n' "$*"
  printf '══════════════════════════════════════════════════════\033[0m\n\n'
}

# Run gradle inside the worktree, capturing full output to a log file.
# Usage: gradle_run <log_path> <task> [task...]
gradle_run() {
  local log="$1"; shift
  ( cd "$WT_ROOT" && $GRADLE "$@" ) > "$log" 2>&1
  local rc=$?
  if [ $rc -ne 0 ]; then
    err "Gradle failed (exit $rc). Last 40 lines of $log:"
    tail -40 "$log" >&2
    return $rc
  fi
  return 0
}

print_env() {
  log "Kotlin: $KOTLIN_VERSION | Kover: $KOVER_VERSION"
  log "Worktree: $WT_ROOT"
  log "GRADLE_USER_HOME: $GRADLE_USER_HOME"
  command -v node >/dev/null && log "Node: $(node -v)" || warn "node: NOT FOUND"
}
