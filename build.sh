#!/bin/bash
set -e

CXX=clang++
CXXFLAGS="-std=c++17"

echo "Building callee.o..."
$CXX $CXXFLAGS -c -o callee.o callee.cpp

echo "Building caller-no-lib..."
$CXX $CXXFLAGS -o caller-no-lib caller.cpp

echo "Building caller-with-lib..."
$CXX $CXXFLAGS -o caller-with-lib caller.cpp callee.o

echo "Build complete!"

echo "Running caller-no-lib..."
./caller-no-lib

echo "Running caller-with-lib..."
./caller-with-lib