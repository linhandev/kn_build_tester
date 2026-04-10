#!/usr/bin/env bash
# Automatic check that OH_LOG_PrintMsg wrap sample produced expected HiLog lines (device only).
# Expected line count is read from #define LOG_HOOK_SAMPLE_LINES in log_caller.c.
#
# Usage:
#   ASSERT_LOG_HOOK_STRICT=1 ./scripts/assert-log-hook-device.sh
#     — after install/launch (e.g. from build-and-check-crash.sh); fail if lines missing.
#   ./scripts/assert-log-hook-device.sh
#     — if no matching HiLog at all, skip (exit 0); use strict mode from the full build script.
set -euo pipefail
_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
_root="$(cd "$_script_dir/.." && pwd -P)"
_caller="$_root/harmonyApp/entry/src/main/cpp/log_caller.c"

if ! command -v hdc >/dev/null 2>&1; then
  echo "assert-log-hook: hdc not found, skip"
  exit 0
fi
if ! hdc list targets 2>/dev/null | tr -d '\r' | grep -q .; then
  echo "assert-log-hook: no device target, skip"
  exit 0
fi

expected="$(sed -n 's/^#define LOG_HOOK_SAMPLE_LINES[[:space:]]*//p' "$_caller" | tr -d ' \r' | head -1)"
[[ -n "${expected:-}" ]] || expected=5
[[ "$expected" =~ ^[0-9]+$ ]] || { echo "assert-log-hook: bad LOG_HOOK_SAMPLE_LINES in log_caller.c" >&2; exit 1; }

strict="${ASSERT_LOG_HOOK_STRICT:-}"

log_out=""
found_ec=0
found_wrap=0
for _attempt in 1 2 3 4 5; do
  log_out=$(hdc shell "hilog -T LogHookTest -x" 2>/dev/null | tr -d '\r' || true)
  found_ec=$(printf '%s\n' "$log_out" | grep -c 'entry_caller line' || true)
  found_wrap=$(printf '%s\n' "$log_out" | grep -c '\[wrap #' || true)
  if [[ "$found_ec" -ge "$expected" && "$found_wrap" -ge "$expected" ]]; then
    echo "assert-log-hook: OK ($found_ec lines with entry_caller, $found_wrap with [wrap #], expected >= $expected)"
    exit 0
  fi
  sleep 2
done

if [[ "$strict" != "1" && "$found_ec" -eq 0 && "$found_wrap" -eq 0 ]]; then
  echo "assert-log-hook: skip (no LogHookTest HiLog lines; run ./scripts/build-and-check-crash.sh for a strict check after install/launch)"
  exit 0
fi

echo "assert-log-hook: FAILED — expected >= $expected lines with 'entry_caller line' and >= $expected with '[wrap #' in hilog -T LogHookTest" >&2
echo "  (set ASSERT_LOG_HOOK_STRICT=0 and empty logs to skip; build script uses STRICT=1)" >&2
echo "  got entry_caller matches: $found_ec, [wrap # matches: $found_wrap" >&2
echo "  last hilog capture (tail):" >&2
printf '%s\n' "$log_out" | tail -40 >&2
exit 1
