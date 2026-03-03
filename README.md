# Sysroot Grammar Scanner

Scans OpenHarmony and HMS sysroot headers (those annotated with `@addtogroup`)
for C/C++ grammar patterns relevant to Kotlin Native cinterop testing.

Produces a searchable JSON database where every declaration carries grammar tags
and verbatim source text, plus an AST coverage report on stderr.

## Prerequisites

- Python 3.10+
- DevEco Studio installed at `/Applications/DevEco-Studio.app/`
- `libclang` Python bindings (`pip install libclang`)

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install libclang
```

## Usage

Full scan (both sysroots, all `@addtogroup` headers):

```bash
python3 scan_sysroot_grammar.py > sysroot_grammar_report.json 2> coverage.log
```

Single header:

```bash
python3 scan_sysroot_grammar.py --single database/rdb/relational_store.h > single.json 2> coverage.log
```

Override libclang path:

```bash
python3 scan_sysroot_grammar.py --library-path /path/to/llvm/lib > report.json 2> coverage.log
```

## Project Structure

```
scan_sysroot_grammar.py   # CLI entry point
scanner/
  config.py               # SDK paths, constants
  helpers.py              # FileCache, path utilities
  coverage.py             # AST coverage tracker
  classify.py             # Type classification (TypeKind -> tags)
  scanners.py             # Declaration scanners (function, struct, enum, etc.)
  walker.py               # AST walker
  parse.py                # Header discovery, umbrella generation, TU parsing
