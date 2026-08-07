#!/usr/bin/env bash
# Build → install → launch → assert alive + on-demand load (k2n then n2k).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
cd "$ROOT"

HDC="${DEVECO_HDC:-/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc}"
CLANG="${OHOS_CLANG:-/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang}"
SYSROOT="${OHOS_SYSROOT:-/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot}"
READELF="${READELF:-/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/llvm-readelf}"
TARGET_ID="${TARGET_ID:-23E0123523000348}"
BUNDLE="com.kotlin.demo"
ABILITY="EntryAbility"

hdc() {
  "$HDC" -t "$TARGET_ID" "$@"
}

echo "== device $TARGET_ID =="
hdc list targets 2>/dev/null | tr -d '\r' | grep -qx "$TARGET_ID" \
  || { echo "FAIL: device $TARGET_ID not online"; exit 1; }

echo "== build libmainstub.so =="
mkdir -p harmonyApp/entry/libs/arm64-v8a
"$CLANG" --target=aarch64-linux-ohos --sysroot="$SYSROOT" -shared -fPIC -O2 \
  -Wl,-soname,libmainstub.so \
  -o harmonyApp/entry/libs/arm64-v8a/libmainstub.so \
  harmonyApp/entry/src/main/cpp/main_stub.c

echo "== link + HAP + install + launch =="
before_crash="$(hdc shell "ls -t /data/log/faultlog/faultlogger/" 2>/dev/null | tr -d '\r' | grep -F "$BUNDLE" | head -1 || true)"
hdc shell aa force-stop "$BUNDLE" >/dev/null 2>&1 || true
hdc shell hilog -r >/dev/null 2>&1 || true
./gradlew :kotlinApp:startHarmonyAppRelease

sleep 3
pid="$(hdc shell pidof "$BUNDLE" 2>/dev/null | tr -d '\r' || true)"
[[ -n "$pid" ]] || { echo "FAIL: process not alive"; exit 1; }
echo "OK: alive pid=$pid"

after_crash="$(hdc shell "ls -t /data/log/faultlog/faultlogger/" 2>/dev/null | tr -d '\r' | grep -F "$BUNDLE" | head -1 || true)"
if [[ -n "$after_crash" && "$before_crash" != "$after_crash" ]]; then
  echo "FAIL: new faultlog $after_crash"
  exit 1
fi
echo "OK: no new cppcrash/faultlog"

echo "== entry must NOT NEEDED business SOs =="
ENTRY_SO="$ROOT/harmonyApp/entry/build/default/intermediates/cmake/default/obj/arm64-v8a/libentry.so"
needed="$("$READELF" -d "$ENTRY_SO" 2>/dev/null | rg 'NEEDED' || true)"
echo "$needed" | rg -q 'libk2n|libn2k' && { echo "FAIL: entry links business SO"; echo "$needed"; exit 1; }
echo "$needed" | rg -q 'libruntime|libstd' || { echo "FAIL: entry missing runtime/std"; echo "$needed"; exit 1; }
echo "OK: entry NEEDED only runtime+std (no k2n/n2k)"

ondemand_log() {
  LC_ALL=C hdc shell hilog -x 2>/dev/null | LC_ALL=C tr -d '\r' | rg " $pid .*ondemand" || true
}

# /proc/<pid>/maps — prove which business SOs are mapped.
maps_has() {
  local so="$1"
  LC_ALL=C hdc shell "cat /proc/$pid/maps" 2>/dev/null | LC_ALL=C tr -d '\r' | rg -q "$so"
}

maps_biz() {
  LC_ALL=C hdc shell "cat /proc/$pid/maps" 2>/dev/null | LC_ALL=C tr -d '\r' \
    | rg 'lib(k2n|n2k|runtime|std|entry|mainstub)\.so' | sed 's/^/  /' || true
}

echo "== on-demand: first module (aboutToAppear → libk2n) =="
for _ in 1 2 3 4 5; do
  log1="$(ondemand_log)"
  echo "$log1" | rg -q 'called libk2n\.so!kn_k2n_run' && break
  sleep 1
done
echo "$log1" | rg -q 'called libk2n\.so!kn_k2n_run' \
  || { echo "FAIL: k2n on-demand missing"; echo "$log1"; exit 1; }
echo "$log1" | rg -q 'dlopen libk2n|loadFirstModule failed' \
  && { echo "FAIL: k2n load error in log"; echo "$log1"; exit 1; }
echo "OK: libk2n.so called (hilog)"

echo "== maps after k2n (pid=$pid): n2k must NOT be mapped =="
maps_biz
maps_has 'libk2n\.so' || { echo "FAIL: libk2n.so missing from maps after first load"; exit 1; }
if maps_has 'libn2k\.so'; then
  echo "FAIL: libn2k.so already in maps before tap (not on-demand)"
  exit 1
fi
echo "OK: maps has libk2n, no libn2k"

echo "== on-demand: second module (tap → libn2k) =="
hdc shell uitest uiInput click 540 1200 >/dev/null 2>&1 || true
for _ in 1 2 3 4 5; do
  log2="$(ondemand_log)"
  echo "$log2" | rg -q 'called libn2k\.so!kn_n2k_run' && break
  sleep 1
done
echo "$log2" | rg -q 'called libn2k\.so!kn_n2k_run' \
  || { echo "FAIL: n2k on-demand missing"; echo "$log2"; exit 1; }
echo "OK: libn2k.so called (hilog)"

echo "== maps after n2k (pid=$pid) =="
maps_biz
maps_has 'libn2k\.so' || { echo "FAIL: libn2k.so still missing from maps after tap"; exit 1; }
echo "OK: libn2k.so now mapped"

pid2="$(hdc shell pidof "$BUNDLE" 2>/dev/null | tr -d '\r' || true)"
[[ -n "$pid2" ]] || { echo "FAIL: died after n2k load"; exit 1; }

echo
echo "PASS: alive + no crash + maps prove on-demand (n2k absent until tap)"
