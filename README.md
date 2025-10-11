# C++ Header Parser Sample Project

A simple Python-based tool to parse C++ header files and extract function information (count and names).

## Overview

This sample project demonstrates how to parse C++ header files using Python with regular expressions. It extracts:
- **Number of functions** declared in the header
- **Function names**

The parser handles various C++ function declaration styles while safely ignoring:
- Macros and preprocessor directives
- Comments (both single-line `//` and multi-line `/* */`)
- Common C++ modifiers (virtual, static, inline, etc.)

## Files

- `cpp_header_parser.py` - The main Python script that performs the parsing
- `sample.h` - A sample C++ header file with various function declarations
- `README.md` - This file

## Requirements

- Python 3.6 or higher
- No external dependencies required (uses only Python standard library)

## Usage

### Basic Usage

```bash
python cpp_header_parser.py <header_file.h>
```

### Example

```bash
python cpp_header_parser.py sample.h
```

### Expected Output

```
Parsing C++ header file: sample.h
============================================================

Total number of functions: 17

Function names:
  1. initializeSystem
  2. calculateSum
  3. computeAverage
  4. formatMessage
  5. validateInput
  6. setConfiguration
  7. processData
  8. allocateBuffer
  9. process
  10. getCount
  11. setName
  12. update
  13. reset
  14. getMaxValue
  15. printDebugInfo
  16. convertToInt
  17. processArray

============================================================
Parsing complete!
```

## Features

### Supported Function Patterns

The parser can identify:

1. **Simple function declarations**
   ```cpp
   void initializeSystem();
   int calculateSum(int a, int b);
   ```

2. **Functions with complex return types**
   ```cpp
   std::string formatMessage(const std::string& message, int priority);
   std::vector<int> processArray(const std::vector<int>& input);
   ```

3. **Functions with default parameters**
   ```cpp
   void setConfiguration(int timeout = 30, bool verbose = false);
   ```

4. **Functions with pointers and references**
   ```cpp
   void processData(int* data, size_t& size);
   float* allocateBuffer(size_t elements);
   ```

5. **Class member functions**
   ```cpp
   class DataProcessor {
   public:
       void process();
       int getCount() const;
       virtual void update();
       static void reset();
   };
   ```

6. **Template functions**
   ```cpp
   template<typename T>
   T getMaxValue(T a, T b);
   ```

7. **Namespace functions**
   ```cpp
   namespace Utils {
       void printDebugInfo();
       int convertToInt(const std::string& str);
   }
   ```

8. **Multi-line function declarations**
   ```cpp
   std::vector<int> 
   processArray(const std::vector<int>& input);
   ```

### What Gets Ignored

- **Macros**: `#define`, `#include`, `#ifndef`, etc.
- **Comments**: Both `//` single-line and `/* */` multi-line comments
- **Preprocessor directives**: All lines starting with `#`

## How It Works

The parser follows these steps:

1. **Read the header file** - Loads the C++ header file content
2. **Remove comments** - Strips out both single-line and multi-line comments
3. **Remove macros** - Eliminates preprocessor directives
4. **Pattern matching** - Uses regex to identify function declarations
5. **Extract names** - Captures function names from matched patterns
6. **Display results** - Shows the count and list of functions

## Customization

You can modify `cpp_header_parser.py` to:

- Add support for more complex C++ patterns
- Filter functions based on naming conventions
- Export results to JSON or other formats
- Handle function definitions (not just declarations)

### Example Modification

To export results to JSON:

```python
import json

# After parsing
results = {
    "file": header_file,
    "function_count": func_count,
    "functions": func_names
}

with open('output.json', 'w') as f:
    json.dump(results, f, indent=2)
```

## Limitations

This is a simple regex-based parser with the following limitations:

- **Not a full C++ parser**: It uses pattern matching, not AST parsing
- **Simple macros only**: Complex macro expansions are not handled
- **May miss edge cases**: Very complex or unusual C++ syntax might be missed
- **No semantic analysis**: Doesn't understand C++ semantics, only syntax patterns

For production use with complex C++ code, consider using:
- [libclang](https://github.com/llvm/llvm-project/tree/main/clang) - Official LLVM/Clang bindings
- [CppHeaderParser](https://pypi.org/project/CppHeaderParser/) - More robust Python library
- [pycparser](https://github.com/eliben/pycparser) - C parser in Python

## Testing

Test the parser with the included sample file:

```bash
python cpp_header_parser.py sample.h
```

You should see a list of 17 functions extracted from `sample.h`.

## License

This is a sample project for educational purposes. Feel free to use and modify as needed.

## Contributing

This is a sample/demonstration project. Feel free to fork and extend it for your needs!
