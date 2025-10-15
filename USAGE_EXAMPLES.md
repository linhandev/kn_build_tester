# Usage Examples

This document provides practical examples for using the Kotlin Code Generator.

## Quick Examples

### 1. Basic Generation

Generate a single snippet to stdout:
```bash
python generator.py
```

Output:
```kotlin
fun main() {
    val var0: Int = 42
    println(var0)
}
```

### 2. Complexity Levels

**Low Complexity** (simple, shallow code):
```bash
python generator.py --complexity low
```

**Medium Complexity** (balanced complexity):
```bash
python generator.py --complexity medium
```

**High Complexity** (deep nesting, more statements):
```bash
python generator.py --complexity high
```

### 3. Multiple Snippets

Generate 10 snippets to stdout:
```bash
python generator.py --count 10 --complexity medium
```

### 4. Save to Files

Generate 100 files in a directory:
```bash
python generator.py --count 100 --output-dir ./kotlin_samples
```

The files will be named: `sample_0000.kt`, `sample_0001.kt`, etc.

### 5. Custom Complexity

Fine-tune the generation parameters:
```bash
# Max depth 6, max 25 statements
python generator.py --max-depth 6 --max-statements 25

# Very simple code
python generator.py --max-depth 2 --max-statements 5

# Complex code
python generator.py --max-depth 10 --max-statements 50
```

### 6. Reproducible Generation

Use a seed for reproducible output:
```bash
# First run
python generator.py --seed 42 --count 10 --output-dir ./baseline

# Later - generates identical files
python generator.py --seed 42 --count 10 --output-dir ./verify

# Compare
diff -r ./baseline ./verify  # Should show no differences
```

## Practical Use Cases

### For Compiler Fuzz Testing

Generate a large test suite:
```bash
# Generate 10,000 test cases across complexity levels
python generator.py --count 3333 --complexity low --output-dir ./test_suite/low
python generator.py --count 3333 --complexity medium --output-dir ./test_suite/medium
python generator.py --count 3334 --complexity high --output-dir ./test_suite/high
```

### For Compiler Comparison

Generate baseline for before/after testing:
```bash
# Create reproducible baseline
python generator.py --count 1000 --seed 12345 --output-dir ./baseline_tests

# Test with compiler version 1
kotlinc-native baseline_tests/*.kt > results_v1.txt 2>&1

# After compiler modifications, regenerate same tests
python generator.py --count 1000 --seed 12345 --output-dir ./verify_tests

# Test with compiler version 2  
kotlinc-native verify_tests/*.kt > results_v2.txt 2>&1

# Compare results
diff results_v1.txt results_v2.txt
```

### For Performance Testing

Generate code samples of specific complexity:
```bash
# Small programs (low overhead)
python generator.py --count 1000 --max-depth 3 --max-statements 10 \
  --output-dir ./perf_test/small

# Large programs (stress test)
python generator.py --count 100 --max-depth 10 --max-statements 100 \
  --output-dir ./perf_test/large
```

### For Regression Testing

Create a stable test suite:
```bash
# Generate regression test suite (fixed seed)
python generator.py --count 500 --seed 999 \
  --complexity medium --output-dir ./regression_suite

# Add to version control
git add regression_suite/
git commit -m "Add regression test suite"
```

## Integration Examples

### Shell Script Integration

```bash
#!/bin/bash
# generate_and_test.sh

OUTPUT_DIR="./test_output"
RESULTS_DIR="./results"

# Generate test cases
echo "Generating test cases..."
python generator.py --count 100 --complexity medium --output-dir "$OUTPUT_DIR"

# Compile each file
echo "Compiling..."
mkdir -p "$RESULTS_DIR"
for file in "$OUTPUT_DIR"/*.kt; do
    base=$(basename "$file" .kt)
    kotlinc-native "$file" -output "$RESULTS_DIR/$base" 2>&1 | \
        tee "$RESULTS_DIR/$base.log"
done

echo "Done! Check $RESULTS_DIR for compilation results"
```

### Python Integration

