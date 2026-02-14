#!/usr/bin/env python3
"""
Check whether a static archive (.a) was compiled with the right flags
to enable linker-side dead code elimination.

Usage:
    python3 check_archive.py <archive.a> [--llvm-prefix <path>]

Two compile-time properties matter:
  1. -ffunction-sections / -fdata-sections
     Each function gets its own section, allowing the linker's --gc-sections
     to strip unused functions individually (instead of all-or-nothing per .o).
  2. -fvisibility=hidden
     Symbols default to HIDDEN, so --exclude-libs is not needed at link time.
     If symbols are DEFAULT, the linker needs --exclude-libs to hide them
     before --gc-sections can treat them as non-roots.
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
    exportable = total_global_default + total_weak_default
    all_func_sections = n_monolithic == 0
    all_hidden = exportable == 0

    actions = []

    if not all_func_sections:
        actions.append(
            f"  - {n_monolithic} object(s) lack per-function sections\n"
            f"    Compile fix:  add -ffunction-sections -fdata-sections\n"
            f"    Link fix:     --gc-sections (all-or-nothing per .o without this compile fix)"
        )
    if not all_hidden:
        actions.append(
            f"  - {exportable} symbol(s) have DEFAULT visibility (exportable)\n"
            f"    Compile fix:  add -fvisibility=hidden\n"
            f"    Link fix:     --exclude-libs={os.path.basename(archive)}"
        )

    print("=" * 64)
    print("  RECOMMENDATIONS")
    print("=" * 64)

    if not actions:
        print("  No action required for the archive. It is already well-prepared:")
        print("  - All symbols have HIDDEN visibility")
        print("  - All objects use per-function sections (-ffunction-sections)")
        print()
        print("  At link time, use --gc-sections to strip unreferenced functions.")
    else:
        print("  Room for improvement preparing the archive:\n")
        print("\n\n".join(actions))
        print()
        print("  At link time, use --gc-sections to strip unreferenced functions.")
    print()


def main():
    parser = argparse.ArgumentParser(
        description="Check if a .a archive is compiled correctly for linker dead code elimination"
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
