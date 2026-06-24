#!/bin/bash
# run-android-device.sh — Android INSTRUMENTED test coverage for the KMP LIBRARY module.
#
# The point is to cover the library's own code (Calculator etc.) from tests running ON the
# device/emulator — distinct from run-android.sh (host unit test on the build JVM).
#
# Key AGP9 KMP facts (all hard-won):
#   * The coverage switch is `enableCoverage` on `withDeviceTest { }` (NOT `withDeviceTestBuilder`),
#     because only KotlinMultiplatformAndroidDeviceTest exposes it — the builder's action type doesn't.
#   * AGP9 KMP androidLibrary does NOT auto-create a coverage report task, so we register a
#     JaCoCoReport task (createAndroidDeviceCoverageReport) in library/build.gradle.kts.
#   * The .ec lands in build/outputs/code_coverage/androidDeviceTest/connected/<avd>/coverage.ec.
#   * classDirectories must point at the ORIGINAL androidMain classes (classes/kotlin/android/main)
#     for line-number mapping — not the instrumented copy.
#
# Requires: a booted emulator visible to `adb devices`.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "Android LIBRARY device coverage (instrumented test on emulator)"

print_env
OUT="$REPORT_ROOT/android-device"
mkdir -p "$OUT"

# 0. pre-flight: emulator online + booted.
log "Step 0: pre-flight — device"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
if ! adb get-state >/dev/null 2>&1; then
  err "No device/emulator visible to adb. Boot one first:"
  echo "  export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools"
  echo "  \$ANDROID_HOME/emulator/emulator -avd Small_Phone -no-window -no-audio &"
  echo "  adb wait-for-device; adb shell 'while [[ -z \$(getprop sys.boot_completed) ]]; do sleep 1; done'"
  exit 3
fi
booted=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')
if [ "$booted" != "1" ]; then
  err "Device visible but not booted (sys.boot_completed != 1). Wait for boot."
  exit 3
fi
ok "device online and booted: $(adb get-state)"

# 1. Run library instrumented tests on the device (enableCoverage=true instruments androidMain).
log "Step 1/3: :library:connectedAndroidDeviceTest (withDeviceTest { enableCoverage = true })"
gradle_run "$OUT/01-connected.log" :library:connectedAndroidDeviceTest
ok "instrumented tests passed"

# 2. Generate the JaCoCo report from the pulled .ec.
log "Step 2/3: :library:createAndroidDeviceCoverageReport (JaCoCoReport task)"
gradle_run "$OUT/02-report.log" :library:createAndroidDeviceCoverageReport
ok "coverage report generated"

XML="$WT_ROOT/library/build/reports/jacoco/createAndroidDeviceCoverageReport/createAndroidDeviceCoverageReport.xml"
HTML="$WT_ROOT/library/build/reports/jacoco/createAndroidDeviceCoverageReport/html/index.html"

log "Step 3/3: summarize"
[ -f "$HTML" ] && ok "HTML report: file://$HTML" || err "HTML not found: $HTML"
[ -f "$XML" ]  && ok "XML report:  $XML"  || { err "XML not found: $XML"; exit 1; }

log "Coverage (library androidMain, from XML):"
python3 - "$XML" <<'PY' | sed 's/^/    /'
import xml.etree.ElementTree as ET, sys
r = ET.parse(sys.argv[1]).getroot()
for c in r.findall('counter'):
    m, cov = int(c.get('missed')), int(c.get('covered'))
    tot = m + cov
    pct = 100.0 * cov / tot if tot else 0
    print(f"{c.get('type'):11s} {cov}/{tot}  {pct:5.1f}%")
print("--- our classes (branch/func/line) ---")
for cls in r.iter('class'):
    name = cls.get('name','').split('/')[-1]
    if any(k in name for k in ('Calculator','Fibi','fibiprops')):
        def g(t):
            c = cls.find(f"counter[@type='{t}']")
            return (c.get('missed'),c.get('covered')) if c is not None else ('?','?')
        print(f"  {name:28s} branch={g('BRANCH')} method={g('METHOD')} line={g('LINE')}")
PY

banner "ANDROID DEVICE DONE — library androidMain covered on emulator"