```

## Output

The JSON report contains:

- `entries[]` -- one object per declaration, with `kind`, `name`, `tags[]`, `source`, `details`
- `coverage` -- AST coverage report (cursor kinds, type kinds, attribute kinds)
- `stats` -- tag frequency counts

## Grammar Tag Reference

Every declaration in the output carries a `tags` array describing what C/C++
grammar constructs it exercises. These are the tags the scanner can produce,
grouped by category.

### Function

| Tag | Description | Example |
|-----|-------------|---------|
| `void_return` | Returns `void` | `void OH_Drawing_CanvasSave(OH_Drawing_Canvas* canvas)` |
| `ptr_return` | Returns a pointer type | `OH_Drawing_Canvas* OH_Drawing_CanvasCreate(void)` |
| `struct_return_by_value` | Returns a struct/union by value | `struct Point2D get_position(void)` |
| `struct_return_small` | Returns struct by value, size <= 16 bytes (register-passed on AArch64) | `OH_Drawing_Point2D make_point(float x, float y)` |
| `struct_return_large` | Returns struct by value, size > 16 bytes (indirect return) | `struct LargeStruct get_large_data(void)` |
| `variadic` | Variadic function (`...`) | `int printf(const char* format, ...)` |
| `static_inline_with_body` | `static inline` function with body in header | `static inline int min(int a, int b) { return a < b ? a : b; }` |
| `inline_non_static` | Non-static `inline` function | `inline void swap(int* a, int* b)` |
| `has_unnamed_param` | Has at least one unnamed parameter | `void func(int, float unnamed_param)` |
| `has_attributes` | Has one or more compiler attributes | `void func(void) __attribute__((availability(ohos, introduced=8.0.0)))` |

### Function Return Type (prefixed `ret_`)

These describe the return type in detail. Applied as `ret_<type_tag>`.

| Tag | Description | Example |
|-----|-------------|---------|
| `ret_primitive_int` | Returns `int` | `int get_count(void)` |
| `ret_primitive_uint` | Returns `unsigned int` | `unsigned int get_flags(void)` |
| `ret_primitive_long` | Returns `long` | `long get_offset(void)` |
| `ret_primitive_ulong` | Returns `unsigned long` | `unsigned long get_size(void)` |
| `ret_primitive_longlong` | Returns `long long` | `long long get_timestamp(void)` |
| `ret_primitive_ulonglong` | Returns `unsigned long long` | `unsigned long long get_uid(void)` |
| `ret_primitive_short` | Returns `short` | `short get_port(void)` |
| `ret_primitive_ushort` | Returns `unsigned short` | `unsigned short get_version(void)` |
| `ret_primitive_char` | Returns `char` | `char get_char(void)` |
| `ret_primitive_uchar` | Returns `unsigned char` | `unsigned char get_byte(void)` |
| `ret_primitive_schar` | Returns `signed char` | `signed char get_signed_byte(void)` |
| `ret_primitive_bool` | Returns `bool` / `_Bool` | `bool OH_Notification_IsNotificationEnabled(void)` |
| `ret_primitive_float` | Returns `float` | `float get_ratio(void)` |
| `ret_primitive_double` | Returns `double` | `double get_precision(void)` |
| `ret_primitive_longdouble` | Returns `long double` | `long double get_high_precision(void)` |
| `ret_primitive_wchar` | Returns `wchar_t` | `wchar_t get_wide_char(void)` |
| `ret_enum` | Returns an enum type | `OH_Drawing_ErrorCode OH_Drawing_CanvasDrawPixelMapNine(...)` |
| `ret_typedef_ref` | Returns a typedef'd type | `uint32_t OH_Drawing_CanvasGetSaveCount(OH_Drawing_Canvas* canvas)` |
| `ret_struct_ptr` | Returns pointer to defined struct | `struct tm* localtime(const time_t* timer)` |
| `ret_opaque_ptr` | Returns pointer to forward-declared struct | `OH_Drawing_Canvas* OH_Drawing_CanvasCreate(void)` |
| `ret_void_ptr` | Returns `void *` | `void* malloc(size_t size)` |
| `ret_double_ptr` | Returns pointer-to-pointer | `char** environ` |
| `ret_ptr` | Returns generic pointer | `const void* get_data(void)` |
| `ret_ptr_to_const` | Returns pointer to `const` | `const char* getenv(const char* name)` |
| `ret_function_pointer` | Returns a function pointer | `int (*get_comparator(void))(const void*, const void*)` |

### Function Parameter Types (prefixed `param_`)

These describe parameter types. Applied as `param_<type_tag>`.

| Tag | Description | Example |
|-----|-------------|---------|
| `param_primitive_int` | Parameter of type `int` | `void set_count(int count)` |
| `param_primitive_uint` | Parameter of type `unsigned int` | `void set_flags(unsigned int flags)` |
| `param_primitive_long` | Parameter of type `long` | `void set_offset(long offset)` |
| `param_primitive_ulong` | Parameter of type `unsigned long` | `void set_size(unsigned long size)` |
| `param_primitive_longlong` | Parameter of type `long long` | `void set_timestamp(long long timestamp)` |
| `param_primitive_short` | Parameter of type `short` | `void set_port(short port)` |
| `param_primitive_ushort` | Parameter of type `unsigned short` | `void set_version(unsigned short version)` |
| `param_primitive_char` | Parameter of type `char` | `void set_char(char c)` |
| `param_primitive_uchar` | Parameter of type `unsigned char` | `void set_byte(unsigned char b)` |
| `param_primitive_schar` | Parameter of type `signed char` | `void set_signed_byte(signed char b)` |
| `param_primitive_bool` | Parameter of type `bool` / `_Bool` | `void enable_feature(bool enabled)` |
| `param_primitive_float` | Parameter of type `float` | `void set_ratio(float ratio)` |
| `param_primitive_double` | Parameter of type `double` | `void set_precision(double precision)` |
| `param_primitive_longdouble` | Parameter of type `long double` | `void set_high_precision(long double precision)` |
| `param_primitive_wchar` | Parameter of type `wchar_t` | `void set_wide_char(wchar_t c)` |
| `param_enum` | Parameter of enum type | `void OH_Drawing_CanvasDrawColor(..., OH_Drawing_BlendMode blendMode)` |
| `param_typedef_ref` | Parameter of typedef'd type | `void OH_Drawing_CanvasRestoreToCount(..., uint32_t saveCount)` |
| `param_struct_ptr` | Pointer to defined struct | `void localtime_r(const time_t* timer, struct tm* result)` |
| `param_opaque_ptr` | Pointer to forward-declared (opaque) struct | `void OH_Drawing_CanvasDestroy(OH_Drawing_Canvas* canvas)` |
| `param_void_ptr` | `void *` parameter | `void* memcpy(void* dest, const void* src, size_t n)` |
| `param_double_ptr` | Pointer-to-pointer parameter | `int main(int argc, char** argv)` |
| `param_ptr` | Generic pointer parameter | `void process_data(const void* data)` |
| `param_ptr_to_const` | Pointer to `const` data | `void OH_Drawing_CanvasAttachPen(..., const OH_Drawing_Pen* pen)` |
| `param_const_ptr` | `const` pointer (`T *const`) | `void func(int* const ptr)` |
| `param_const_qualified` | `const`-qualified parameter | `void func(const int value)` |
| `param_char_ptr` | `char *` parameter | `char* strcpy(char* dest, const char* src)` |
| `param_const_char_ptr` | `const char *` parameter | `size_t strlen(const char* str)` |
| `param_function_pointer` | Function pointer parameter | `void qsort(void* base, size_t n, size_t size, int (*compar)(const void*, const void*))` |
| `param_function_proto` | Function prototype parameter | `void register_callback(void (*callback)(int))` |
| `param_incomplete_array` | Incomplete array parameter (`T[]`) | `OH_NN_ReturnCode HMS_HiAISingleOpExecutor_Execute(..., HiAI_SingleOpTensor* input[])` |
| `param_fixed_array` | Fixed-size array parameter | `void process_array(int arr[10])` |
| `param_record` | Struct/union parameter by value | `void print_point(struct Point p)` |
| `param_struct_by_value` | Struct passed by value | `void draw_rect(struct Rect rect)` |
| `param_struct_by_value_small` | Struct by value, size <= 16 bytes | `void OH_Drawing_CanvasDrawShadow(..., OH_Drawing_Point3D planeParams)` |
| `param_struct_by_value_large` | Struct by value, size > 16 bytes | `void HMS_HiAISingleOpDescriptor_CreateConvolution(HiAISingleOpDescriptor_ConvolutionParam param)` |
| `param_type_LVALUEREFERENCE` | C++ lvalue reference parameter (`T&`) | `void swap(int& a, int& b)` |

### Struct / Union

| Tag | Description | Example |
|-----|-------------|---------|
| `struct` | Struct declaration | `struct OH_NativeBuffer_Config { int32_t width; int32_t height; ... }` |
| `union` | Union declaration | `union Data { int i; float f; char* s; }` |
| `opaque` | Forward declaration only (no definition) | `struct OH_NativeBuffer; typedef struct OH_NativeBuffer OH_NativeBuffer` |
| `anonymous` | Anonymous struct/union | `struct { int x, y; } point` |
| `size_small` | Size <= 16 bytes | `struct Point { int x; int y; }` |
| `size_medium` | Size 17-64 bytes | `struct Medium { char data[32]; int flags; }` |
| `size_large` | Size > 64 bytes | `struct Large { char buffer[128]; double values[8]; }` |
| `has_bitfield` | Contains bitfield member(s) | `struct Flags { unsigned int enabled : 1; unsigned int visible : 1; }` |
| `has_fixed_array_field` | Contains fixed-size array member | `struct OH_NativeBuffer_Planes { uint32_t planeCount; OH_NativeBuffer_Plane planes[4]; }` |
| `has_multidim_array_field` | Contains multi-dimensional array member | `struct Matrix { int data[3][3]; }` |
| `has_flexible_array_member` | Trailing incomplete array (`T field[]`) | `struct FlexArray { int count; char data[]; }` |
| `has_function_pointer_field` | Contains function pointer member | `struct VTable { void (*draw)(void*); void (*destroy)(void*); }` |
| `has_struct_by_value_field` | Contains nested struct by value | `struct Container { struct Point origin; struct Size dimensions; }` |
| `has_union_field` | Contains union member | `struct Variant { int type; union { int i; float f; } value; }` |
| `has_nested_struct` | Contains nested struct definition | `struct Outer { struct Inner { int x; } inner; }` |
| `has_nested_union` | Contains nested union definition | `struct Complex { union { int i; float f; } u; }` |
| `has_anonymous_member` | Contains anonymous struct/union member | `struct Anon { struct { int x, y; }; int z; }` |
| `vtable_like` | All fields are function pointers | `struct VTable { void* (*alloc)(size_t); void (*free)(void*); }` |
| `self_referential` | Struct has pointer to itself | `struct Node { int data; struct Node* next; }` |
| `pragma_packed` | Uses `#pragma pack` | `#pragma pack(1) struct Packed { char c; int i; }` |

