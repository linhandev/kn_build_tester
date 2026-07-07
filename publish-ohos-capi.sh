#!/usr/bin/env bash
# 发布 org.cpf.kotlin:ohos-capi + hms-capi:<klibVersion>(主产物),biz-klib(测试用,bare 依赖)。
# biz-klib 永远只发本地 m2,不上 colab(测试用 klib,非 capi 封装产物)。
# static-lib-demo 仍走 ./gradlew :static-lib-demo:publish(测试用,不纳入本脚本)。
#
# Usage:
#   ./publish-ohos-capi.sh              # 只发本地 m2 (默认)
#   REMOTE=true ./publish-ohos-capi.sh  # 发本地 m2 + colab 远程仓
#
# 前置:
#   - cpf 0.4 KN dist: ~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03
#   - REMOTE=true 时 producer/local.properties 含 colabMavenUser/Pass
#   - producer/gradle.properties 的 klibVersion = 版本号
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROD="$ROOT/producer"
CPF_DIST="$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03"

REMOTE="${REMOTE:-false}"

[ -d "$CPF_DIST" ] || { echo "❌ cpf 0.4 KN dist 不在 $CPF_DIST"; exit 1; }

KLIB_VER=$(grep '^klibVersion=' "$PROD/gradle.properties" | cut -d= -f2)
echo "klibVersion=$KLIB_VER"
echo "cpf 0.4 dist: $CPF_DIST"
echo "REMOTE=$REMOTE"

if [ "$REMOTE" = "true" ]; then
    [ -f "$PROD/local.properties" ] || { echo "❌ REMOTE=true 需要 $PROD/local.properties 含 colabMavenUser/Pass"; exit 1; }
    # local.properties 有凭据时 build 脚本会加 colab repo;置空 env 让 local.properties 生效
    echo "目标: colab + 本地 m2"
else
    # REMOTE=false: 置空凭据 env,build 脚本里 !isNullOrEmpty 判断会跳过 colab repo
    export COLAB_MAVEN_USER=""
    export COLAB_MAVEN_PASS=""
    echo "目标: 仅本地 m2"
fi

cd "$PROD"
# ohos-capi + hms-capi:主产物,REMOTE=true 时发 colab + 本地 m2。
env kotlin.native.home="$CPF_DIST" ./gradlew :ohos-capi:publish :hms-capi:publish --no-daemon

# biz-klib:测试用 klib(bare 依赖它),永远只发本地 m2,不上 colab(置空凭据强制跳过 colab repo)。
COLAB_MAVEN_USER="" COLAB_MAVEN_PASS="" env kotlin.native.home="$CPF_DIST" \
  ./gradlew :biz-klib:publish --no-daemon

echo "✅ 发布完成: org.cpf.kotlin:ohos-capi:$KLIB_VER + org.cpf.kotlin:hms-capi:$KLIB_VER (colab+m2); biz-klib:$KLIB_VER (仅 m2)"

# 验证
if [ "$REMOTE" = "true" ]; then
    COLAB_USER=$(grep '^colabMavenUser=' "$PROD/local.properties" | cut -d= -f2)
    COLAB_PASS=$(grep '^colabMavenPass=' "$PROD/local.properties" | cut -d= -f2)
    COLAB="https://packages.aliyun.com/687e79a0e94e043d2d0f76ea/maven/colab"
    echo "验证 colab:"
    for f in "ohos-capi-$KLIB_VER.pom" "ohos-capi-$KLIB_VER.klib" "ohos-capi-$KLIB_VER.module"; do
        code=$(curl -sI -u "$COLAB_USER:$COLAB_PASS" "$COLAB/org/cpf/kotlin/ohos-capi/$KLIB_VER/$f" 2>/dev/null | head -1)
        echo "  $f: $code"
    done
fi
echo "本地 m2:"
ohosCount=$(ls "$ROOT/m2/org/cpf/kotlin/ohos-capi/$KLIB_VER/"*.klib 2>/dev/null | wc -l | tr -d ' ')
hmsCount=$(ls "$ROOT/m2/org/cpf/kotlin/hms-capi/$KLIB_VER/"*.klib 2>/dev/null | wc -l | tr -d ' ')
bizCount=$(ls "$ROOT/m2/org/cpf/kotlin/biz-klib/$KLIB_VER/"*.klib 2>/dev/null | wc -l | tr -d ' ')
echo "  ohos-capi: $ohosCount 个 klib (期望 145 = 144 cinterop + 1 main)"
echo "  hms-capi:  $hmsCount 个 klib (期望 20 = 19 + 1)"
echo "  biz-klib:  $bizCount 个 klib (仅 m2,不上 colab)"
ls "$ROOT/m2/org/cpf/kotlin/ohos-capi/$KLIB_VER/"*.klib 2>/dev/null | head -2 | while read -r f; do
    echo "  $(basename "$f")"
done
ls "$ROOT/m2/org/cpf/kotlin/hms-capi/$KLIB_VER/"*.klib 2>/dev/null | head -2 | while read -r f; do
    echo "  $(basename "$f")"
done
