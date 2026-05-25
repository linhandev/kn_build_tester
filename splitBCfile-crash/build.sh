#!/bin/bash
# Reproduction script for ALI-33: splitBCfile=2 compile crash on OHOS arm64
# Usage: ./build.sh [no-split | split1 | split2 | all]

set -e

KONAN_HOME="$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04"
KONANC="$KONAN_HOME/bin/konanc"
TARGET="ohos_arm64"
SRC_DIR="src"
OUT_DIR="build"
SOURCE="$SRC_DIR/main.kt"

if [ ! -f "$KONANC" ]; then
    echo "ERROR: konanc not found at $KONANC"
    echo "Install kotlin-native-prebuilt-macos-aarch64-2.2.21-0.3.0-04 first"
    exit 1
fi

mkdir -p "$OUT_DIR"

run_compile() {
    local label="$1"
    local extra_flags="$2"
    local output_name="$3"

    echo "============================================"
    echo "TEST: $label"
    echo "FLAGS: $extra_flags"
    echo "OUTPUT: $OUT_DIR/$output_name"
    echo "============================================"

    local cmd="$KONANC -target $TARGET -produce program -o $OUT_DIR/$output_name $extra_flags $SOURCE"
    echo "CMD: $cmd"
    echo "---"

    if $cmd 2>&1; then
        echo "RESULT: SUCCESS (exit code: $?)"
    else
        local exit_code=$?
        echo "RESULT: FAILED (exit code: $exit_code)"
    fi
    echo ""
}

MODE="${1:-all}"

case "$MODE" in
    no-split)
        run_compile "Baseline (no splitBCfile)" "" "baseline"
        ;;
    split1)
        run_compile "splitBCfile=1" "-Xbinary=splitBCfile=1" "split1"
        ;;
    split2)
        run_compile "splitBCfile=2" "-Xbinary=splitBCfile=2" "split2"
        ;;
    all)
        run_compile "Baseline (no splitBCfile)" "" "baseline"
        run_compile "splitBCfile=1" "-Xbinary=splitBCfile=1" "split1"
        run_compile "splitBCfile=2" "-Xbinary=splitBCfile=2" "split2"
        ;;
    *)
        echo "Usage: $0 [no-split | split1 | split2 | all]"
        exit 1
        ;;
esac
