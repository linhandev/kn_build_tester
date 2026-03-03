import sys
import subprocess
from pathlib import Path
from collections import Counter

import clang.cindex as ci

from .config import (
    OH_SYSROOT, HMS_SYSROOT,
    LLVM_CXX_INCLUDE, LLVM_CLANG_INCLUDE,
    EXCLUDED_HEADERS, INCLUDE_PREFIXES,
)


def _is_excluded(header_path: str) -> bool:
    for prefix in INCLUDE_PREFIXES:
        if header_path.startswith(prefix):
            rel = header_path[len(prefix):]
            if rel in EXCLUDED_HEADERS:
                return True
    return False


def find_addtogroup_headers() -> list[str]:
    headers: list[str] = []
    for sysroot in (OH_SYSROOT, HMS_SYSROOT):
        include_dir = Path(sysroot) / "usr" / "include"
        if not include_dir.exists():
            continue
        result = subprocess.run(
            ["grep", "-rl", "@addtogroup", str(include_dir)],
            capture_output=True, text=True,
        )
        for line in result.stdout.strip().split("\n"):
            line = line.strip()
            if line and line.endswith(".h") and not _is_excluded(line):
                headers.append(line)
    return sorted(set(headers))


def generate_umbrella(headers: list[str], path: str):
    with open(path, "w") as f:
        for h in headers:
            f.write(f'#include "{h}"\n')
    return path


def parse_flags() -> list[str]:
    return [
        "-x", "c++",
        "-std=c++17",
        "--target=aarch64-linux-ohos",
        f"--sysroot={OH_SYSROOT}",
        f"-isystem{LLVM_CXX_INCLUDE}",
        f"-isystem{LLVM_CLANG_INCLUDE}",
        f"-isystem{OH_SYSROOT}/usr/include",
        f"-isystem{OH_SYSROOT}/usr/include/aarch64-linux-ohos",
        f"-isystem{OH_SYSROOT}/usr/include/TEEKit/tee",
        f"-isystem{HMS_SYSROOT}/usr/include",
        "-D__OHOS__",
        "-D__aarch64__",
        "-D__MUSL__",
        "-w",
        "-ferror-limit=0",
    ]


def parse_tu(index, filepath: str):
    opts = (ci.TranslationUnit.PARSE_DETAILED_PROCESSING_RECORD
            | ci.TranslationUnit.PARSE_SKIP_FUNCTION_BODIES)
    tu = index.parse(filepath, args=parse_flags(), options=opts)
    diag_counts = Counter()
    for d in tu.diagnostics:
        diag_counts[d.severity] += 1
        if d.severity >= ci.Diagnostic.Error:
            print(f"  DIAG: {d}", file=sys.stderr)
    print(f"  Diagnostics: {dict(diag_counts)}", file=sys.stderr)
    return tu
