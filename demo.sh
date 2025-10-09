#!/bin/bash
# Demonstration script for Kotlin Code Generator

echo "==============================================="
echo "Kotlin Code Generator - Demonstration"
echo "==============================================="
echo ""

echo "1. Generate a simple snippet (low complexity):"
echo "   Command: python generator.py --complexity low"
echo ""
python generator.py --complexity low --seed 42
echo ""

echo "-----------------------------------------------"
echo ""
echo "2. Generate multiple snippets:"
echo "   Command: python generator.py --count 3 --complexity medium"
echo ""
python generator.py --count 3 --complexity medium --seed 100
echo ""

echo "-----------------------------------------------"
echo ""
echo "3. Generate to files:"
echo "   Command: python generator.py --count 5 --output-dir /tmp/demo_output"
echo ""
python generator.py --count 5 --complexity medium --output-dir /tmp/demo_output
echo ""
echo "Generated files:"
ls -lh /tmp/demo_output/
echo ""

echo "-----------------------------------------------"
echo ""
echo "4. Run tests:"
echo "   Command: python test_generator.py"
echo ""
python test_generator.py
echo ""

echo "==============================================="
echo "Demonstration complete!"
echo "See README.md for more usage examples"
echo "==============================================="
