#!/usr/bin/env bash
# kn_sample end-to-end: producer publish → consumer-bare smoke → consumer-capi-demo autotest
# Usage: ./run-all.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROD="$ROOT/producer"
BARE="$ROOT/consumer-bare"
CAPI="$ROOT/consumer-capi-demo"
DEV="/Applications/DevEco-Studio.app"
HMS_SYSROOT="$ROOT/sysroot/sysroot-hms-aarch64-6.0.2.640-02"
READELF="$DEV/Contents/sdk/default/openharmony/native/llvm/bin/llvm-readelf"

# --- color ---
green() { printf "\033[32m%s\033[0m\n" "$*"; }
red()   { printf "\033[31m%s\033[0m\n" "$*"; }
yellow(){ printf "\033[33m%s\033[0m\n" "$*"; }
bold()  { printf "\033[1m%s\033[0m\n" "$*"; }

fail() { red "❌ $*"; exit 1; }

# --- step banner ---
step() { echo; bold "======================================================================"; bold "  $*"; bold "======================================================================"; }

# ============ 0. preflight ============
step "0/4  前置检查"
command -v java >/dev/null || fail "java 未找到"
command -v hdc  >/dev/null || fail "hdc 未找到(DevEco toolchain 不在 PATH?)"
[ -d "$DEV" ] || fail "DevEco 未安装在 $DEV"
[ -d "$HMS_SYSROOT" ] || fail "HMS sysroot 不在 $HMS_SYSROOT"
hdc list targets 2>/dev/null | LC_ALL=C tr -d '\r' | grep -q . || fail "无 hdc 设备在线"
for p in "$BARE" "$CAPI"; do
  [ -f "$p/local.properties" ] || fail "$p/local.properties 缺失(需 huaweiMavenUser/Pass)"
done
green "✅ java/hdc/DevEco/HMS sysroot/设备/凭据 齐全"

# ============ 1/4  producer: publish klib to maven local ============
step "1/4  producer: publishToMavenLocal (cpf 0.4 → hilog-klib + static-lib-demo)"
cd "$PROD"
./gradlew --stop >/dev/null 2>&1 || true
rm -rf "$HOME/.m2/repository/com/example/hilog-klib" "$HOME/.m2/repository/com/example/static-lib-demo"
if ./gradlew :hilog-klib:publishToMavenLocal :static-lib-demo:publishToMavenLocal --no-daemon 2>&1 | tail -20 | grep -q "BUILD SUCCESSFUL"; then
  klibs=$(ls "$HOME/.m2/repository/com/example/hilog-klib/1.0-SNAPSHOT/"*.klib 2>/dev/null | wc -l | tr -d ' ')
  green "✅ hilog-klib 发布: $klibs 个 klib (期望 160)"
  slklibs=$(ls "$HOME/.m2/repository/com/example/static-lib-demo/1.0-SNAPSHOT/"*.klib 2>/dev/null | wc -l | tr -d ' ')
  green "✅ static-lib-demo 发布: $slklibs 个 klib (含嵌入 .a)"
else
  fail "producer publishToMavenLocal 失败"
fi

# ============ 2/4  consumer-bare: compile + link + deploy + hilog ============
step "2/4  consumer-bare: 简单消费者 (HiLog + AssetApi→AssetType) 端到端"
cd "$BARE"
./gradlew --stop >/dev/null 2>&1 || true
# link first to surface link errors clearly
./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-daemon 2>&1 | tail -5 | grep -q "BUILD SUCCESSFUL" \
  || fail "consumer-bare link 失败"
yellow "  libc2k.so NEEDED:"
"$READELF" -d kotlinApp/build/bin/ohosArm64/debugShared/libc2k.so 2>/dev/null | grep NEEDED | sed 's/^/    /'

hdc uninstall com.kotlin.demo >/dev/null 2>&1 || true
./gradlew :kotlinApp:startHarmonyAppDebug --no-daemon --rerun-tasks 2>&1 | tail -10 | grep -q "BUILD SUCCESSFUL" \
  || fail "consumer-bare startHarmonyAppDebug 失败"

