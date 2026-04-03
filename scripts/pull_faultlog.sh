#!/usr/bin/env bash
# List and copy faultlogger entries from a connected device (requires hdc).
# Typical relocation failure lines look like:
#   relocating failed:symbol not found dso=... s=knifunptr_..._OH_AVBuffer_GetAddr
set -euo pipefail
REMOTE_DIR="/data/log/faultlog/faultlogger"
OUT="${1:-./faultlog_out}"

if ! command -v hdc >/dev/null 2>&1; then
  echo "hdc not found in PATH" >&2
  exit 1
fi

echo "=== hdc target list ==="
hdc list targets || true

echo "=== ls ${REMOTE_DIR} ==="
hdc shell "ls -la ${REMOTE_DIR}" || true

mkdir -p "${OUT}"
echo "=== recv ${REMOTE_DIR} -> ${OUT} ==="
hdc file recv "${REMOTE_DIR}" "${OUT}" || {
  echo "recv failed (device offline or path permission)" >&2
  exit 1
}

echo "Done. Inspect: ${OUT}"
