#!/usr/bin/env python3
"""
Diagnose whether a static archive (.a) has room for code-size improvement
when linked into a shared library with --exclude-libs and --gc-sections.

Usage:
    python3 check_archive.py <archive.a> [--llvm-prefix <path>]

Two independent axes matter:
  1. Symbol visibility: DEFAULT vs HIDDEN
     - --exclude-libs only helps DEFAULT symbols (demotes them to local/hidden)
  2. Section granularity: -ffunction-sections vs monolithic .text
     - --gc-sections can only strip individual functions if each is in its own section

Best case:  DEFAULT visibility + -ffunction-sections  → both flags help
Worst case: HIDDEN visibility  + monolithic .text      → nothing to gain at link time
"""

import argparse
import subprocess
import sys
import re
import os
from collections import defaultdict


def find_tool(name, llvm_prefix):
    """Locate an llvm tool, preferring the given prefix."""
    if llvm_prefix:
        candidate = os.path.join(llvm_prefix, "bin", name)
        if os.path.isfile(candidate):
            return candidate
    # fall back to PATH
    for p in os.environ.get("PATH", "").split(os.pathsep):
        candidate = os.path.join(p, name)
        if os.path.isfile(candidate):
            return candidate
    return name  # hope for the best


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    return r.stdout


def analyse(archive, llvm_prefix):
    readelf = find_tool("llvm-readelf", llvm_prefix)
    objdump = find_tool("llvm-objdump", llvm_prefix)
    ar = find_tool("llvm-ar", llvm_prefix)

    # ── 1. List member objects ──────────────────────────────────────────
    members = run([ar, "t", archive]).strip().splitlines()
    print(f"Archive: {archive}")
    print(f"Member objects: {len(members)}")
    print()

    # ── 2. Check section granularity per object ─────────────────────────
    objdump_out = run([objdump, "-h", archive])
    current_obj = None
    obj_has_func_sections = {}
    obj_text_size = defaultdict(int)

    for line in objdump_out.splitlines():
        m = re.match(r"^.*\((.+?)\):", line)
        if m:
            current_obj = m.group(1)
            if current_obj not in obj_has_func_sections:
                obj_has_func_sections[current_obj] = False
            continue
        if current_obj and line.strip():
            parts = line.split()
            if len(parts) >= 3:
                sec_name = parts[1] if not parts[1].isdigit() else (parts[2] if len(parts) > 2 else "")
                # Sections like .text._ZN... indicate -ffunction-sections
                if sec_name.startswith(".text.") and sec_name != ".text":
                    obj_has_func_sections[current_obj] = True
                if sec_name.startswith(".text"):
                    try:
                        size_hex = parts[2] if not parts[1].isdigit() else parts[3]
                        obj_text_size[current_obj] += int(size_hex, 16)
                    except (ValueError, IndexError):
                        pass

    n_func_sections = sum(1 for v in obj_has_func_sections.values() if v)
    n_monolithic = sum(1 for v in obj_has_func_sections.values() if not v)

    # ── 3. Check symbol visibility ──────────────────────────────────────
    readelf_out = run([readelf, "-s", archive])

    total_global_default = 0
    total_global_hidden = 0
    total_weak_default = 0
    default_text_bytes = {}
    hidden_text_bytes = {}
    current_obj = None

    for line in readelf_out.splitlines():
        m = re.match(r"^File: (.+)\((.+?)\)", line)
        if m:
            current_obj = m.group(2)
            continue
        # Symbol table line:  Num: Value Size Type Bind Vis Ndx Name
        parts = line.split()
        if len(parts) < 8:
            continue
        try:
            sym_type = parts[3]
            sym_bind = parts[4]
            sym_vis = parts[5]
            sym_ndx = parts[6]
            sym_size = int(parts[2])
        except (ValueError, IndexError):
            continue

        if sym_type != "FUNC" or sym_ndx == "UND":
            continue

        if sym_bind == "GLOBAL" and sym_vis == "DEFAULT":
            total_global_default += 1
        elif sym_bind == "GLOBAL" and sym_vis == "HIDDEN":
            total_global_hidden += 1
        elif sym_bind == "WEAK" and sym_vis == "DEFAULT":
            total_weak_default += 1

    # ── 4. Print report ─────────────────────────────────────────────────
    total_text = sum(obj_text_size.values())

    print("=" * 64)
    print("  SECTION GRANULARITY  (-ffunction-sections)")
    print("=" * 64)
    print(f"  Objects with per-function sections (.text.*): {n_func_sections}")
    print(f"  Objects with monolithic .text:                {n_monolithic}")
    print()

    print("=" * 64)
    print("  SYMBOL VISIBILITY")
    print("=" * 64)
    print(f"  GLOBAL + DEFAULT  (exportable, --exclude-libs helps): {total_global_default}")
    print(f"  GLOBAL + HIDDEN   (already hidden, flag is a no-op):  {total_global_hidden}")
    print(f"  WEAK   + DEFAULT  (exportable, --exclude-libs helps): {total_weak_default}")
    print()

    print("=" * 64)
    print("  TOTAL CODE SIZE")
    print("=" * 64)
    print(f"  Sum of .text* sections: {total_text:,} bytes ({total_text / 1024 / 1024:.2f} MiB)")
    print()

    # ── 5. Verdict ──────────────────────────────────────────────────────
    print("=" * 64)
    print("  VERDICT")
    print("=" * 64)

    exportable = total_global_default + total_weak_default

    if exportable == 0 and n_func_sections == 0:
        print("  No room for improvement at link time.")
        print("  - All symbols are already HIDDEN → --exclude-libs is a no-op")
        print("  - No per-function sections → --gc-sections is all-or-nothing per .o")
        print("  ➜ Must rebuild the archive with -ffunction-sections -fdata-sections")
        print("    and/or -fvisibility=default to enable linker-side stripping.")
    elif exportable == 0 and n_func_sections > 0:
        print("  Partial room for improvement.")
        print("  - All symbols are already HIDDEN → --exclude-libs is a no-op")
        print(f"  - {n_func_sections} objects have per-function sections")
        print("  ➜ --gc-sections can already strip unreferenced functions from those objects.")
        print("    --exclude-libs won't add anything. Check if --gc-sections is enabled.")
    elif exportable > 0 and n_func_sections == 0:
        print("  Partial room for improvement.")
        print(f"  - {exportable} symbols are exportable → --exclude-libs will hide them")
        print("  - But no per-function sections → --gc-sections is coarse-grained")
        print("  ➜ --exclude-libs helps shrink .dynsym/.hash, but code removal is limited.")
        print("    Rebuild the archive with -ffunction-sections for better stripping.")
    else:
        print("  Good potential for code-size improvement!")
        print(f"  - {exportable} exportable symbols → --exclude-libs will hide them")
        print(f"  - {n_func_sections} objects have per-function sections")
        print("  ➜ Use --exclude-libs=<archive> with --gc-sections for best results.")

    print()


def main():
    parser = argparse.ArgumentParser(
        description="Check if a .a archive can benefit from --exclude-libs / --gc-sections"
    )
    parser.add_argument("archive", help="Path to the static archive (.a)")
    parser.add_argument(
        "--llvm-prefix",
        default=None,
        help="Path to LLVM toolchain root (contains bin/llvm-readelf etc.)",
    )
    args = parser.parse_args()

    if not os.path.isfile(args.archive):
        print(f"Error: {args.archive} not found", file=sys.stderr)
        sys.exit(1)

    analyse(args.archive, args.llvm_prefix)


if __name__ == "__main__":
    main()