### Enum

| Tag | Description | Example |
|-----|-------------|---------|
| `named_enum` | Has an explicit name | `enum OH_Drawing_CanvasClipOp { DIFFERENCE, INTERSECT }` |
| `anonymous_enum` | Anonymous enum | `enum { RED, GREEN, BLUE } color` |
| `has_negative_values` | Contains negative enumerator values | `enum ArkWeb_NetError { ARKWEB_NET_OK = 0, ARKWEB_ERR_IO_PENDING = -1, ... }` |
| `has_explicit_values` | Has explicit `= value` assignments | `enum BundleManager_ErrorCode { ..._NO_ERROR = 0, ..._PERMISSION_DENIED = 201 }` |
| `large_range` | Range (max-min) exceeds 2^31 | `enum LargeRange { MIN = -2000000000LL, MAX = 2000000000LL }` |

The enumerator count is stored as a numeric field `details.value_count` in the JSON output
rather than as a tag, since the count is a continuous value (ranges from 0 to 1235 in this
sysroot) and would generate excessive unique tags.

### Typedef

| Tag | Description | Example |
|-----|-------------|---------|
| `to_struct` | Aliases a defined struct | `typedef struct Point { int x, y; } Point` |
| `to_union` | Aliases a union | `typedef union Data { int i; float f; } Data` |
| `to_enum` | Aliases an enum | `typedef enum OH_Drawing_CanvasClipOp OH_Drawing_CanvasClipOp` |
| `to_opaque` | Aliases a forward-declared struct | `typedef struct OH_NativeBuffer OH_NativeBuffer` |
| `to_function_pointer` | Aliases a function pointer type | `typedef void (*Callback)(int)` |
| `to_pointer` | Aliases a non-function pointer | `typedef char* String` |
| `to_primitive` | Aliases a primitive type | `typedef unsigned int uint32_t` |
| `to_array` | Aliases an array type | `typedef int Vector[3]` |
| `to_void` | Aliases `void` | `typedef void Void` |
| `chain` | Typedef of another typedef | `typedef uint32_t MyInt` |
| `type_alias` | C++ `using` alias (not C `typedef`) | `using String = std::string` |

