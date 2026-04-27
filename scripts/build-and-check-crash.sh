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
  # Fault log generation is asynchronous after process abort; poll briefly.
  after=""
  for _ in {1..10}; do
    sleep 2
    after="$(hdc shell "ls -t /data/log/faultlog/faultlogger/" 2>/dev/null | tr -d '\r' | grep -F "$bundle" | head -1 || true)"
    [[ -n "$after" && "$before" != "$after" ]] && break
  done

  if [[ -n "$after" && "$before" != "$after" ]]; then
    mkdir -p build/crash-check
    hdc file recv "/data/log/faultlog/faultlogger/$after" "build/crash-check/" >/dev/null
    echo "New crash log: build/crash-check/$after"
    rg '^Timestamp:|^Reason:' "build/crash-check/$after" || true
    if rg -q '^Reason:Signal:|^Reason:.*[Aa]bort' "build/crash-check/$after"; then
      echo "Crash detected."
      exit 1
    fi
  fi
fi
