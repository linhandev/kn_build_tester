# Kotlin Code Snippet Generator

A proof-of-concept tool for generating random Kotlin code snippets using ANTLR4 grammar. This tool is designed for fuzz testing Kotlin Native compilers by generating a large number of syntactically valid Kotlin code samples.

## Features

- Generate syntactically correct Kotlin code snippets
- Control code complexity (nesting depth, statement count)
- Batch generation for large-scale testing
- Python-based implementation for easy integration

## Installation

1. Install Python 3.8 or higher

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Install ANTLR4 (if not already installed):
```bash
# On Ubuntu/Debian
sudo apt-get install antlr4

# On macOS
brew install antlr

# Or download from https://www.antlr.org/download.html
```

## Usage

### Basic Usage

Generate a single Kotlin code snippet:
```bash
python generator.py
```

### Generate Multiple Snippets

Generate 10 code snippets:
```bash
python generator.py --count 10
```

### Control Complexity

Generate snippets with specific complexity:
```bash
# Low complexity (max depth: 3, max statements: 10)
python generator.py --complexity low

# Medium complexity (max depth: 5, max statements: 20)
python generator.py --complexity medium

# High complexity (max depth: 8, max statements: 40)
python generator.py --complexity high

# Custom complexity
python generator.py --max-depth 6 --max-statements 25
```

### Save to Files

Save generated snippets to separate files:
```bash
python generator.py --count 100 --output-dir ./kotlin_samples
```

### Run Tests

```bash
python test_generator.py
```

## How It Works

### Architecture

1. **ANTLR4 Grammar**: Uses the official Kotlin grammar from the grammars-v4 repository
2. **Random Generation**: Traverses grammar rules to generate random but syntactically valid code
3. **Complexity Control**: Limits recursion depth and statement count to control output size

### Implementation Details

The generator works by:

1. **Parsing Grammar Rules**: ANTLR4 parses the Kotlin grammar to understand language structure
2. **Random Rule Selection**: When multiple alternatives exist in a grammar rule, the generator randomly selects one
3. **Recursive Generation**: Recursively expands rules while respecting depth limits
4. **Terminal Generation**: Generates appropriate identifiers, literals, and keywords

### Complexity Parameters

- **Max Depth**: Maximum nesting level of code structures (classes, functions, loops, etc.)
- **Max Statements**: Maximum number of statements to generate in a scope
- **Branching Factor**: Probability of choosing optional grammar elements

### Known Limitations

- ANTLR4 primarily generates syntactically correct code (not semantically correct)
- Generated code may not be meaningful or compilable without imports
- Type checking and semantic correctness are not guaranteed
- Generating incorrect code requires manual rule breaking (not natively supported by ANTLR4)

## Examples

### Example Output (Low Complexity)

```kotlin
fun main() {
    val x = 42
    println(x)
}
```

### Example Output (Medium Complexity)

```kotlin
class MyClass {
    var property: Int = 0
    
    fun method(param: String): Boolean {
        if (param.isEmpty()) {
            return false
        }
        return true
    }
}
```

### Example Output (High Complexity)

```kotlin
interface MyInterface {
    fun interfaceMethod(): Unit
}

class ComplexClass : MyInterface {
    private val list = mutableListOf<Int>()
    
    override fun interfaceMethod() {
        for (i in 0..10) {
            list.add(i)
            if (i % 2 == 0) {
                println("Even: $i")
            } else {
                println("Odd: $i")
            }
        }
    }
    
    companion object {
        const val CONSTANT = "value"
    }
}
```

## Contributing

This is a proof-of-concept tool. For production use, consider:
- Adding semantic validation
- Implementing type checking
- Supporting more Kotlin features
- Adding negative test case generation

## License

MIT License
