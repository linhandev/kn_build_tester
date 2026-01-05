# Minimal Weak Reference Example

This demonstrates weak linking with 2 functions:
- **strong_function()**: Always available in caller
- **weak_function()**: Weak function from callee.so (optional)

The example also demonstrates using a struct in the weak function interface when the struct is **only defined in the library** (`callee.cpp`). The caller uses `void*` in the function signature, so it **doesn't need to define the struct at all** - even in the default weak implementation!

## Files

- `callee.cpp` - Library code: defines the `Data` struct and implements weak_function(void*) as a strong symbol
- `caller.cpp` - Application code: uses `void*` in weak_function() signature, provides default weak implementation without needing to know the struct definition (doesn't include callee.cpp or have access to its header)

## Build Commands

### 1. Build callee.so (shared library) and callee.o (object file)
```bash
clang++ -std=c++17 -fPIC -shared -o callee.so callee.cpp
clang++ -std=c++17 -c -o callee.o callee.cpp
```

### 2. Build caller WITHOUT callee (uses default weak implementation)
```bash
clang++ -std=c++17 -o caller-no-lib caller.cpp
```

### 3. Build caller WITH callee linked (weak function overridden)
```bash
# Option A: Link as object file (recommended, works reliably)
clang++ -std=c++17 -o caller-with-lib caller.cpp callee.o
```

## Run Examples

### Without library linked:
```bash
./caller-no-lib
```
Output:
```
=== Caller Program ===
Strong function from caller
Calling weak function:
Weak function default implementation (callee not linked)
  (data provided but struct definition not needed)

Calling weak function with null:
Weak function default implementation (callee not linked)
  (null data)
```

### With library linked (object file):
```bash
./caller-with-lib
```
Output:
```
=== Caller Program ===
Strong function from caller
Calling weak function:
Weak function actual implementation from callee
  Data value: 42
  Data message: Hello from caller

Calling weak function with null:
Weak function actual implementation from callee (null data)
```

### With library linked (shared library):
```bash
./caller-with-lib-so
```
Output (may vary by platform):
```
=== Caller Program ===
Strong function from caller
Calling weak function:
Weak function default (callee.so not linked)
```
Note: On macOS, linking .so files directly may not override weak symbols. Use object files (.o) for reliable weak symbol overriding.

### Runtime loading (alternative):
You can also load callee.so at runtime using dlopen, but the weak linking approach is simpler.

## Key Points

- `caller.cpp` does NOT include `callee.cpp` - it provides a default weak implementation
- Default weak implementation is used when callee is not linked
- When callee.o is linked, its strong implementation overrides the weak default
- Strong function is always available regardless of library linking
- Weak symbols allow optional library linking without undefined symbol errors
- On macOS, use object files (.o) for reliable weak symbol overriding; .so files may not work as expected

## Struct in Interface (Using void*)

The example demonstrates using a struct (`Data`) in the weak function interface when you **don't have access to the library's header**:
- The struct is **only defined in `callee.cpp`** (the library code)
- `caller.cpp` **does NOT include any header from callee** - it doesn't have access to it
- **The function signature uses `void*`** instead of `Data*`, so the caller doesn't need to know the struct definition at all!
- The default weak implementation in `caller.cpp` can work with `void*` without needing to define or know about the `Data` struct
- When `callee.o` is linked, its strong implementation casts `void*` to `Data*` internally
- This mimics a real scenario: a library adds an API that uses a struct, but you don't have the `.h` file - you can use `void*` to avoid needing the struct definition

**Key insight**: Using `void*` in the function signature allows the caller to provide a default weak implementation without needing to know or define the struct type. The caller can pass any pointer (or nullptr) without needing the struct definition.



```shell
Building callee.o...
Building caller-no-lib...
Building caller-with-lib...
Build complete!
Running caller-no-lib...
=== Caller Program ===
Strong function from caller
Calling weak function, data from create_data :
Weak function default implementation (callee not linked)
  (null data)
Calling weak function, data from constructor :
Weak function default implementation (callee not linked)
  (null data)
Running caller-with-lib...
=== Caller Program ===
Strong function from caller
Calling weak function, data from create_data :
Weak function actual implementation from callee
  Data value: 2
  Data message: default message
Calling weak function, data from constructor :
Weak function actual implementation from callee (null data)
```