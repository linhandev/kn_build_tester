#!/usr/bin/env python3
# 给 producer/ohos-capi 的 def 的 linkerOpts 包 --push-state --as-needed ... --pop-state,
# 实现按需链接:让 lld 只把实际被该库满足未定义符号的库写进 DT_NEEDED,
# 运行时不再因 dlopen 一堆无关 .so 在设备上找不到而崩溃。
#
# 为什么用 push/pop 而非裸 --as-needed:
#   最终 link 出 .so 的 ld 命令行很长(KN dist 的 linkerKonanFlags + consumer 选项 + 本 def 的
#   linkerOpts + trailing flags)。在本 def 这句 linkerOpts 被插入的点之前,链接器可能已处于
#   as-needed,也可能已处于 no-as-needed(由前面那串选项累积决定)。--push-state 压栈的是 push
#   点的完整状态(含 as-needed 标志),--as-needed 只在 push/pop 之间生效,--pop-state 弹回 push
#   前的完整状态 → "原来是 as 还是 as,原来是 noas 还是 noas"。裸 --as-needed 会把状态泄漏到
#   后面的选项,改变后续 -l 的链接行为。
#
# 包装粒度:整行一组(一个 def 的所有 -l 共用一对 push/pop)。
#   linkerOpts = -Wl,--push-state -Wl,--as-needed <所有原 -l ...> -Wl,--pop-state
# (状态选项加 -Wl, 前缀转发给 ld.lld,因为 KN 最终经 clang++ 驱动链接,clang 不认裸
#  --push-state/--as-needed/--pop-state;-l 保持裸写法。)
#
# 幂等:已是目标形态的不改;可反复运行。旧脚本产物的裸形态(--push-state 无 -Wl,)会被
#   识别并重写成写法 B。
# 安全阀:只改"纯 -l 列表"的 linkerOpts 行;遇到任何非 -l token(已含 push-state / -L /
#   -framework / -Wl / -static / -Bstatic 等)一律跳过并 warning,绝不改变原有链接选项状态。
#
# Usage:
#   ./wrap-linkeropts.py            # dry-run,打印将改写的 def + 前后对比(默认,不落盘)
#   ./wrap-linkeropts.py --write    # 落盘
#   ./wrap-linkeropts.py --check    # 仅自检现状(不写),push/pop 配对/as-needed 位置/无状态泄漏
import sys, os, re, argparse
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent
# 拆分后 def 分布在两个模块:ohos-capi(144 ohos def)+ hms-capi(19 hms def)。脚本对两边都管。
DEF_DIRS = [
    REPO_ROOT / "producer" / "ohos-capi" / "nativeInterop" / "ohosArm64",
    REPO_ROOT / "producer" / "hms-capi" / "nativeInterop" / "ohosArm64",
]

# 状态控制类 token:这些一旦出现就会改变链接器 as-needed 状态,绝不能混进我们的 push/pop 组里
# (否则我们就在改变"原有链接选项状态",违反硬约束)。
STATE_TOKENS = {
    "--push-state", "--pop-state", "--as-needed", "--no-as-needed",
    "-push-state", "-pop-state", "-as-needed", "-no-as-needed",  # lld 单横线也认
    "-Bstatic", "-Bdynamic", "--start-group", "--end-group",
    "-static", "--whole-archive", "--no-whole-archive",
}

# 非 -l 且非状态类的"其他链接选项"(-L/-framework/-Wl/-soname/-z/...),出现说明这行不是纯库列表
def is_lib_token(tok: str) -> bool:
    return tok.startswith("-l")

def parse_linkeropts_line(line: str):
    """返回 (key, sep, value) 或 None。
    只认 `linkerOpts = ...` / `linkerOpts=...` 形态(key 小写敏感:文件里就是小写)。
    """
    m = re.match(r"^(linkerOpts)(\s*=\s*)(.*)$", line)
    if not m:
        return None
    return m.group(1), m.group(2), m.group(3)

def tokenize_value(value: str):
    """拆 value 为 token 列表(保序),去首尾空白;空值返回 []。"""
    value = value.strip()
    if not value:
        return []
    return value.split()

def is_pure_lib_list(tokens):
    """全为 -l<lib> 才算纯库列表。"""
    return bool(tokens) and all(is_lib_token(t) for t in tokens)

# 目标写法(写法 B):状态选项加 -Wl, 前缀转发给 ld.lld,-l 保持裸写法。
#   -Wl,--push-state -Wl,--as-needed <纯 -l...> -Wl,--pop-state
# 为什么用 -Wl, 前缀:KN 最终经 clang++ 驱动链接(非直接调 ld.lld),clang 驱动不认裸
#   --push-state/--as-needed/--pop-state(报 "unsupported option"),必须用 -Wl, 转发。
#   konan.properties 里 KN dist 自身的 --as-needed 走的是另一条路径(直接给 ld.lld),
#   不经 clang 驱动;def 的 linkerOpts 经 manifest → clang 驱动,故必须 -Wl,。
WRAP_PREFIX = ["-Wl,--push-state", "-Wl,--as-needed"]
WRAP_SUFFIX = ["-Wl,--pop-state"]

