#!/usr/bin/env python3
"""
Scan OpenHarmony + HMS sysroot headers for C/C++ grammar patterns.

Produces a searchable JSON database of every declaration with grammar tags
and source text, plus a coverage report on stderr showing handled vs
unhandled AST node kinds.

Usage:
    python3 scan_sysroot_grammar.py > report.json 2> coverage.log
    python3 scan_sysroot_grammar.py --single database/rdb/relational_store.h > single.json 2> coverage.log
"""
import sys
import json
import argparse
from pathlib import Path
from collections import Counter

import clang.cindex as ci

from scanner.config import OH_SYSROOT, HMS_SYSROOT
from scanner.helpers import FileCache
from scanner.coverage import Coverage
from scanner.walker import walk
from scanner.parse import find_addtogroup_headers, generate_umbrella, parse_tu


def main():
    parser = argparse.ArgumentParser(description="Scan sysroot headers for grammar patterns")
    parser.add_argument("--single", metavar="HEADER",
                        help="Scan a single header (relative to sysroot/usr/include/ or absolute)")
    parser.add_argument("--library-path", metavar="PATH",
                        help="Override libclang library path")
    args = parser.parse_args()

    if args.library_path:
        ci.Config.set_library_path(args.library_path)

    index = ci.Index.create()
    fc = FileCache()
    cov = Coverage()
    entries: list[dict] = []

    if args.single:
        hdr = args.single
        if not Path(hdr).is_absolute():
            for sysroot in (OH_SYSROOT, HMS_SYSROOT):
                candidate = Path(sysroot) / "usr" / "include" / hdr
                if candidate.exists():
                    hdr = str(candidate)
                    break
        hdr = str(Path(hdr).resolve())
        allow_paths = frozenset({str(Path(hdr).resolve().parent) + "/"})
        print(f"Parsing single file: {hdr}", file=sys.stderr)
        atg = {hdr}
        tu = parse_tu(index, hdr)
        walk(tu.cursor, entries, fc, atg, cov, allow_paths=allow_paths)
    else:
        print("Finding @addtogroup headers ...", file=sys.stderr)
        headers = find_addtogroup_headers()
        print(f"  Found {len(headers)} headers", file=sys.stderr)
        atg = set(headers)
        umbrella = "/tmp/sysroot_umbrella.h"
        generate_umbrella(headers, umbrella)
        print(f"Parsing umbrella header ({len(headers)} includes) ...", file=sys.stderr)
        tu = parse_tu(index, umbrella)
        print("Walking AST ...", file=sys.stderr)
        walk(tu.cursor, entries, fc, atg, cov)

    print(f"\nCataloged {len(entries)} declarations", file=sys.stderr)

    tag_counts: Counter = Counter()
    for e in entries:
        for t in e["tags"]:
            tag_counts[t] += 1

    cov.print_stderr()

    print(f"\nUnique grammar tags: {len(tag_counts)}", file=sys.stderr)
    for tag, cnt in tag_counts.most_common():
        print(f"  {tag:40s} {cnt:>6d}", file=sys.stderr)

    output = {
        "entries": entries,
        "coverage": cov.report(),
        "stats": {
            "total_entries": len(entries),
            "tag_counts": dict(tag_counts.most_common()),
        },
    }
    json.dump(output, sys.stdout, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    main()
