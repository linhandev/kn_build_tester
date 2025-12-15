# ProtectedBuffer Demo

This project demonstrates a custom memory allocator class, `ProtectedBuffer`, which uses `mmap` and `mprotect` to detect memory access violations (buffer overflow and underflow) by placing "Guard Pages" adjacent to the user's memory.

## Design

The `ProtectedBuffer` class manages a raw memory allocation using the RAII (Resource Acquisition Is Initialization) pattern. It automatically handles memory mapping upon construction and unmapping upon destruction.

### Memory Allocation Strategy

For a requested `user_size`, the allocator calculates the total memory required as follows:

1.  **Data Pages**: Calculates the number of pages needed to hold `user_size`.
    $$ \text{Data Pages} = \lceil \frac{\text{user\_size}}{\text{PAGE\_SIZE}} \rceil $$
2.  **Total Size**: Adds one extra page for the Guard Page.
    $$ \text{Total Size} = (\text{Data Pages} + 1) \times \text{PAGE\_SIZE} $$

### Protection Modes

The class supports two protection modes, specified during construction:

#### 1. Overflow Protection
Detects access beyond the end of the allocated buffer (e.g., `buffer[size]`).

*   **Layout**: `[ Data Pages ... ] [ Guard Page (PROT_NONE) ]`
*   **Placement**: The user data is **right-aligned** within the Data Pages region.
*   **Result**: The byte immediately following the user data (`user_ptr + size`) falls exactly at the beginning of the Guard Page. Accessing it triggers a `SIGSEGV` or `SIGBUS`.

#### 2. Underflow Protection
Detects access before the beginning of the allocated buffer (e.g., `buffer[-1]`).

*   **Layout**: `[ Guard Page (PROT_NONE) ] [ Data Pages ... ]`
*   **Placement**: The user data is **left-aligned** immediately following the Guard Page.
*   **Result**: The byte immediately preceding the user data (`user_ptr - 1`) falls exactly at the end of the Guard Page. Accessing it triggers a `SIGSEGV` or `SIGBUS`.

## Usage

```cpp
#include "protected_buffer_demo.cpp"

// Overflow protection
{
    ProtectedBuffer buf(64, ProtectionType::Overflow);
    void* ptr = buf.get();
    // ptr[64] will crash
}

// Underflow protection
{
    ProtectedBuffer buf(64, ProtectionType::Underflow);
    void* ptr = buf.get();
    // ptr[-1] will crash
}
```

## Testing

### macOS (Local)

You can compile and run the demo directly on macOS using `clang++`.

```bash
clang++ -g -Wall -Wextra -std=c++17 -o protected_buffer_demo protected_buffer_demo.cpp
./protected_buffer_demo
```

### OpenHarmony (OHOS)

To cross-compile and run on an OpenHarmony device, use the provided `run_ohos.sh` script. This script performs the following:

1.  **Cross-Compile**: Uses the OHOS LLVM toolchain to compile the C++ code for `aarch64-linux-ohos`.
2.  **Deploy**: Pushes the binary to `/data/local/tmp/` on the connected device using `hdc`.
3.  **Run**: Executes the binary on the device.

**Prerequisites:**
*   OHOS SDK / Toolchain configured (paths defined in `run_ohos.sh`).
*   `hdc` tool available in path.
*   Device connected.

**Command:**
```bash
./run_ohos.sh
```
