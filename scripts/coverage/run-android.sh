#!/bin/bash
# run-android.sh — Android target coverage via Kover + AGP enableUnitTestCoverage (host unit tests).
# This is the official path: AGP offline-instruments debug classes, host unit tests run on the build
# JVM, Kover/JaCoCo aggregates the .exec data into a report.
#
# Per user instruction: do NOT auto-skip. Run until a dependency is missing, then STOP and report
# exactly what's missing so the user can install it before continuing.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "Android target coverage (Kover + AGP enableUnitTestCoverage)"

print_env

OUT="$REPORT_ROOT/android"
mkdir -p "$OUT"

# 0. Pre-flight: Android SDK must be present.
log "Step 0: pre-flight — Android SDK"
if [ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]; then
  err "ANDROID_HOME / ANDROID_SDK_ROOT is not set."
  echo ""
  echo "Install the Android command-line tools and set ANDROID_HOME, then re-run."
  echo "  1. Download cmdline-tools from https://developer.android.com/studio#command-line-tools-only"
  echo "  2. export ANDROID_HOME=\$HOME/Library/Android/sdk"
  echo "  3. sdkmanager \"platform-tools\" \"platforms;android-36\" \"build-tools;36.0.0\""
  echo ""
  echo "Once installed, tell me and I'll continue from here."
  exit 3
fi
ok "ANDROID_HOME=$ANDROID_HOME"

# 1. Build + run Android host unit tests with coverage enabled.
log "Step 1/3: build + :library:testDebugUnitTest (with enableUnitTestCoverage)"
# enableUnitTestCoverage is wired in library/build.gradle.kts androidLibrary block.
gradle_run "$OUT/01-testDebugUnitTest.log" :library:testDebugUnitTest
ok "testDebugUnitTest passed"

# 2. Kover Android report.
log "Step 2/3: generate Kover Android report"
gradle_run "$OUT/02-koverReport.log" :library:koverHtmlReportAndroid :library:koverXmlReportAndroid
ok "Kover Android reports generated"

HTML="$WT_ROOT/library/build/reports/kover/htmlAndroid/index.html"
XML="$WT_ROOT/library/build/reports/kover/reportAndroid.xml"

log "Step 3/3: summarize"
[ -f "$HTML" ] && ok "HTML report: file://$HTML" || err "HTML not found: $HTML"
[ -f "$XML" ]  && ok "XML report:  $XML"  || err "XML not found: $XML"
if [ -f "$XML" ]; then
  log "Coverage (project, from XML):"
  python3 - "$XML" <<'PY' | sed 's/^/    /'
import xml.etree.ElementTree as ET, sys
r = ET.parse(sys.argv[1]).getroot()
for c in r.findall('counter'):
    m, cov = int(c.get('missed')), int(c.get('covered'))
    tot = m + cov
    pct = 100.0 * cov / tot if tot else 0
    print(f"{c.get('type'):11s} {cov}/{tot}  {pct:5.1f}%")
PY
fi

banner "ANDROID DONE — official Kover + AGP path"