```python
#!/usr/bin/env python3
# test_runner.py

import subprocess
import os

def generate_tests(count, output_dir):
    """Generate Kotlin test files"""
    cmd = [
        'python', 'generator.py',
        '--count', str(count),
        '--complexity', 'medium',
        '--output-dir', output_dir
    ]
    subprocess.run(cmd, check=True)

def compile_file(kotlin_file):
    """Compile a Kotlin file and return success status"""
    result = subprocess.run(
        ['kotlinc-native', kotlin_file],
        capture_output=True,
        text=True
    )
    return result.returncode == 0

def main():
    # Generate tests
    output_dir = './generated_tests'
    generate_tests(100, output_dir)
    
    # Compile each file
    success_count = 0
    fail_count = 0
    
    for filename in os.listdir(output_dir):
        if filename.endswith('.kt'):
            filepath = os.path.join(output_dir, filename)
            if compile_file(filepath):
                success_count += 1
            else:
                fail_count += 1
                print(f"Failed: {filename}")
    
    print(f"Results: {success_count} passed, {fail_count} failed")

if __name__ == '__main__':
    main()
```

### Makefile Integration

```makefile
# Makefile

GENERATOR = python generator.py
TEST_DIR = tests
COUNT = 100

.PHONY: all clean generate test

all: generate

generate:
$(GENERATOR) --count $(COUNT) --output-dir $(TEST_DIR)

test: generate
@for file in $(TEST_DIR)/*.kt; do \
echo "Testing $$file..."; \
kotlinc-native $$file || echo "Failed: $$file"; \
done

clean:
rm -rf $(TEST_DIR)

regenerate: clean generate
```

## Advanced Usage

### Custom Seed for Different Test Suites

```bash
# Suite A - Basic operations
python generator.py --count 500 --seed 100 \
  --complexity low --output-dir ./suite_a

# Suite B - Control flow
python generator.py --count 500 --seed 200 \
  --complexity medium --output-dir ./suite_b

# Suite C - Complex structures  
python generator.py --count 500 --seed 300 \
  --complexity high --output-dir ./suite_c
```

### Parallel Generation

```bash
# Generate multiple suites in parallel
python generator.py --count 250 --complexity low --output-dir ./parallel_1 &
python generator.py --count 250 --complexity medium --output-dir ./parallel_2 &
python generator.py --count 250 --complexity high --output-dir ./parallel_3 &
python generator.py --count 250 --max-depth 7 --output-dir ./parallel_4 &

# Wait for all to complete
wait

echo "Generated $(find ./parallel_* -name '*.kt' | wc -l) files"
```

### Filtered Generation

```bash
# Generate until you have 100 files that compile
count=0
while [ $count -lt 100 ]; do
    python generator.py --output-dir ./temp
    file="./temp/sample_0000.kt"
    if kotlinc-native "$file" 2>/dev/null; then
        mv "$file" "./valid/sample_$(printf '%04d' $count).kt"
        ((count++))
        echo "Valid files: $count/100"
    fi
done
```

## Performance Tips

### Fast Generation
- Use low complexity for maximum speed
- Avoid disk I/O by using stdout (no --output-dir)
- Generate in batches rather than one at a time

### Large Batches
```bash
# Generate 10,000 files efficiently
python generator.py --count 10000 --complexity medium \
  --output-dir ./large_batch
```

### Memory Considerations
- Each file is generated independently (no memory accumulation)
- Can generate millions of files without memory issues
- Limited only by disk space

## Validation

### Check Generated Code

Use the test suite:
```bash
python test_generator.py
```

### Manual Inspection

```bash
# Generate a few examples to inspect
python generator.py --count 5 --complexity medium

# Save and view
python generator.py --count 1 --output-dir ./inspect
cat ./inspect/sample_0000.kt
```

### Compilation Check

```bash
# Generate and try to compile
python generator.py --output-dir ./compile_test
kotlinc ./compile_test/sample_0000.kt
```

Note: Compilation may fail due to semantic issues (undefined variables), but syntax should always be valid.

## Troubleshooting

### Issue: Output looks too similar

**Solution**: Increase complexity or use different seeds
```bash
python generator.py --complexity high --seed $RANDOM
```

### Issue: Code is too simple

**Solution**: Increase depth and statements
```bash
python generator.py --max-depth 10 --max-statements 50
```

### Issue: Code is too complex

**Solution**: Decrease parameters
```bash
python generator.py --max-depth 2 --max-statements 10
```

## Summary

The generator is flexible and can be used for:
- ✅ Fuzz testing
- ✅ Regression testing
- ✅ Performance testing
- ✅ Compiler comparison
- ✅ Stress testing
- ✅ Code coverage analysis

See `README.md` for more information and `TECHNICAL_DOCS.md` for implementation details.