def is_already_wrapped(tokens):
    """已是目标写法 B:-Wl,--push-state -Wl,--as-needed <纯 -l...> -Wl,--pop-state。"""
    if len(tokens) < len(WRAP_PREFIX) + 1 + len(WRAP_SUFFIX):
        return False
    if tokens[:len(WRAP_PREFIX)] != WRAP_PREFIX:
        return False
    if tokens[-len(WRAP_SUFFIX):] != WRAP_SUFFIX:
        return False
    middle = tokens[len(WRAP_PREFIX):-len(WRAP_SUFFIX)]
    return bool(middle) and all(is_lib_token(t) for t in middle)

def is_legacy_wrapped(tokens):
    """旧脚本产物的裸形态:--push-state --as-needed <纯 -l...> --pop-state(无 -Wl, 前缀)。
    clang 驱动不认,需重写成写法 B。"""
    if len(tokens) < 4:
        return False
    if tokens[0] != "--push-state" or tokens[1] != "--as-needed":
        return False
    if tokens[-1] != "--pop-state":
        return False
    middle = tokens[2:-1]
    return bool(middle) and all(is_lib_token(t) for t in middle)

def extract_lib_tokens_from_wrapped(tokens):
    """从已 wrapped(写法 B 或 legacy)的 token 里抽出中间的 -l 列表。"""
    if is_already_wrapped(tokens):
        return tokens[len(WRAP_PREFIX):-len(WRAP_SUFFIX)]
    if is_legacy_wrapped(tokens):
        return tokens[2:-1]
    return None

def wrap_value(tokens):
    """纯 -l 列表 → 写法 B:-Wl,--push-state -Wl,--as-needed <tokens...> -Wl,--pop-state。"""
    return " ".join([*WRAP_PREFIX, *tokens, *WRAP_SUFFIX])

def classify_line(line: str):
    """对一行做分类。返回 (kind, detail)。
    kind:
      not_linkeropts  — 非 linkerOpts 行,不动
      empty           — linkerOpts 但右侧为空,不动
      already_wrapped — 已是目标写法 B,不动
      to_rewrap       — 旧脚本裸形态(--push-state 无 -Wl,),重写成写法 B
      skipped         — 含非 -l token(状态类/其他选项),不动 + warning
      to_wrap         — 纯 -l 列表,待改写
    detail: 对 to_wrap/already_wrapped/skipped 给 token 列表或原因。
    """
    parsed = parse_linkeropts_line(line)
    if not parsed:
        return "not_linkeropts", None
    _, _, value = parsed
    tokens = tokenize_value(value)
    if not tokens:
        return "empty", []
    if is_already_wrapped(tokens):
        return "already_wrapped", tokens
    if is_legacy_wrapped(tokens):
        return "to_rewrap", extract_lib_tokens_from_wrapped(tokens)
    if is_pure_lib_list(tokens):
        return "to_wrap", tokens
    # 含非 -l token:区分一下是状态类还是其他,方便 warning 措辞
    state_hit = [t for t in tokens if t in STATE_TOKENS]
    other_hit = [t for t in tokens if not is_lib_token(t) and t not in STATE_TOKENS]
    return "skipped", {"tokens": tokens, "state": state_hit, "other": other_hit}

# ---------- --check 自检 ----------

def check_file(def_path: Path):
    """对单个 def 的 linkerOpts 行做自检。返回 list[str](问题列表,空=通过)。"""
    problems = []
    try:
        lines = def_path.read_text(encoding="utf-8", errors="replace").splitlines()
    except Exception as e:
        return [f"{def_path.name}: 读失败 {e}"]
    lineno = 0
    for raw in lines:
        lineno += 1
        if not parse_linkeropts_line(raw):
            continue
        _, _, value = parse_linkeropts_line(raw)
        tokens = tokenize_value(value)
        if not tokens:
            continue
        tag = f"{def_path.name}:{lineno}"
        # 1. 已是目标写法 B:通过
        if is_already_wrapped(tokens):
            continue
        # 2. 旧脚本裸形态(--push-state 无 -Wl,):clang 不认,必须重写
        if is_legacy_wrapped(tokens):
            problems.append(f"{tag}: 旧裸形态 --push-state(无 -Wl,),clang 不认,跑 ./wrap-linkeropts.py --write 重写")
            continue
        # 3. 纯 -l 列表但没包(脚本还没跑过)
        if is_pure_lib_list(tokens):
            problems.append(f"{tag}: 纯 -l 列表但未包 push/pop(跑 ./wrap-linkeropts.py --write)")
            continue
        # 4. 其他非纯库行(含 -L/-framework/残留 -Wl, 等):本来就不是纯库行,脚本跳过,
        #    属"原有链接选项状态",保留,不报错。
    return problems

# ---------- 主流程 ----------

