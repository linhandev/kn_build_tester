#!/usr/bin/env python3
"""Generate many Kotlin source files to reproduce ARG_MAX on the K/N Linux linker.

With incremental compilation (kotlin.incremental.native=true + cacheOrchestration=compiler),
each source file produces a per-file static cache .a file. All paths are passed
as individual arguments to ld.lld. With enough files, the command line exceeds
the OS ARG_MAX limit (2MB on Linux) and execve() fails with E2BIG.

Usage:
    python3 generate-sources.py [count]
    # default: 15000 files (enough to exceed 2MB ARG_MAX with ~140-byte paths)
    # minimum to reproduce: ~12000 on typical Linux with standard paths
"""
import os, sys

COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 15000
SRC_DIR = os.path.join("src", "commonMain", "kotlin", "generated")

os.makedirs(SRC_DIR, exist_ok=True)

for i in range(COUNT):
    path = os.path.join(SRC_DIR, f"File{i:05d}.kt")
    with open(path, "w") as f:
        f.write(f"package generated\n\nfun compute{i:05d}(): Int = {i}\n")

main_path = os.path.join("src", "commonMain", "kotlin", "Main.kt")
os.makedirs(os.path.dirname(main_path), exist_ok=True)
with open(main_path, "w") as f:
    f.write("import generated.*\n\n")
    f.write("fun main() {\n")
    f.write("    var sum = 0\n")
    for i in range(COUNT):
        f.write(f"    sum += compute{i:05d}()\n")
    f.write('    println("Sum = $sum")\n')
    f.write("}\n")

print(f"Generated {COUNT} source files in {SRC_DIR}/ and {main_path}")