### Variable

| Tag | Description | Example |
|-----|-------------|---------|
| `extern_var` | `extern` linkage | `extern int global_counter` |
| `static_var` | `static` storage | `static const char* version = "1.0"` |
| `const_var` | `const`-qualified | `const int MAX_SIZE = 100` |
| `volatile_var` | `volatile`-qualified | `volatile int shared_flag` |
| `thread_local_var` | `_Thread_local` / `thread_local` | `_Thread_local int thread_id` |

### Macro

| Tag | Description | Example |
|-----|-------------|---------|
| `object_like_macro` | Object-like macro (`#define FOO val`) | `#define MAX_BUFFER_SIZE 1024` |
| `function_like_macro` | Function-like macro (`#define FOO(x) ...`) | `#define MIN(a, b) ((a) < (b) ? (a) : (b))` |
| `empty_macro` | Macro with no body / guard macro | `#ifndef HEADER_H` (generates `empty_macro`) |
| `macro_uses_builtin` | Body references `__builtin_*` | `#define likely(x) __builtin_expect(!!(x), 1)` |
| `macro_uses_typeof` | Body references `__typeof` | `#define SWAP(a, b) do { __typeof(a) tmp = a; a = b; b = tmp; } while(0)` |
| `macro_uses_extension` | Body references `__extension__` | `#define EXTENDED_FEATURE __extension__ ({ int x = 1; x; })` |

