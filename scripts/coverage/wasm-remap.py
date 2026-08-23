#!/usr/bin/env python3
"""wasm-remap.py — remap %DebugCollectWasmCoverage ranges to .kt source via .wasm.map.

Usage: wasm-remap.py <wasm.map> <wasm-binary> <cov.json> [--lcov <out.info>]

The wasm source map (v3) emitted by the Kotlin/Wasm compiler stores generated
positions as BINARY-ABSOLUTE byte offsets (address + code_section_offset, same
convention as emscripten wasm-sourcemap.py). %DebugCollectWasmCoverage returns
ranges as CODE-SECTION-RELATIVE offsets. So: binary_offset = collected.start + code_section_base.
We find the code section base by parsing the wasm binary, then for each collected
range find the nearest preceding source-map segment and attribute its (source, line).
"""
import json, sys, os, bisect, argparse

B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
SHIFTS = {c: i for i, c in enumerate(B64)}


def decode_vlq(s):
    res, val, bits, cur = [], 0, 0, []
    for c in s:
        if c == ',':
            if cur: res.append(cur); cur = []
            continue
        d = SHIFTS[c]
        val |= (d & 31) << bits
        if d & 32:
            bits += 5
        else:
            sign = -1 if (val & 1) else 1
            cur.append(sign * (val >> 1)); val = 0; bits = 0
    if cur: res.append(cur)
    return res


def code_section_offset(wasm_bytes):
    """Return the byte offset of the wasm code (section id 10) in the binary."""
    b = wasm_bytes
    if b[:4] != b"\x00asm":
        raise ValueError("not a wasm binary")
    i = 8
    while i < len(b):
        sid = b[i]; i += 1
        size = 0; shift = 0
        while True:
            x = b[i]; i += 1
            size |= (x & 0x7f) << shift; shift += 7
            if not (x & 0x80): break
        if sid == 10:  # code section
            return i, size
        i += size
    raise ValueError("no code section in wasm binary")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("wasm_map")
    ap.add_argument("wasm_bin")
    ap.add_argument("cov_json")
    ap.add_argument("--lcov")
    a = ap.parse_args()

    mp = json.load(open(a.wasm_map))
    segs = decode_vlq(mp["mappings"])
    # accumulate all fields as running deltas (standard source map; wasm uses one "line")
    abs_off = si = sl = sc = 0
    mapped = []
    for s in segs:
        abs_off += s[0]
        if len(s) > 1: si += s[1]
        if len(s) > 2: sl += s[2]
        if len(s) > 3: sc += s[3]
        mapped.append((abs_off, si, sl, sc))
    sources = mp["sources"]

    base, _ = code_section_offset(open(a.wasm_bin, "rb").read())

    cov = json.load(open(a.cov_json))
    # %DebugCollectWasmCoverage -> list of modules, each a list of {start,end,count}
    if isinstance(cov, list) and cov and isinstance(cov[-1], list):
        ranges = cov[-1]
    else:
        ranges = cov
    offs = sorted(m[0] for m in mapped)

    per_file = {i: {"covered": 0, "total": 0, "lines": {}} for i in range(len(sources))}
    unmapped = 0
    for r in ranges:
        binoff = r["start"] + base
        i = bisect.bisect_right(offs, binoff) - 1
        if i < 0:
            unmapped += 1; continue
        _, sidx, sline, _ = mapped[i]
        if sidx < 0 or sidx >= len(sources):
            unmapped += 1; continue
        pf = per_file[sidx]
        pf["total"] += 1
        pf["lines"].setdefault(sline, 0)
        if r["count"] > 0:
            pf["covered"] += 1
            pf["lines"][sline] += 1

    print(f"wasm source map: {len(mapped)} segments, code section base = {base}")
    print(f"collected ranges: {len(ranges)} (unmapped: {unmapped})")
    print(f"{'file':40s} {'ranges':>14s} {'lines':>12s}")
    lcov = []
    for i, s in enumerate(sources):
        pf = per_file[i]
        tot = pf["total"]; cc = pf["covered"]
        pct = 100.0 * cc / tot if tot else 0
        nl = len(pf["lines"]); cl = sum(1 for v in pf["lines"].values() if v > 0)
        name = os.path.basename(s)
        print(f"  {name:38s} {cc:5d}/{tot:<5d} {pct:5.1f}%  lines {cl}/{nl}")
        if a.lcov:
            lcov.append(f"TN:\nSF:{s}\n")
            for ln, hit in sorted(pf["lines"].items()):
                lcov.append(f"DA:{ln},{hit}\n")
            lcov.append(f"LF:{nl}\nLH:{cl}\nend_of_record\n")
    if a.lcov:
        with open(a.lcov, "w") as f:
            f.write("".join(lcov))
        print(f"\nlcov written: {a.lcov}")


if __name__ == "__main__":
    main()
