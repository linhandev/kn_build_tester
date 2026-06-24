#!/bin/bash
# run-jvm.sh — JVM target coverage via Kover (JetBrains official).
# Kover instruments JVM bytecode at runtime (agent) and is the officially supported path.
# Full flow: build → jvmTest → Kover HTML/XML report.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

banner "JVM target coverage (Kover — official)"

print_env

OUT="$REPORT_ROOT/jvm"
mkdir -p "$OUT"

log "Step 1/3: build + run :library:jvmTest"
gradle_run "$OUT/01-jvmTest.log" :library:jvmTest
ok "jvmTest passed"

log "Step 2/3: generate Kover reports (HTML + XML) — Jvm-specific tasks avoid Android target config"
# Use target-specific Kover tasks so they don't require ANDROID_HOME (which is absent here).
gradle_run "$OUT/02-koverReport.log" :library:koverHtmlReportJvm :library:koverXmlReportJvm
ok "Kover reports generated"

HTML="$WT_ROOT/library/build/reports/kover/htmlJvm/index.html"
XML="$WT_ROOT/library/build/reports/kover/reportJvm.xml"

log "Step 3/3: summarize"
if [ -f "$HTML" ]; then
  ok "HTML report: file://$HTML"
else
  err "HTML report not found at $HTML"
fi
[ -f "$XML" ] && ok "XML report:  $XML"

# JaCoCo XML stores coverage in <counter> elements (missed/covered), not line-rate attrs.
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

banner "JVM DONE — official Kover path works ✅"
