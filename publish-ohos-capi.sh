#!/usr/bin/env bash
# 只发布 org.cpf.kotlin:ohos-capi:<klibVersion> 到 colab 远程仓 + 本地 m2。
# biz-klib / static-lib-demo 是测试用 klib,不发布。
#
# Usage:
#   ./publish-ohos-capi.sh            # 发到 colab + 本地 m2
#   COLAB_OFF=1 ./publish-ohos-capi.sh # 只发本地 m2(跳过 colab,本地调试用)
#
# 前置:
#   - cpf 0.4 KN dist: ~/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03
#   - producer/local.properties 含 colabMavenUser/Pass(发 colab 时需要,COLAB_OFF=1 时不需要)
#   - producer/gradle.properties 的 klibVersion = 版本号(当前 22-0.1)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROD="$ROOT/producer"
CPF_DIST="$HOME/.konan/kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-03"

green() { printf "\033[32m%s\033[0m\n" "$*"; }
red()   { printf "\033[31m%s\033[0m\n" "$*"; }
bold()  { printf "\033[1m%s\033[0m\n" "$*"; }
fail()  { red "❌ $*"; exit 1; }

# COLAB_OFF=1 时跳过 colab(build.gradle.kts 里 colab 仓库靠 COLAB_MAVEN_USER 环境变量 +
# local.properties 凭据;unset 这两个 env 后,代码里 colabUser==null → 不加 colab repo)
PUBLISH_COLAB=1
if [ "${COLAB_OFF:-0}" = "1" ]; then
    PUBLISH_COLAB=0
    export COLAB_MAVEN_USER=""
    export COLAB_MAVEN_PASS=""
fi

bold "======================================================================"
bold "  发布 org.cpf.kotlin:ohos-capi"
bold "======================================================================"

# --- 前置检查 ---
[ -d "$CPF_DIST" ] || fail "cpf 0.4 KN dist 不在 $CPF_DIST"
if [ "$PUBLISH_COLAB" -eq 1 ]; then
    [ -f "$PROD/local.properties" ] || fail "发 colab 需要 $PROD/local.properties 含 colabMavenUser/Pass (本地调试用 COLAB_OFF=1)"
    green "✅ 目标: colab + 本地 m2"
else
    green "✅ 目标: 仅本地 m2 (COLAB_OFF=1)"
fi

KLIB_VER=$(grep '^klibVersion=' "$PROD/gradle.properties" | cut -d= -f2)
green "✅ klibVersion=$KLIB_VER"
green "✅ cpf 0.4 dist: $CPF_DIST"

# --- 发布 ---
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT/kn_sample-gradle-home}"
[ -d "$GRADLE_USER_HOME" ] || export GRADLE_USER_HOME="$HOME/.gradle"
green "✅ GRADLE_USER_HOME=$GRADLE_USER_HOME"

cd "$PROD"
env kotlin.native.home="$CPF_DIST" ./gradlew :ohos-capi:publish --no-daemon

green "✅ 发布完成: org.cpf.kotlin:ohos-capi:$KLIB_VER"

# --- 验证 ---
if [ "$PUBLISH_COLAB" -eq 1 ]; then
    COLAB_USER=$(grep '^colabMavenUser=' "$PROD/local.properties" | cut -d= -f2)
    COLAB_PASS=$(grep '^colabMavenPass=' "$PROD/local.properties" | cut -d= -f2)
    COLAB="https://packages.aliyun.com/687e79a0e94e043d2d0f76ea/maven/colab"
    bold "验证 colab:"
    for f in "ohos-capi-$KLIB_VER.pom" "ohos-capi-$KLIB_VER.klib" "ohos-capi-$KLIB_VER.module"; do
        code=$(curl -sI -u "$COLAB_USER:$COLAB_PASS" "$COLAB/org/cpf/kotlin/ohos-capi/$KLIB_VER/$f" 2>/dev/null | head -1)
        green "  $f: $code"
    done
fi
bold "验证本地 m2:"
ls "$ROOT/m2/org/cpf/kotlin/ohos-capi/$KLIB_VER/"*.klib 2>/dev/null | head -3 | while read -r f; do
    green "  $(basename "$f")"
done

bold "🎉 完成"