### Type Qualifiers and Modifiers

These appear standalone or as prefixes in `param_` / `ret_` tags.

| Tag | Description | Example |
|-----|-------------|---------|
| `const_qualified` | `const`-qualified type | `const int value` |
| `volatile_qualified` | `volatile`-qualified type | `volatile int* ptr` |
| `ptr_to_const` | Pointer to `const` | `const char* str` |
| `const_ptr` | `const` pointer itself | `char* const ptr` |
| `ptr_to_volatile` | Pointer to `volatile` | `volatile char* buffer` |

### Pointer Types

| Tag | Description | Example |
|-----|-------------|---------|
| `void_ptr` | `void *` | `void* malloc(size_t size)` |
| `char_ptr` | `char *` | `char* strcpy(char* dest, const char* src)` |
| `const_char_ptr` | `const char *` | `const char* getenv(const char* name)` |
| `double_ptr` | Pointer to pointer | `char** environ` |
| `struct_ptr` | Pointer to defined struct | `struct tm* localtime(const time_t* timer)` |
| `opaque_ptr` | Pointer to forward-declared struct | `OH_Drawing_Canvas* OH_Drawing_CanvasCreate(void)` |
| `function_pointer` | Pointer to function | `int (*strcmp)(const char*, const char*)` |
| `ptr` | Other pointer type | `const int* get_array(void)` |

### Array Types

| Tag | Description | Example |
|-----|-------------|---------|
| `fixed_array` | Fixed-size array (`T[N]`) | `int buffer[1024]` |
| `multidim_array` | Multi-dimensional array (`T[N][M]`) | `int matrix[3][3]` |
| `incomplete_array` | Incomplete array (`T[]`) | `char data[]` (in struct, flexible array member) |
| `variable_length_array` | Variable-length array | `void func(int n) { int arr[n]; }` |

### Record Types

| Tag | Description | Example |
|-----|-------------|---------|
| `record` | Struct/union with definition | `struct Point { int x, y; }` |
| `opaque_record` | Struct/union without definition | `struct OpaqueStruct` |

### Other Types

| Tag | Description | Example |
|-----|-------------|---------|
| `enum` | Enum type reference | `enum OH_Drawing_CanvasClipOp clipOp` |
| `typedef_ref` | Typedef type reference | `uint32_t flags` |
| `function_proto` | Function prototype type | `void (*callback)(int, char*)` |
| `function_noproto` | K&R-style function (no prototype) | `int old_func()` (K&R C style) |
| `complex` | `_Complex` type | `_Complex double c` |
| `unexposed_type` | Type not directly exposed by libclang | `typeof(int) t` |

### Primitive Types

| Tag | Description | Example |
|-----|-------------|---------|
| `void` | `void` | `void func(void)` |
| `primitive_bool` | `bool` / `_Bool` | `bool enabled` |
| `primitive_char` | `char` (signed) | `char letter` |
| `primitive_uchar` | `unsigned char` | `unsigned char byte` |
| `primitive_schar` | `signed char` | `signed char value` |
| `primitive_char16` | `char16_t` | `char16_t utf16_char` |
| `primitive_char32` | `char32_t` | `char32_t utf32_char` |
| `primitive_wchar` | `wchar_t` | `wchar_t wide_char` |
| `primitive_short` | `short` | `short small_num` |
| `primitive_ushort` | `unsigned short` | `unsigned short small_pos` |
| `primitive_int` | `int` | `int count` |
| `primitive_uint` | `unsigned int` | `unsigned int flags` |
| `primitive_long` | `long` | `long offset` |
| `primitive_ulong` | `unsigned long` | `unsigned long size` |
| `primitive_longlong` | `long long` | `long long timestamp` |
| `primitive_ulonglong` | `unsigned long long` | `unsigned long long uid` |
| `primitive_float` | `float` | `float ratio` |
| `primitive_double` | `double` | `double precision` |
| `primitive_longdouble` | `long double` | `long double high_precision` |

