#!/usr/bin/env bash
# 列出 byte / kuikly / cpf 三家 fork 的所有 platform.* 包名,做交集,分析冲突。
# "其他 target 也有"的判断以 cpf 为准(cpf target 最全)。
#
# 输入:三个 platformLibs 根目录(各自含 <target>/ 子目录,每个 target 下有 *.def)。
# Usage:
#   ./list-platform-packages.sh <cpf-platformLibs> <byte-platformLibs> <kuikly-platformLibs>
#   # 或不传参用默认路径
set -euo pipefail

CPF="${1:-$HOME/git/reference/kotlin/kotlin-native/platformLibs/src/platform}"
BYTE="${2:-$HOME/git/worktree/kotlin-byte-2.0/kotlin-native/platformLibs/src/platform}"
KUIKLY="${3:-$HOME/git/worktree/kotlin-kuikly-2.0/kotlin-native/platformLibs/src/platform}"

# 提取一个 platformLibs 根目录下 **ohos target** 的所有 def 的 package FQN
# (只关心 ohos platform 库,不混入 ios/osx 等)
# 输出:每行一个 package(去重排序)
extract_ohos_packages() {
    local root="$1"
    [ -d "$root" ] || { echo "(目录不存在: $root)" >&2; return 1; }
    find "$root/ohos" -mindepth 1 -maxdepth 1 -name "*.def" 2>/dev/null | while read -r def; do
        grep -E "^package[[:space:]]*=" "$def" 2>/dev/null | head -1 \
            | sed -E 's/.*=[[:space:]]*//; s/[[:space:]]*$//; s/\r//'
    done | sort -u
}

# 给一个 package,返回它在 cpf 哪些 target 出现(以 cpf 为准)
# 一次性建 cpf 的 package→targets 映射表,后续查表
build_cpf_pkg_targets() {
    find "$CPF" -mindepth 2 -maxdepth 2 -name "*.def" 2>/dev/null | while read -r def; do
        p=$(grep -E "^package[[:space:]]*=" "$def" 2>/dev/null | head -1 \
            | sed -E 's/.*=[[:space:]]*//; s/[[:space:]]*$//; s/\r//')
        [ -n "$p" ] && printf '%s\t%s\n' "$p" "$(dirname "$def" | xargs basename)"
    done | awk '{ if (!seen[$0]++) pkg[$1]=pkg[$1]","$2 } END { for (p in pkg) print p"\t"substr(pkg[p],2) }' \
        | sort > /tmp/cpf-pkg-targets.txt
}

# 标注:只要 cpf 里有非 ohos target 出现该 package,就标"其他target也有"
# (ohos target 有是必然的,因为三家都是 ohos fork)
mark() {
    local pkg="$1"
    local targets
    targets=$(awk -F'\t' -v p="$pkg" '$1==p{print $2}' /tmp/cpf-pkg-targets.txt | tr ',' '\n' | sort -u | tr '\n' ' ')
    local others
    others=$(echo "$targets" | tr ' ' '\n' | grep -vE '^ohos$|^$')
    if [ -n "$others" ]; then
        echo "  [其他target也有: $(echo $targets | tr ' ' ',' | sed 's/^ //;s/,$//')]"
    fi
}

echo "======================================================================"
echo "  三家 platform 库包名 + 冲突分析"
echo "======================================================================"
echo "cpf:    $CPF"
echo "byte:   $BYTE"
echo "kuikly: $KUIKLY"
echo

extract_ohos_packages "$CPF"    > /tmp/pkg-cpf.txt
extract_ohos_packages "$BYTE"   > /tmp/pkg-byte.txt
extract_ohos_packages "$KUIKLY" > /tmp/pkg-kuikly.txt
build_cpf_pkg_targets

echo "====== 包名数 ======"
echo "cpf:    $(wc -l < /tmp/pkg-cpf.txt    | tr -d ' ') 个"
echo "byte:   $(wc -l < /tmp/pkg-byte.txt   | tr -d ' ') 个"
echo "kuikly: $(wc -l < /tmp/pkg-kuikly.txt | tr -d ' ') 个"
echo

echo "======================================================================"
echo "  冲突 (同 package FQN,所有冲突都列出)"
echo "  [其他target也有] = cpf 里该 package 还出现在非 ohos target(以 cpf 为准)"
echo "======================================================================"
echo

print_intersect() {
    local name="$1" a="$2" b="$3"
    echo "====== $name ======"
    comm -12 "$a" "$b" > /tmp/intersect.tmp
    if [ -s /tmp/intersect.tmp ]; then
        while read -r p; do
            echo "$p$(mark "$p")"
        done < /tmp/intersect.tmp
    else
        echo "(无)"
    fi
    echo
}

print_intersect "cpf ∩ byte"   /tmp/pkg-cpf.txt    /tmp/pkg-byte.txt
print_intersect "cpf ∩ kuikly" /tmp/pkg-cpf.txt    /tmp/pkg-kuikly.txt
print_intersect "byte ∩ kuikly" /tmp/pkg-byte.txt  /tmp/pkg-kuikly.txt

echo "======================================================================"
echo "  说明"
echo "======================================================================"
echo "- 冲突 = 同 package FQN 在两家都存在(OHOS 专有模块命名体系不同,多数不撞)"
echo "- [其他target也有] 表示该 package 在 cpf 的非 ohos target(linux/android/macos 等)"
echo "  也存在——独立封装若发会和多家 dist 撞定义,需评估"
echo "- 无标注 = 该 package 只在 ohos target 出现(cpf 里),是 ohos 独有"
