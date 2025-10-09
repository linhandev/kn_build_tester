# Quick Start Guide

Get started with the Kotlin Code Generator in 5 minutes!

## Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd kn_samples
```

2. **Install dependencies**
```bash
pip install -r requirements.txt
```

3. **Download ANTLR4 tool** (if not already installed)
```bash
# Download the JAR file
curl -O https://www.antlr.org/download/antlr-4.13.1-complete.jar

# Or on Ubuntu/Debian
sudo apt-get install antlr4

# Or on macOS
brew install antlr
```

4. **Generate the parsers** (already done if you cloned the repo)
```bash
cd grammar
java -jar /path/to/antlr-4.13.1-complete.jar -Dlanguage=Python3 KotlinLexer.g4 KotlinParser.g4
cd ..
```

## Basic Usage

### Generate a single snippet
```bash
python generator.py
```

Output:
```kotlin
fun main() {
    val var0: Int = 42
    println(var0)
    for (i1 in 0..5) {
        println("str2")
    }
}
```

### Generate with different complexity levels

**Low complexity** (simple code):
```bash
python generator.py --complexity low
```

**Medium complexity** (balanced):
```bash
python generator.py --complexity medium
```

**High complexity** (nested structures):
```bash
python generator.py --complexity high
```

### Generate multiple snippets

Generate 10 snippets:
```bash
python generator.py --count 10
```

Generate 100 snippets to files:
```bash
python generator.py --count 100 --output-dir ./samples
```

### Custom complexity

Generate with custom parameters:
```bash
python generator.py --max-depth 6 --max-statements 30
```

## Common Use Cases

### For Fuzz Testing

Generate 1000 test cases:
```bash
python generator.py --count 1000 --complexity medium --output-dir ./test_cases
```

### For Compiler Comparison

Generate reproducible test cases:
```bash
python generator.py --count 100 --seed 12345 --output-dir ./baseline
```

Later, regenerate the same cases:
```bash
python generator.py --count 100 --seed 12345 --output-dir ./verify
```

### Quick Examples

Generate a few examples to see what's possible:
```bash
python generator.py --count 3 --complexity low
python generator.py --count 3 --complexity medium  
python generator.py --count 3 --complexity high
```

## Viewing Examples

Pre-generated examples are in the `examples/` directory:
```bash
cat examples/low_complexity.kt
cat examples/medium_complexity.kt
cat examples/high_complexity.kt
```

## Testing

Run the test suite:
```bash
python test_generator.py
```

Verbose output:
```bash
python test_generator.py -v
```

## Next Steps

- Read the [README.md](README.md) for detailed documentation
- Read [TECHNICAL_DOCS.md](TECHNICAL_DOCS.md) to understand how it works
- Modify `generator.py` to add new Kotlin constructs
- Integrate with your compiler testing workflow

## Troubleshooting

**Problem**: `ModuleNotFoundError: No module named 'antlr4'`
- **Solution**: Run `pip install antlr4-python3-runtime`

**Problem**: `ModuleNotFoundError: No module named 'KotlinParser'`
- **Solution**: Make sure you generated the parsers with ANTLR4 (step 4 above)

**Problem**: Generated code has errors
- **Note**: The generated code is syntactically correct but may not be semantically valid. Variables might be used before declaration, which is expected for this POC.

## Help

View all options:
```bash
python generator.py --help
```

## Feedback

This is a proof-of-concept. For production use, consider:
- Adding semantic validation
- Expanding Kotlin feature support
- Implementing negative test generation (incorrect code)

Enjoy generating Kotlin code!