### Attributes

| Tag | Description | Example |
|-----|-------------|---------|
| `attr_availability` | `__attribute__((availability(...)))` -- API version gating | `__attribute__((availability(ohos, introduced=8.0.0)))` |
| `attr_visibility` | `__attribute__((visibility("default")))` -- symbol visibility | `__attribute__((visibility("hidden")))` |
| `attr_deprecated` | `__attribute__((deprecated))` | `__attribute__((deprecated("Use new_func instead")))` |
| `attr_noreturn` | `__attribute__((noreturn))` / `_Noreturn` | `_Noreturn void exit(int status)` |
| `attr_constructor` | `__attribute__((constructor))` | `__attribute__((constructor)) void init(void)` |
| `attr_format` | `__attribute__((format(...)))` -- printf/scanf format checking | `int printf(const char* format, ...) __attribute__((format(printf, 1, 2)))` |
| `attr_packed` | `__attribute__((packed))` | `struct __attribute__((packed)) Data { char c; int i; }` |
| `attr_aligned` | `__attribute__((aligned(N)))` | `struct __attribute__((aligned(16))) AlignedStruct { int x; }` |
| `attr_const` | `__attribute__((const))` | `__attribute__((const)) int square(int x)` |
| `attr_asm_label` | `__asm__("label")` asm label attribute | `void func(void) __asm__("my_func")` |
| `attr_UNEXPOSED_ATTR` | Attribute not fully resolved by libclang | `void func(void) __attribute__((custom_attr))` |

## AST Coverage Report

When the scanner traverses the Clang AST, every node is classified into one of four
dispositions: **handled** (produces output), **child-handled** (processed by a parent
handler), **attribute** (mapped to `attr_*` tags), or **skipped** (intentionally ignored).
The coverage report on stderr proves no node type falls through unhandled.

### CursorKind Dispositions

#### Handled -- produce scanner output

| CursorKind | Handler | What it produces |
|---|---|---|
| `TRANSLATION_UNIT` | `walk()` root | Entry point; recurses into children. No output entry itself. |
| `LINKAGE_SPEC` | `walk()` passthrough | `extern "C" { ... }` block. Transparent wrapper -- recurses into children without emitting an entry. |
| `NAMESPACE` | `walk()` passthrough | C++ `namespace N { ... }`. Same as `LINKAGE_SPEC`: transparent, recurses into children. |
| `FUNCTION_DECL` | `scan_function()` | Produces a `"kind": "function"` entry with return type tags, parameter tags, and attribute tags. |
| `STRUCT_DECL` | `scan_record()` | Produces a `"kind": "struct"` entry with size class, field analysis, and structural tags (bitfield, vtable-like, self-referential, etc.). |
| `UNION_DECL` | `scan_record()` | Same as `STRUCT_DECL` but tagged `"kind": "union"`. |
| `ENUM_DECL` | `scan_enum()` | Produces a `"kind": "enum"` entry with value list, explicit/negative value flags, and range analysis. |
| `TYPEDEF_DECL` | `scan_typedef()` | Produces a `"kind": "typedef"` entry classifying what the typedef aliases (struct, enum, function pointer, primitive, etc.). |
| `TYPE_ALIAS_DECL` | `scan_typedef()` | C++11 `using X = T` -- handled identically to typedef but additionally tagged `type_alias`. |
| `VAR_DECL` | `scan_var()` | Produces a `"kind": "variable"` entry with storage class and qualifier tags. |
| `MACRO_DEFINITION` | `scan_macro()` | Produces a `"kind": "macro"` entry classifying as object-like, function-like, or empty, plus builtin/typeof/extension usage. |

#### Child-handled -- processed inside a parent's handler

These are never visited at the top level of the walker. Instead, each parent handler
iterates over its children and processes these kinds directly.

