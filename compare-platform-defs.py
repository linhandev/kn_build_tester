#!/usr/bin/env python3
# 比较四个 prebuilt dist 的 ohos platform 库包名,其他三个(byte/kuikly/huawei)分别与 cpf 做交集。
# "其他 target 也有"以 cpf dist 的 platformDef 下全部 target 子目录判断。
#
# 输入:prebuilt/ 下四个 dist 目录,每个的 konan/platformDef/ohos_arm64/*.def
# Usage:
#   ./compare-platform-defs.py                          # 用默认 prebuilt 路径
#   ./compare-platform-defs.py <prebuilt-root>          # 指定 prebuilt 根
import sys, os, re
from pathlib import Path

# 四个 dist 的目录名(在 prebuilt/ 下)
CPF = "kotlin-native-prebuilt-macos-aarch64-2.2.21-0.4.0-08"
BYTE = "kotlin-native-prebuilt-macos-aarch64-2.0.20-bytekmp-1001"
KUIKLY = "kotlin-native-prebuilt-macos-aarch64-2.0.21-KBA-014"
HUAWEI = "kotlin-native-prebuilt-macos-aarch64-2.3.20-HUAWEI"

DEFAULT_PREBUILT = Path(__file__).parent / "prebuilt"

# dist 里 platformDef 的 target 子目录(判断"其他 target 也有"用)
# def 在 <dist>/konan/platformDef/<target>/*.def
OHOS_TARGET = "ohos_arm64"

def dist_root(prebuilt: Path, name: str) -> Path:
    return prebuilt / name / "konan" / "platformDef"

def extract_ohos_packages(platform_def_root: Path) -> set:
    """读 ohos_arm64 下所有 def 的 package= 行,返回 package FQN 集合。"""
    ohos_dir = platform_def_root / OHOS_TARGET
    if not ohos_dir.is_dir():
        print(f"(目录不存在: {ohos_dir})", file=sys.stderr)
        return set()
    pkgs = set()
    for def_file in sorted(ohos_dir.glob("*.def")):
        pkg = read_package(def_file)
        if pkg:
            pkgs.add(pkg)
    return pkgs

def read_package(def_file: Path) -> str:
    """读 def 文件的 package= 行(格式 'package = platform.xxx' 或 'package=...')。"""
    try:
        for line in def_file.read_text(encoding="utf-8", errors="replace").splitlines():
            m = re.match(r"^package\s*=\s*(\S.*?)\s*$", line)
            if m:
                return m.group(1).strip()
    except Exception as e:
        print(f"读 {def_file} 失败: {e}", file=sys.stderr)
    return ""

def build_cpf_pkg_targets(cpf_root: Path) -> dict:
    """扫 cpf platformDef 下所有 target 子目录,建 package→[targets] 映射。"""
    mapping = {}
    if not cpf_root.is_dir():
        return mapping
    for target_dir in sorted(cpf_root.iterdir()):
        if not target_dir.is_dir():
            continue
        for def_file in target_dir.glob("*.def"):
            pkg = read_package(def_file)
            if not pkg:
                continue
            mapping.setdefault(pkg, set()).add(target_dir.name)
    return mapping

def mark_other_targets(pkg: str, cpf_mapping: dict) -> str:
    """以 cpf 为准,只要有一个非 ohos 平台 target 出现该 package,就标注。
    ohos_arm64/ohos_x64 都是 ohos(同平台不同架构),不算"其他 target"。
    只有 linux/android/ios/macos 等非 ohos target 才算。"""
    targets = cpf_mapping.get(pkg, set())
    others = sorted(t for t in targets if not t.startswith("ohos_"))
    if others:
        return f"  [其他target也有: {','.join(others)}]"
    return ""

