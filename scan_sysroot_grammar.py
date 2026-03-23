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
from scanner.helpers import FileCache, relative_path
from scanner.coverage import Coverage
from scanner.walker import walk
from scanner.parse import find_addtogroup_headers, generate_umbrella, parse_tu


def build_function_group_stats(entries: list[dict]) -> dict:
    function_keys: Counter = Counter()
    function_x_return: Counter = Counter()
    function_x_param: Counter = Counter()
    full_signature: Counter = Counter()
    return_shapes: Counter = Counter()
    param_shapes: Counter = Counter()

    for entry in entries:
        matrix = entry.get("matrix_keys", {})
        return_group = entry.get("return", {})
        param_groups = entry.get("parameters", [])

        fgk = entry.get("function_group_key")
        if fgk:
            function_keys[fgk] += 1
        fxr = matrix.get("function_x_return")
        if fxr:
            function_x_return[fxr] += 1
        fs = matrix.get("full_signature")
        if fs:
            full_signature[fs] += 1
        ret_key = return_group.get("key")
        if ret_key:
            return_shapes[ret_key] += 1
        for fxp in matrix.get("function_x_params", []):
            function_x_param[fxp] += 1
        for param in param_groups:
            pk = param.get("key")
            if pk:
                param_shapes[pk] += 1

    return {
        "function_keys": dict(function_keys.most_common()),
        "function_x_return": dict(function_x_return.most_common()),
        "function_x_param": dict(function_x_param.most_common()),
        "full_signature": dict(full_signature.most_common()),
        "return_shapes": dict(return_shapes.most_common()),
        "param_shapes": dict(param_shapes.most_common()),
    }


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
        primary_rel = relative_path(hdr)
        entries = [e for e in entries if e.get("file") == primary_rel]
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

    functions = [e for e in entries if e.get("kind") == "function"]

    print(f"\nCataloged {len(functions)} function declarations", file=sys.stderr)

    cov.print_stderr()
    function_groups = build_function_group_stats(functions)

    print(f"\nUnique function group keys: {len(function_groups['function_keys'])}", file=sys.stderr)
    for key, cnt in function_groups["function_keys"].items():
        print(f"  {key:60s} {cnt:>6d}", file=sys.stderr)

    print(f"\nUnique return shapes: {len(function_groups['return_shapes'])}", file=sys.stderr)
    for key, cnt in function_groups["return_shapes"].items():
        print(f"  {key:60s} {cnt:>6d}", file=sys.stderr)

    print(f"\nUnique param shapes: {len(function_groups['param_shapes'])}", file=sys.stderr)
    for key, cnt in function_groups["param_shapes"].items():
        print(f"  {key:60s} {cnt:>6d}", file=sys.stderr)

    print(f"\nUnique function x return groups: {len(function_groups['function_x_return'])}", file=sys.stderr)
    for key, cnt in list(function_groups["function_x_return"].items())[:50]:
        print(f"  {key:80s} {cnt:>6d}", file=sys.stderr)

    print(f"\nUnique function x param groups: {len(function_groups['function_x_param'])}", file=sys.stderr)
    for key, cnt in list(function_groups["function_x_param"].items())[:80]:
        print(f"  {key:80s} {cnt:>6d}", file=sys.stderr)

    output = {
        "functions": functions,
        "coverage": cov.report(),
        "stats": {
            "total_functions": len(functions),
            "function_groups": function_groups,
        },
    }
    json.dump(output, sys.stdout, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    main()
