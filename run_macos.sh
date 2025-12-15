#!/bin/bash

set -e

echo "--- Test 1: Manual Electric Fence (mprotect) ---"
echo "Compiling without ASan..."
clang++ \
      -g \
      -Wall -Wextra \
      -std=c++17 \
      -o protected_buffer_demo protected_buffer_demo.cpp

echo "Running demo (expecting mprotect crashes caught by signal handler)..."
./protected_buffer_demo

echo ""
echo "--- Test 2: Stock ASan ---"
echo "Compiling with ASan..."
clang++ \
      -fsanitize=address \
      -g \
      -Wall -Wextra \
      -std=c++17 \
      -o protected_buffer_demo_asan protected_buffer_demo.cpp

echo "Running demo with ASan..."
# We expect ASan to catch issues. 
# Note: The signal handler in the code catches SIGSEGV/SIGBUS.
# ASan installs its own signal handlers.
# If ASan detects an error, it usually prints a report and exits with non-zero.
# However, our code catches signals.
# If ASan is active, it might intercept the signal before our handler, OR our handler might run.
# But for "NoGuard", we are relying on ASan to detect the overflow that mprotect DOESN'T catch.

./protected_buffer_demo_asan
