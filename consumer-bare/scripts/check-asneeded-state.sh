#!/usr/bin/env bash
# 两个断言一起验证 def 的 --push-state --as-needed ... --pop-state 既"不泄漏"又"真生效":
#
# 断言 1(不泄漏):全局 as 状态探针 -ltss2-tctildr
#   consumer-bare 链接器处于 no-as-needed(lld 默认)。
#   故意 -ltss2-tctildr(bare 没用到、不在任何 def 的 linkerOpts、HMS sysroot 有 stub)。
#     - 全局 no-as-needed → 探针库(哪怕未引用)进 NEEDED  ← 我们要的
#     - 全局被泄漏成 as-needed → 探针库被当未引用丢弃,不在 NEEDED  ← 失败
#   探针在 NEEDED ⇔ 全局 no-as-needed ⇔ def 的 push/pop 没把 --as-needed 泄漏到全局。
#
# 断言 2(按需生效):未引用库 -lohcrypto
#   bare 只调 AVTranscoder,但依赖整个 ohos-capi 聚合(159 个 def 的 linkerOpts 全进 link
#   命令行)。多个 def 有 -lohcrypto(bare 没用到,ohos 主 sysroot 有 stub)。
#     - def 内 --as-needed 生效 → 未引用的 ohcrypto 被丢,不在 NEEDED  ← 我们要的
#     - def 内 --as-needed 没生效 → ohcrypto 进 NEEDED(连同其余 150+ 个库)  ← 失败
#   ohcrypto 不在 NEEDED ⇔ def 内 as-needed 真生效,按需链接达成。
#
# 前置:
#   - m2/ 已发布新版 ohos-capi(def 的 linkerOpts 已包 push-state/as-needed/pop-state)
#   - DevEco 的 llvm-readelf 在 DEV/Contents/sdk/default/openharmony/native/llvm/bin/
# Usage:
#   ./scripts/check-asneeded-state.sh
set -euo pipefail

_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$_script_dir/.."

DEV="/Applications/DevEco-Studio.app"
READELF="$DEV/Contents/sdk/default/openharmony/native/llvm/bin/llvm-readelf"
SO="kotlinApp/build/bin/ohosArm64/debugShared/libc2k.so"
# 探针:全局 as 状态探针。bare 没用到、不在任何 def 的 linkerOpts 里、HMS sysroot 有 stub。
# 全局 no-as-needed → 进 NEEDED;全局被泄漏成 as-needed → 被丢,不在 NEEDED。
PROBE="libtss2-tctildr.so"
# 反例:按需生效探针。bare 没用到,但多个 def 的 linkerOpts 里有 -lohcrypto(ohos 主 sysroot
# 有 stub)。def 内 --push-state --as-needed 生效 → 未引用的 ohcrypto 被丢,不在 NEEDED;
# 若 def 内 as-needed 没生效 → ohcrypto 进 NEEDED(连同其余 150+ 个库)。
UNUSED_BUT_LINKED="libohcrypto.so"

[ -x "$READELF" ] || { echo "❌ llvm-readelf 不在 $READELF(DevEco 未安装?)"; exit 1; }

echo "== 1/3 link :kotlinApp:linkDebugSharedOhosArm64 =="
./gradlew :kotlinApp:linkDebugSharedOhosArm64 --no-daemon --rerun-tasks 2>&1 | tail -3

[ -f "$SO" ] || { echo "❌ link 产物不存在: $SO"; exit 1; }

needed="$("$READELF" -d "$SO" 2>/dev/null | grep NEEDED || true)"
echo "== libc2k.so NEEDED =="
echo "$needed" | sed 's/^/  /'
echo ""

rc=0

echo "== 2/3 断言探针 $PROBE 在 NEEDED 里(全局 no-as-needed → 没泄漏 as-needed 状态)==="
if echo "$needed" | grep -q "$PROBE"; then
  echo "  ✅ $PROBE 在 NEEDED → 全局仍是 no-as-needed,def 的 push/pop 没有泄漏 as-needed。"
else
  echo "  ❌ $PROBE 不在 NEEDED → 全局被泄漏成了 as-needed,def 的 push/pop 改变了全局 as 状态!"
  rc=1
fi

echo "== 3/3 断言 $UNUSED_BUT_LINKED 不在 NEEDED 里(def 内 as-needed 生效,按需丢弃未引用库)==="
if echo "$needed" | grep -q "$UNUSED_BUT_LINKED"; then
  echo "  ❌ $UNUSED_BUT_LINKED 在 NEEDED → def 内 --as-needed 没生效(bare 没用到它本该被丢),按需链接失败!"
  rc=1
else
  echo "  ✅ $UNUSED_BUT_LINKED 不在 NEEDED → def 内 --as-needed 生效,未引用库被按需丢弃。"
fi

exit $rc