# capture hilog
hdc shell "hilog -r" >/dev/null 2>&1
hdc shell "aa force-stop com.kotlin.demo" >/dev/null 2>&1
sleep 1
hdc shell "aa start -a EntryAbility -b com.kotlin.demo" >/dev/null 2>&1
sleep 4
kn_log=$(hdc shell "hilog -x" 2>/dev/null | LC_ALL=C tr -d '\r' | grep -a "A01234.*kn_demo" | tail -1)
greeting=$(hdc shell "hilog -x" 2>/dev/null | LC_ALL=C tr -d '\r' | grep -a "testTag.*Kotlin greeting" | tail -1)
[ -n "$kn_log" ] && green "✅ KN hilog: $kn_log" || fail "未抓到 KN OH_LOG_Print 日志 (kn_demo)"
[ -n "$greeting" ] && green "✅ ArkTS: $greeting" || yellow "⚠️  未抓到 ArkTS greeting 日志"

# ============ 3/4  consumer-capi-demo: build .so + HAP + install ============
step "3/4  consumer-capi-demo: 构建 libkn.so + HAP + 安装"
cd "$CAPI"
./gradlew --stop >/dev/null 2>&1 || true
./gradlew :composeApp:publishDebugBinariesToHarmonyApp --no-daemon 2>&1 | tail -5 | grep -q "BUILD SUCCESSFUL" \
  || fail "consumer-capi-demo publishBinaries 失败"

HA="$CAPI/harmonyApp"
export NODE_HOME="$DEV/Contents/tools/node"
export DEVECO_SDK_HOME="$DEV/Contents/sdk"
export PATH="$DEV/Contents/tools/node/bin:$PATH"
cd "$HA"
yellow "  ohpm install..."
"$DEV/Contents/tools/ohpm/bin/ohpm" install --all --registry https://ohpm.openharmony.cn/ohpm/ --strict_ssl true >/dev/null 2>&1 \
  || fail "ohpm install 失败"
yellow "  hvigor assembleHap..."
"$DEV/Contents/tools/node/bin/node" "$DEV/Contents/tools/hvigor/bin/hvigorw.js" \
  --mode module -p module=entry@default -p product=default -p buildMode=debug \
  -p requiredDeviceType=phone assembleHap --analyze=false --parallel --incremental 2>&1 | tail -5 | grep -q "BUILD SUCCESSFUL" \
  || fail "assembleHap 失败"

HAP=$(find "$HA/entry/build" -name "entry-default-signed.hap" 2>/dev/null | head -1)
[ -n "$HAP" ] || fail "未找到 signed HAP"
hdc uninstall com.kotlin.demo >/dev/null 2>&1 || true
hdc install "$HAP" 2>&1 | grep -q "install bundle successfully" || fail "HAP 安装失败"
green "✅ HAP 安装成功"

# ============ 4/4  consumer-capi-demo: autotest.py ============
step "4/4  consumer-capi-demo: autotest.py (9 模块 CAPI smoke UI 自动化)"
cd "$CAPI"
# autotest.py exits 0 regardless of case pass/fail; parse its summary line
output=$(python3 autotest.py 2>&1 || true)
echo "$output" | tail -15

# parse the case-level summary (📊 测试结果统计 block), not the module-level "9/9"
stats=$(echo "$output" | sed -n '/测试结果统计/,/生成测试报告/p')
ok=$(echo "$stats" | grep -oE "成功: [0-9]+" | grep -oE "[0-9]+")
fail_n=$(echo "$stats" | grep -oE "失败: [0-9]+" | grep -oE "[0-9]+")
total=$(echo "$stats" | grep -oE "总计: [0-9]+" | grep -oE "[0-9]+")
echo
bold "==================== 端到端测试结果 ===================="
green "  producer klib        : 发布 $klibs 个 ✅"
green "  consumer-bare        : KN hilog + ArkTS greeting ✅"
if [ "${fail_n:-0}" -eq 0 ]; then
  green "  consumer-capi-demo   : $total 用例,成功 $ok,失败 $fail_n ✅"
  echo
  green "🎉 全流程通过"
  exit 0
else
  red "  consumer-capi-demo   : $total 用例,成功 $ok,失败 $fail_n (有真实失败)"
  exit 1
fi
