# GCOV LLVM Pass Analysis

## Pass Identification

**Pass Name:** `--insert-gcov-profiling`  
**Purpose:** Inserts GCOV profiling instrumentation into LLVM IR

## Verification

The pass can be found using:
```bash
llvm-opt --help | grep -i gcov
# Output: --insert-gcov-profiling - Insert instrumentation for GCOV profiling
```

## What the Pass Adds

When GCOV instrumentation is inserted (via `-fprofile-arcs -ftest-coverage` compiler flags), the following are added to the LLVM IR:

### 1. Counter Arrays
Global arrays that track execution counts for each basic block:
```llvm
@__llvm_gcov_ctr = internal global [6 x i64] zeroinitializer
@__llvm_gcov_ctr.2 = internal global [3 x i64] zeroinitializer
; ... more counters for each function/file
```

### 2. Counter Increments in Basic Blocks
At the start of each basic block, counter increment code is inserted:
```llvm
%11 = load i64, i64* getelementptr inbounds ([6 x i64], [6 x i64]* @__llvm_gcov_ctr, i64 0, i64 0), align 8
%12 = add i64 %11, 1
store i64 %12, i64* getelementptr inbounds ([6 x i64], [6 x i64]* @__llvm_gcov_ctr, i64 0, i64 0), align 8
```

### 3. Runtime Functions
Three functions are generated:
- `@__llvm_gcov_init()` - Initializes GCOV runtime
- `@__llvm_gcov_writeout()` - Writes coverage data to `.gcda` files
- `@__llvm_gcov_reset()` - Resets counters

### 4. Global Constructor
A global constructor ensures initialization runs at program start:
```llvm
@llvm.global_ctors = appending global [1 x { i32, void ()*, i8* }] 
    [{ i32, void ()*, i8* } { i32 0, void ()* @__llvm_gcov_init, i8* null }]
```

### 5. Type Definitions
Special types for GCOV data structures:
```llvm
%emit_function_args_ty = type { i32, i32, i32 }
%emit_arcs_args_ty = type { i32, i64* }
%file_info = type { %start_file_args_ty, i32, %emit_function_args_ty*, %emit_arcs_args_ty* }
%start_file_args_ty = type { i8*, i32, i32 }
```

## Important Notes

1. **The pass is applied during compilation**, not as a standalone `opt` pass on existing IR
2. **Compiler flags trigger it:** `-fprofile-arcs -ftest-coverage` cause clang to run this pass internally
3. **Standalone opt usage:** Running `opt -insert-gcov-profiling` on existing IR doesn't add instrumentation because:
   - The pass needs compiler-generated metadata
   - It's designed to run as part of the compilation pipeline
   - Source location information is required

## For Kotlin Native Integration

To add GCOV support to Kotlin Native's LLVM IR generation:

1. **Add the pass to your LLVM pass pipeline** when generating IR from Kotlin IR
2. **Ensure source location metadata** is preserved in the LLVM IR (for `.gcno` file generation)
3. **Link against GCOV runtime libraries** (typically `libclang_rt.profile-*.a`)
4. **The pass should run before optimization passes** to ensure accurate coverage

### Example Integration Point

In Kotlin Native's LLVM backend, you would:
```kotlin
// Pseudo-code for Kotlin Native integration
val passManager = PassManagerBuilder.createFunctionPassManager(module)
passManager.addPass(InsertGCOVProfilingPass())  // Add GCOV pass
// ... other passes
passManager.run(module)
```

The pass is typically found in LLVM's `lib/Transforms/Instrumentation/GCOVProfiling.cpp`.
