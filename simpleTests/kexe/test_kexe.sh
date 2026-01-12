#!/bin/bash
# Minimal GCOV test for Kotlin Native
set -e

COMPILER="/Users/hl/git/kmp/KuiklyBase-kotlin/kotlin-native/dist/bin/kotlinc-native"
LLVM_COV="/Users/hl/.konan/dependencies/llvm-1201-macos-aarch64/bin/llvm-cov"

# Create coverage output directory
mkdir -p coverage

echo "=== Compiling ==="
$COMPILER test_coverage.kt -target ohos_arm64 -o test -g -Xbinary=coverage=true

echo "=== Deploying ==="
hdc file send test.kexe /data/local/tmp/test
hdc file send test.gcno /data/local/tmp/test.gcno
hdc shell chmod 777 /data/local/tmp/test

echo "=== Running on Device ==="
hdc shell "cd /data/local/tmp && rm -f *.gcda && GCOV_PREFIX=/data/local/tmp/gcov GCOV_PREFIX_STRIP=99 ./test"

echo "=== Retrieving Coverage ==="
hdc file recv /data/local/tmp/gcov/test.gcda coverage/test.gcda

echo "=== Generating Report ==="
# Change to coverage directory so all .gcov files are generated there
cd coverage
$LLVM_COV gcov ../test_coverage.kt --gcno ../test.gcno --gcda test.gcda > /dev/null
cd ..

echo ""
head -26 coverage/test_coverage.kt.gcov | tail -22
echo ""
echo "✅ Done! Full report in coverage/test_coverage.kt.gcov"
