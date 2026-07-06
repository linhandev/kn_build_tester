#!/usr/bin/env bash
# 列出 byte / kuikly / cpf 三家 OHOS KMP fork 的 platform 库包名,并分析命名冲突。
# 多 target 共有的基础库(posix/linux/builtin/zlib 等)备注出来。
#
# 数据源:三家 fork 的 platformLibs/src/platform/ohos(或 platformDef)下的 .def 文件,
# 读每个 def 的 `package = ...` 行(不依赖 dist,看源码即可)。
#
# Usage:
#   ./list-platform-packages.sh                # 用默认路径
#   CPF=<path> BYTE=<path> KUIKLY=<path> ./list-platform-packages.sh  # 覆盖路径
set -euo pipefail

# 默认路径(可被环境变量覆盖)
CPF="${CPF:-$HOME/git/reference/kotlin/kotlin-native/platformLibs/src/platform/ohos}"
BYTE="${BYTE:-$HOME/git/worktree/kotlin-byte-2.0/kotlin-native/platformLibs/src/platform/ohos}"
KUIKLY="${KUIKLY:-$HOME/git/worktree/kotlin-kuikly-2.0/kotlin-native/dist/konan/platformDef/ohos_arm64}"

# 多 target 共有的基础库(在 linux/macos 等其它 target 也有同名 package)
BASE_LIBS="posix linux builtin zlib iconv"

# 提取一个目录下所有 def 的 package FQN,输出 "package<TAB>def文件"
extract_packages() {
    local dir="$1"
    [ -d "$dir" ] || { echo "(目录不存在: $dir)" >&2; return 1; }
    # def 里 package 行格式: "package = platform.xxx" 或 "package=platform.xxx"
    for def in "$dir"/*.def; do
        [ -f "$def" ] || continue
        pkg=$(grep -E "^package[[:space:]]*=" "$def" 2>/dev/null | head -1 | sed -E 's/.*=[[:space:]]*//; s/[[:space:]]*$//; s/\r//')
        [ -n "$pkg" ] && printf '%s\t%s\n' "$pkg" "$(basename "$def")"
    done | sort
}

echo "======================================================================"
echo "  三家 platform 库包名 (byte / kuikly / cpf)"
echo "======================================================================"
echo "cpf    def 目录: $CPF"
echo "byte   def 目录: $BYTE"
echo "kuikly def 目录: $KUIKLY"
echo

# 各自包名列表(去重,只留 package 列)
extract_packages "$CPF"    | cut -f1 | sort -u > /tmp/pkg-cpf.txt
extract_packages "$BYTE"   | cut -f1 | sort -u > /tmp/pkg-byte.txt
extract_packages "$KUIKLY" | cut -f1 | sort -u > /tmp/pkg-kuikly.txt

echo "====== cpf ($(wc -l < /tmp/pkg-cpf.txt | tr -d ' ') 个) ======"
cat /tmp/pkg-cpf.txt
echo
echo "====== byte ($(wc -l < /tmp/pkg-byte.txt | tr -d ' ') 个) ======"
cat /tmp/pkg-byte.txt
echo
echo "====== kuikly ($(wc -l < /tmp/pkg-kuikly.txt | tr -d ' ') 个) ======"
cat /tmp/pkg-kuikly.txt
echo

# 冲突分析:同 package FQN 的
echo "======================================================================"
echo "  命名冲突分析 (同 package FQN)"
echo "======================================================================"

echo "====== cpf ∩ byte ======"
comm -12 /tmp/pkg-cpf.txt /tmp/pkg-byte.txt > /tmp/cpf-byte-intersect
if [ -s /tmp/cpf-byte-intersect ]; then
    while read -r p; do
        mark=""
        for b in $BASE_LIBS; do echo "$p" | grep -qE "\.$b$|^platform\.$b$" && mark="  [基础库,多target共有]"; done
        echo "$p$mark"
    done < /tmp/cpf-byte-intersect
else
    echo "(无)"
fi
echo

echo "====== cpf ∩ kuikly ======"
comm -12 /tmp/pkg-cpf.txt /tmp/pkg-kuikly.txt > /tmp/cpf-kuikly-intersect
if [ -s /tmp/cpf-kuikly-intersect ]; then
    while read -r p; do
        mark=""
        for b in $BASE_LIBS; do echo "$p" | grep -qE "\.$b$|^platform\.$b$" && mark="  [基础库,多target共有]"; done
        echo "$p$mark"
    done < /tmp/cpf-kuikly-intersect
else
    echo "(无)"
fi
echo

echo "====== byte ∩ kuikly ======"
comm -12 /tmp/pkg-byte.txt /tmp/pkg-kuikly.txt > /tmp/byte-kuikly-intersect
if [ -s /tmp/byte-kuikly-intersect ]; then
    while read -r p; do
        mark=""
        for b in $BASE_LIBS; do echo "$p" | grep -qE "\.$b$|^platform\.$b$" && mark="  [基础库,多target共有]"; done
        echo "$p$mark"
    done < /tmp/byte-kuikly-intersect
else
    echo "(无)"
fi
echo

echo "======================================================================"
echo "  备注:多 target 共有的基础库"
echo "======================================================================"
echo "以下 package 名在 linux/macos 等其它 target 的 platformLibs 里也同名存在,"
echo "独立封装若也发这几个会和 dist 撞定义(见 design.md §2.1)。"
echo
echo "在三家 OHOS dist 里的基础库 package:"
for b in $BASE_LIBS; do
    in_cpf=$(grep -cE "^platform\.$b$" /tmp/pkg-cpf.txt 2>/dev/null || true)
    in_byte=$(grep -cE "^platform\.$b$" /tmp/pkg-byte.txt 2>/dev/null || true)
    in_kuikly=$(grep -cE "^platform\.$b$" /tmp/pkg-kuikly.txt 2>/dev/null || true)
    printf '  platform.%s: cpf=%s byte=%s kuikly=%s\n' "$b" "$in_cpf" "$in_byte" "$in_kuikly"
done
