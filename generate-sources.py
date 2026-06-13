#!/usr/bin/env python3
"""Generate many Kotlin source files to reproduce ARG_MAX on the K/N Linux linker.

With static caches enabled (default), each source file produces a separate
cache .a file. The linker receives all of them as individual arguments,
blowing past the OS ARG_MAX limit on Linux (GccBasedLinker lacks @file support).

Usage:
    python3 generate-sources.py [count]
    # default: 200 files
"""
import os, sys

COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 200
SRC_DIR = os.path.join("src", "commonMain", "kotlin", "generated")

os.makedirs(SRC_DIR, exist_ok=True)

for i in range(COUNT):
    path = os.path.join(SRC_DIR, f"File{i:04d}.kt")
    with open(path, "w") as f:
        f.write(f"package generated\n\nfun compute{i:04d}(): Int = {i}\n")

main_path = os.path.join("src", "commonMain", "kotlin", "Main.kt")
os.makedirs(os.path.dirname(main_path), exist_ok=True)
with open(main_path, "w") as f:
    f.write("import generated.*\n\n")
    f.write("fun main() {\n")
    f.write("    var sum = 0\n")
    for i in range(COUNT):
        f.write(f"    sum += compute{i:04d}()\n")
    f.write('    println("Sum = $sum")\n')
    f.write("}\n")

print(f"Generated {COUNT} source files in {SRC_DIR}/ and {main_path}")