def main():
    prebuilt = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_PREBUILT
    if not prebuilt.is_dir():
        print(f"❌ prebuilt 目录不存在: {prebuilt}")
        sys.exit(1)

    print("=" * 70)
    print("  四个 prebuilt dist 的 ohos platform 库包名 + 与 cpf 的交集")
    print("=" * 70)
    print(f"prebuilt: {prebuilt}")
    print()

    cpf_root = dist_root(prebuilt, CPF)
    byte_root = dist_root(prebuilt, BYTE)
    kuikly_root = dist_root(prebuilt, KUIKLY)
    huawei_root = dist_root(prebuilt, HUAWEI)

    cpf_pkgs = extract_ohos_packages(cpf_root)
    byte_pkgs = extract_ohos_packages(byte_root)
    kuikly_pkgs = extract_ohos_packages(kuikly_root)
    huawei_pkgs = extract_ohos_packages(huawei_root)

    print(f"cpf({CPF}):    {len(cpf_pkgs)} 个")
    print(f"byte({BYTE}):   {len(byte_pkgs)} 个")
    print(f"kuikly({KUIKLY}): {len(kuikly_pkgs)} 个")
    print(f"huawei({HUAWEI}): {len(huawei_pkgs)} 个")
    print()

    # 以 cpf 为准建 package→targets 映射(判断"其他 target 也有")
    cpf_mapping = build_cpf_pkg_targets(cpf_root)

    # 三组交集合并成表:每行一个库名,标注在 byte/kuikly/bilibili 哪几家有冲突
    # 列:库名 | 其他target有 | byte | kuikly | bilibili(huawei)
    # 按"冲突家数"降序,再按库名排序
    inter_byte = cpf_pkgs & byte_pkgs
    inter_kuikly = cpf_pkgs & kuikly_pkgs
    inter_huawei = cpf_pkgs & huawei_pkgs
    all_conflict = inter_byte | inter_kuikly | inter_huawei

    # 给每个库名算:冲突家数(0-3),其他target有没有
    rows = []
    for p in all_conflict:
        in_byte = "v" if p in inter_byte else ""
        in_kuikly = "v" if p in inter_kuikly else ""
        in_bilibili = "v" if p in inter_huawei else ""
        # 其他target:以 cpf 为准,有非 ohos target 就标 v
        targets = cpf_mapping.get(p, set())
        other_targets = any(not t.startswith("ohos_") for t in targets)
        other_mark = "v" if other_targets else ""
        count = (1 if in_byte else 0) + (1 if in_kuikly else 0) + (1 if in_bilibili else 0)
        rows.append((count, p, other_mark, in_byte, in_kuikly, in_bilibili))

    # 按冲突家数降序,再按库名
    rows.sort(key=lambda r: (-r[0], r[1]))

    print("=" * 70)
    print("  与 cpf 的冲突 (按冲突家数降序)")
    print("  其他target有 = cpf dist 里该 package 还出现在非 ohos target")
    print("  byte/kuikly/bilibili 列:与 cpf 撞名写 v")
    print("=" * 70)
    print()
    print(f"| {'库名':<28} | 其他target有 | byte | kuikly | bilibili |")
    print(f"|{'-'*30}|{'-'*14}|{'-'*6}|{'-'*8}|{'-'*10}|")
    for count, p, other_mark, in_byte, in_kuikly, in_bilibili in rows:
        print(f"| {p:<28} | {other_mark:<12} | {in_byte:<4} | {in_kuikly:<6} | {in_bilibili:<8} |")
    print()

    print("=" * 70)
    print("  说明")
    print("=" * 70)
    print("- 冲突 = 同 package FQN 在 cpf 和该 dist 都存在")
    print("- 其他target有 v = 该 package 在 cpf dist 的非 ohos target(linux/android/ios 等)")
    print("  也存在——独立封装若发会和多家 dist 撞定义")
    print("- 其他target有 空 = 该 package 只在 ohos target 出现,是 ohos 独有")
    print("- byte/kuikly/bilibili 列 v = 与 cpf 该 dist 撞名")

if __name__ == "__main__":
    main()
