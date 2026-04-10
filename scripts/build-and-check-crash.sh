#!/usr/bin/env bash
set -euo pipefail
# Physical repo root (avoids symlink / cwd edge cases for Gradle and hvigor).
_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."
bundle="$(grep '"bundleName"' harmonyApp/AppScope/app.json5 | sed -n 's/.*"bundleName"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
[[ -n "$bundle" ]] || { echo "error: bundleName not found in harmonyApp/AppScope/app.json5" >&2; exit 1; }
hdc uninstall "$bundle" 2>/dev/null || true

before=""
if command -v hdc >/dev/null 2>&1 && hdc list targets 2>/dev/null | tr -d '\r' | grep -q .; then
  before="$(hdc shell "ls -t /data/log/faultlog/faultlogger/" 2>/dev/null | tr -d '\r' | grep -F "$bundle" | head -1 || true)"
fi

./gradlew :kotlinApp:startHarmonyAppDebug --rerun-tasks --no-daemon

if command -v hdc >/dev/null 2>&1 && hdc list targets 2>/dev/null | tr -d '\r' | grep -q .; then
  sleep 3
  # Fails the script if wrap sample did not emit expected HiLog lines (see bridge.md, log_caller.c).
  ASSERT_LOG_HOOK_STRICT=1 "$_script_dir/assert-log-hook-device.sh"
  after="$(hdc shell "ls -t /data/log/faultlog/faultlogger/" 2>/dev/null | tr -d '\r' | grep -F "$bundle" | head -1 || true)"
  if [[ -n "$after" && "$before" != "$after" ]]; then
    mkdir -p build/crash-check
    hdc file recv "/data/log/faultlog/faultlogger/$after" "build/crash-check/"
    if grep -qE '^Reason:Signal:|^Reason:.*[Aa]bort' "build/crash-check/$after"; then
      echo "Crash detected:"
      tail -n +19 "build/crash-check/$after" | head -n 50 >&2
      exit 1
    fi
  fi
fi