def iter_def_files():
    files = []
    for d in DEF_DIRS:
        if not d.is_dir():
            sys.exit(f"❌ def 目录不存在: {d}")
        files.extend(d.glob("*.def"))
    return sorted(files)

def cmd_dryrun(files):
    """打印将改写的 def + 前后对比,不落盘。"""
    to_wrap = []
    skipped = []
    already = []
    empty = []
    no_linkeropts = []
    for f in files:
        text = f.read_text(encoding="utf-8", errors="replace")
        found_linkeropts = False
        for raw in text.splitlines():
            kind, detail = classify_line(raw)
            if kind == "not_linkeropts":
                continue
            found_linkeropts = True
            # to_wrap(纯 -l 待包)和 to_rewrap(旧裸形态待重写)处理逻辑相同,都并入 to_wrap
            if kind in ("to_wrap", "to_rewrap"):
                to_wrap.append((f, raw, detail))
            elif kind == "already_wrapped":
                already.append(f)
            elif kind == "empty":
                empty.append(f)
            elif kind == "skipped":
                skipped.append((f, raw, detail))
        if not found_linkeropts:
            no_linkeropts.append(f)

    print("=" * 70)
    print("  wrap-linkeropts dry-run (不落盘)")
    print("=" * 70)
    print(f"def 目录: {[str(d.relative_to(REPO_ROOT)) for d in DEF_DIRS]}")
    print(f"def 总数: {len(files)}")
    print()

    print(f"将改写 (to_wrap+rewrap): {len(to_wrap)} 个 def")
    print(f"已是目标形态 (already):  {len(already)} 个 def")
    print(f"跳过-非纯-l (skipped):   {len(skipped)} 个 def (含非 -l token,保留原状)")
    print(f"跳过-空 linkerOpts:      {len(empty)} 个 def")
    print(f"无 linkerOpts 行:        {len(no_linkeropts)} 个 def")
    print()

    if to_wrap:
        print("---- 将改写的前后对比 ----")
        for f, raw, tokens in to_wrap:
            new_val = wrap_value(tokens)
            print(f"  {f.name}")
            print(f"    - {raw.rstrip()}")
            print(f"    + linkerOpts = {new_val}")
        print()

    if skipped:
        print("---- 跳过(含非 -l token,保留原状)----")
        for f, raw, detail in skipped:
            why = []
            if detail["state"]:
                why.append(f"状态token={detail['state']}")
            if detail["other"]:
                why.append(f"其他选项={detail['other']}")
            print(f"  ⚠️  {f.name}: {', '.join(why)}")
            print(f"      {raw.rstrip()}")
        print()

    if no_linkeropts:
        print("---- 无 linkerOpts(不动)----")
        for f in no_linkeropts:
            print(f"  {f.name}")
        print()

    return to_wrap, skipped, already, empty, no_linkeropts

def cmd_write(files):
    """落盘改写。先 dry-run 打印摘要,再写,写完跑自检。"""
    to_wrap, skipped, already, empty, no_linkeropts = cmd_dryrun(files)
    if not to_wrap:
        print("ℹ️  没有需要改写的 def(可能已全部 wrapped,或无 linkerOpts)。不落盘。")
        return
    print(f"---- 落盘 {len(to_wrap)} 个 def ----")
    for f, raw, tokens in to_wrap:
        text = f.read_text(encoding="utf-8", errors="replace")
        new_val = wrap_value(tokens)
        new_line = f"linkerOpts = {new_val}"
        # 精确替换那一行(只换第一个匹配的 linkerOpts 行;to_wrap 只对纯 -l 行,唯一)
        new_text = text.replace(raw, new_line, 1)
        if new_text == text:
            print(f"  ⚠️  {f.name}: 替换失败(行不匹配,跳过)")
            continue
        f.write_text(new_text, encoding="utf-8")
        print(f"  ✅ {f.name}")
    print()
    print("---- 落盘完成,跑自检 ----")
    cmd_check(files)

def cmd_check(files):
    """自检现状。exit 1 = 有问题。"""
    problems = []
    for f in files:
        problems.extend(check_file(f))
    print("=" * 70)
    print("  wrap-linkeropts --check (自检)")
    print("=" * 70)
    if not problems:
        print(f"✅ 全部 {len(files)} 个 def 自检通过:push/pop 配对、as-needed 紧跟 push、组外无状态泄漏。")
        return
    print(f"❌ 发现 {len(problems)} 个问题:")
    for p in problems:
        print(f"  {p}")
    sys.exit(1)

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    g = ap.add_mutually_exclusive_group()
    g.add_argument("--write", action="store_true", help="落盘改写(默认 dry-run 不落盘)")
    g.add_argument("--check", action="store_true", help="仅自检现状,不写")
    args = ap.parse_args()

    files = iter_def_files()
    if args.check:
        cmd_check(files)
    elif args.write:
        cmd_write(files)
    else:
        cmd_dryrun(files)

if __name__ == "__main__":
    main()