| CursorKind | Parent handler | Why handled as child |
|---|---|---|
| `PARM_DECL` | `scan_function()` | Function parameters. Each parameter's type is classified and tagged as `param_*`. The parameter is meaningless outside its parent function -- it has no standalone declaration. |
| `FIELD_DECL` | `scan_record()` | Struct/union fields. Each field's type, array dimensions, and bitfield width are analyzed. Fields only exist within a record declaration and cannot be scanned independently. |
| `ENUM_CONSTANT_DECL` | `scan_enum()` | Enumerator values (`RED = 0`). Each constant's name and integer value are collected. They are children of the enum and have no meaning outside it. |

#### Attribute nodes -- mapped to `attr_*` tags

These are child nodes of declarations that represent compiler attributes. They are detected by `cursor.kind.is_attribute()` and mapped to semantic `attr_*` tags.

| CursorKind | Mapped to | What it represents |
|---|---|---|
| `UNEXPOSED_ATTR` | `attr_availability`, `attr_deprecated`, etc. | Catch-all for attributes libclang doesn't fully model. The scanner reads the source text to classify the actual attribute (availability, deprecated, format, etc.). Most `__attribute__((__availability__(ohos, ...)))` annotations land here because libclang exposes them as unexposed. |
| `VISIBILITY_ATTR` | `attr_visibility` | `__attribute__((visibility("default")))` or `__attribute__((visibility("hidden")))`. Controls symbol export from shared libraries. |
| `PACKED_ATTR` | `attr_packed` | `__attribute__((packed))` on a struct. Eliminates padding between fields. |
| `ALIGNED_ATTR` | `attr_aligned` | `__attribute__((aligned(N)))`. Forces alignment to N bytes. |
| `ASM_LABEL_ATTR` | `attr_asm_label` | `__asm__("symbol_name")`. Overrides the linker symbol name for a declaration. |
| `CONST_ATTR` | `attr_const` | `__attribute__((const))`. Tells the compiler the function has no side effects and depends only on its arguments (stronger than `pure`). |

#### Skipped -- intentionally ignored

These AST node kinds carry no grammar information relevant to cinterop and are
skipped without producing output.

| CursorKind | Count | Why skipped |
|---|---|---|
| `MACRO_INSTANTIATION` | ~6600 | Records where a `#define` macro is *used* (expanded) in source code. The scanner only cares about macro *definitions* (`MACRO_DEFINITION`), not their expansion sites. Macro instantiation sites carry no type or signature information -- they are just textual expansion points tracked by the preprocessor. Scanning these would produce thousands of duplicate entries with no grammar value. |
| `TYPE_REF` | ~5000 | A reference to a previously declared type, e.g. when `OH_Drawing_Canvas` appears as a parameter type, libclang creates a `TYPE_REF` child pointing back to the struct declaration. These are internal cross-reference links in the AST -- the actual type information is already fully captured when we classify the parameter's type via `classify_type()`. Processing them would double-count every type mention. |
| `INCLUSION_DIRECTIVE` | ~1300 | `#include "foo.h"` directives. These are preprocessor bookkeeping that tells libclang which file was included. The scanner already processes all declarations from included files via the umbrella header -- the include directives themselves carry no declaration or type grammar. |
| `UNEXPOSED_DECL` | ~78 | Declarations that libclang cannot fully represent in its public API. In this sysroot these are typically `extern "C"` blocks that Clang partially models as `UNEXPOSED_DECL` instead of `LINKAGE_SPEC` (a known libclang quirk). The actual declarations *inside* these blocks are still visited as children, so no content is lost. |

### TypeKind and AttributeKind

TypeKinds (28 encountered) and AttributeKinds (6 encountered) are not skipped -- they
are all processed by `classify_type()` and the attribute-handling logic respectively.
Every TypeKind maps to one or more grammar tags via the `PRIMITIVE_MAP` and pointer/array/record
classification logic in `scanner/classify.py`. Every AttributeKind maps to an `attr_*` tag
as shown in the table above.
