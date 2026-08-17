#!/bin/bash
# Patch konan.properties to set OHOS arm64 CPU features.
# Usage: ./patch-konan-properties.sh <feature-set>
#   <feature-set> = "issue"     -> +fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a
#   <feature-set> = "no-fix"    -> +fp-armv8,+neon,+reserve-x28,+v8a  (remove the trigger)
#   <feature-set> = "fix-only"  -> +fix-cortex-a53-835769             (isolate the trigger)
#   <feature-set> = "default"   -> restore CPF default features
set -euo pipefail

MODE="${1:-issue}"

# Locate the prebuilt konan.properties
KONAN_HOME="${KONAN_HOME:-$HOME/.konan}"
PROPS=$(find "$KONAN_HOME" -path "*/konan/konan.properties" -name "konan.properties" 2>/dev/null | sort -V | tail -1)
if [ -z "$PROPS" ]; then
  echo "ERROR: konan.properties not found under $KONAN_HOME" >&2
  exit 1
fi
echo "Using: $PROPS"

# Backup once
[ -f "$PROPS.bak" ] || cp "$PROPS" "$PROPS.bak"

case "$MODE" in
  issue)
    FEATURES="+fix-cortex-a53-835769,+fp-armv8,+neon,+reserve-x28,+v8a"
    ;;
  no-fix)
    FEATURES="+fp-armv8,+neon,+reserve-x28,+v8a"
    ;;
  fix-only)
    FEATURES="+fix-cortex-a53-835769"
    ;;
  default)
    cp "$PROPS.bak" "$PROPS"
    echo "Restored default konan.properties"
    exit 0
    ;;
  *)
    echo "Usage: $0 {issue|no-fix|fix-only|default}" >&2
    exit 1
    ;;
esac

# Replace the targetCpuFeatures.ohos_arm64 line
if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' "s|^targetCpuFeatures\\.ohos_arm64 = .*|targetCpuFeatures.ohos_arm64 = $FEATURES|" "$PROPS"
else
  sed -i    "s|^targetCpuFeatures\\.ohos_arm64 = .*|targetCpuFeatures.ohos_arm64 = $FEATURES|" "$PROPS"
fi

echo "Set targetCpuFeatures.ohos_arm64 = $FEATURES"
grep "targetCpuFeatures.ohos_arm64" "$PROPS"
