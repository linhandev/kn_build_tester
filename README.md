# Sysroot Function Group Scanner

Scans OpenHarmony and HMS sysroot headers annotated with `@addtogroup` and produces a function-centered JSON report for Kotlin/Native `cinterop` test planning.

The report is organized around:

- function-level flags
- normalized return groups
- normalized per-parameter groups

## Prerequisites

- Python 3.10+
- DevEco Studio installed at `/Applications/DevEco-Studio.app/`
- `libclang` Python bindings: `pip install libclang`

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install libclang
```

## Usage

Full scan:

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

## Output

The JSON report contains:

- `functions[]` -- one object per function declaration
- `coverage` -- AST coverage report
- `stats.total_functions` -- total number of function entries
- `stats.function_groups` -- precomputed grouping counts

`stats.function_groups` contains:

- `function_x_return`
- `function_x_param`
- `full_signature`
- `return_shapes`
- `param_shapes`

## How Scanning Works

### Step 1: Select the API surface

Only headers containing `@addtogroup` are treated as public API. In a full run, the scanner discovers those headers under the OpenHarmony and HMS sysroots.

### Step 2: Build the parse input

For a full run, the scanner generates an umbrella header that includes all API headers. For `--single`, it parses only the requested header.

Example umbrella fragment:

```c
#include "/path/to/database/rdb/relational_store.h"
#include "/path/to/AppGalleryKit/module_install.h"
```

### Step 3: Parse with libclang

The umbrella or single header is parsed with the OpenHarmony target/sysroot flags. Diagnostics are written to stderr.

### Step 4: Walk the AST

The walker still visits all top-level declarations for coverage accounting, but the JSON payload keeps only `FUNCTION_DECL` entries. For `--single`, the output is filtered back down to declarations whose source location is the requested header itself, so transitive includes do not pollute the grouping matrix.

### Step 5: Normalize each function

Each function is split into:

1. `function_group_key`
2. `return`
3. `parameters[]`

The return and each parameter have:

- one `base_tag`
- zero or more `modifier_tags`
- one human-readable `key`

### Step 6: Emit matrix-ready keys

Each function entry contains:

- `matrix_keys.function_x_return`
- `matrix_keys.function_x_params[]`
- `matrix_keys.full_signature`

These are the main fields to use for test-matrix construction.

## Detailed Example

For this real declaration from `database/preferences/oh_preferences.h`:

```c
int OH_Preferences_RegisterDataObserver(OH_Preferences *preference, void *context,
    OH_PreferencesDataObserver observer, const char *keys[], uint32_t keyCount)
```

### Where Each Part Comes From

Function group key:

- function AST node: `FUNCTION_DECL`
- source does not start with `static inline` or `inline`
- `cursor.type.is_function_variadic()` is false (no `...`)
- all `PARM_DECL` nodes have non-empty spelling
- result: no flags apply, so `function_group_key=plain_function`

Return `int`:

- AST shape: `TypeKind.INT`
- `PRIMITIVE_MAP` maps it directly to `primitive_int`
- result: `base_tag=primitive_int`, `modifier_tags=[]`

Parameter `OH_Preferences *preference`:

- AST shape: top-level `POINTER`
- pointee canonical kind: `RECORD` whose declaration has no definition (opaque)
- `_pointer_base()` maps that to `opaque_ptr`
- the pointee is behind a typedef (`OH_Preferences`), so `typedef_ptr` is added
- result: `base_tag=opaque_ptr`, `modifier_tags=[typedef_ptr]`

Parameter `void *context`:

- AST shape: top-level `POINTER`
- pointee canonical kind: `VOID`
- `_pointer_base()` maps that to `void_ptr`
- result: `base_tag=void_ptr`, `modifier_tags=[]`

Parameter `OH_PreferencesDataObserver observer`:

- AST shape: `TYPEDEF` whose canonical type is `POINTER` to `FUNCTIONPROTO`
- `_pointer_base()` maps that to `function_pointer`
- the type is spelled as a typedef, so `typedef_ref` is added
- result: `base_tag=function_pointer`, `modifier_tags=[typedef_ref]`

Parameter `const char *keys[]`:

- AST shape: `INCOMPLETEARRAY`
- recursive element classification sees `POINTER` to const-qualified `CHAR_S`
- `_pointer_base()` maps the element to `const_char_ptr`
- array base becomes `incomplete_array`
- recursive element analysis adds `incomplete_array_of_const_char_ptr`
- result: `base_tag=incomplete_array`, `modifier_tags=[incomplete_array_of_const_char_ptr]`

Parameter `uint32_t keyCount`:

- AST shape: `TYPEDEF` whose canonical type is `UINT`
- `PRIMITIVE_MAP` maps the canonical type to `primitive_uint`
- the type is spelled as a typedef, so `typedef_ref` is added
- result: `base_tag=primitive_uint`, `modifier_tags=[typedef_ref]`

## Group Reference

### Function Group Key

`function_group_key` is formed from the sorted subset of function flags below. If none apply, the key is `plain_function`.

Exhaustive function flags currently emitted:

| Flag | Trigger | Example |
|---|---|---|
| `void_return` | Canonical result type is `VOID` | `void destroy(Foo *p)` |
| `ptr_return` | Canonical result type is `POINTER` | `Foo *create(void)` |
| `struct_return_by_value` | Canonical result type is `RECORD` whose declaration is a struct | `struct Point get_point(void)` |
| `struct_return_small` | Same as above and size `<= 16` | `struct Point get_point(void)` |
| `struct_return_large` | Same as above and size `> 16` | `struct Big get_big(void)` |
| `union_return_by_value` | Canonical result type is `RECORD` whose declaration is a union | `union Value get_value(void)` |
| `union_return_small` | Same as above and size `<= 16` | `union Value get_value(void)` |
| `union_return_large` | Same as above and size `> 16` | `union BigValue get_value(void)` |
| `variadic` | `cursor.type.is_function_variadic()` is true | `int printf(const char *fmt, ...)` |
| `static_inline_with_body` | Source starts with `static inline` or `static __inline` | `static inline int min(int a, int b) { ... }` |
| `inline_non_static` | Source starts with `inline ` | `inline void swap(int *a, int *b)` |
| `has_unnamed_param` | At least one `PARM_DECL` has empty spelling | `int strcmp(const char *, const char *)` |

Combinations are produced by joining the applicable flags with ` + `, for example `has_unnamed_param + ptr_return`.

### Return / Parameter Base Tags

Base tags come from normalized type classification. The table below is exhaustive for the current implementation.

#### Primitive scalar bases

| Base tag | AST shape | Example |
|---|---|---|
| `void` | `TypeKind.VOID` | `void f(void)` |
| `primitive_bool` | `TypeKind.BOOL` | `bool enabled` |
| `primitive_char` | `TypeKind.CHAR_S` | `char c` |
| `primitive_schar` | `TypeKind.SCHAR` | `signed char c` |
| `primitive_uchar` | `TypeKind.CHAR_U` or `UCHAR` | `unsigned char c` |
| `primitive_short` | `TypeKind.SHORT` | `short n` |
| `primitive_ushort` | `TypeKind.USHORT` | `unsigned short n` |
| `primitive_int` | `TypeKind.INT` | `int n` |
| `primitive_uint` | `TypeKind.UINT` | `unsigned int n` |
| `primitive_long` | `TypeKind.LONG` | `long n` |
| `primitive_ulong` | `TypeKind.ULONG` | `unsigned long n` |
| `primitive_longlong` | `TypeKind.LONGLONG` | `long long n` |
| `primitive_ulonglong` | `TypeKind.ULONGLONG` | `unsigned long long n` |
| `primitive_float` | `TypeKind.FLOAT` | `float x` |
| `primitive_double` | `TypeKind.DOUBLE` | `double x` |
| `primitive_longdouble` | `TypeKind.LONGDOUBLE` | `long double x` |
| `primitive_wchar` | `TypeKind.WCHAR` | `wchar_t wc` |
| `primitive_char16` | `TypeKind.CHAR16` | `char16_t c16` |
| `primitive_char32` | `TypeKind.CHAR32` | `char32_t c32` |
| `complex` | `TypeKind.COMPLEX` | `_Complex double z` |

#### Pointer bases

| Base tag | AST shape | Example |
|---|---|---|
| `void_ptr` | `POINTER` to canonical `VOID` | `void *p` |
| `char_ptr` | `POINTER` to non-const canonical char kind | `char *s` |
| `const_char_ptr` | `POINTER` to const canonical char kind | `const char *s` |
| `double_ptr` | `POINTER` to canonical `POINTER` | `Foo **p` |
| `function_pointer` | `POINTER` to canonical `FUNCTIONPROTO` | `int (*cmp)(const void *, const void *)` |
| `struct_ptr` | `POINTER` to defined canonical record | `struct tm *tmv` |
| `opaque_ptr` | `POINTER` to forward-declared canonical record | `Foo *handle` |
| `enum_ptr` | `POINTER` to canonical `ENUM` | `enum State *state` |
| `primitive_bool_ptr` | `POINTER` to canonical `BOOL` | `bool *out` |
| `primitive_char_ptr` | `POINTER` to canonical `CHAR_S` | `char *buf` in unusual non-specialized cases |
| `primitive_schar_ptr` | `POINTER` to canonical `SCHAR` | `signed char *p` |
| `primitive_uchar_ptr` | `POINTER` to canonical `UCHAR` / `CHAR_U` | `unsigned char *p` |
| `primitive_short_ptr` | `POINTER` to canonical `SHORT` | `short *p` |
| `primitive_ushort_ptr` | `POINTER` to canonical `USHORT` | `unsigned short *p` |
| `primitive_int_ptr` | `POINTER` to canonical `INT` | `int *p` |
| `primitive_uint_ptr` | `POINTER` to canonical `UINT` | `unsigned int *p` |
| `primitive_long_ptr` | `POINTER` to canonical `LONG` | `long *p` |
| `primitive_ulong_ptr` | `POINTER` to canonical `ULONG` | `size_t *p` after canonicalization |
| `primitive_longlong_ptr` | `POINTER` to canonical `LONGLONG` | `long long *p` |
| `primitive_ulonglong_ptr` | `POINTER` to canonical `ULONGLONG` | `unsigned long long *p` |
| `primitive_float_ptr` | `POINTER` to canonical `FLOAT` | `float *p` |
| `primitive_double_ptr` | `POINTER` to canonical `DOUBLE` | `double *p` |
| `primitive_longdouble_ptr` | `POINTER` to canonical `LONGDOUBLE` | `long double *p` |
| `primitive_wchar_ptr` | `POINTER` to canonical `WCHAR` | `wchar_t *p` |
| `primitive_char16_ptr` | `POINTER` to canonical `CHAR16` | `char16_t *p` |
| `primitive_char32_ptr` | `POINTER` to canonical `CHAR32` | `char32_t *p` |
| `ptr` | Fallback pointer case not matched above | `const int *p` if the specialized primitive-pointer case does not apply through some wrapper path |

#### Array / aggregate / other bases

| Base tag | AST shape | Example |
|---|---|---|
| `fixed_array` | `TypeKind.CONSTANTARRAY` | `int a[10]` |
| `incomplete_array` | `TypeKind.INCOMPLETEARRAY` | `char a[]` |
| `variable_length_array` | `TypeKind.VARIABLEARRAY` | `int a[n]` |
| `record` | defined canonical `RECORD` | `struct Point` |
| `opaque_record` | forward-declared canonical `RECORD` | `struct Opaque` |
| `enum` | `TypeKind.ENUM` | `enum Mode` |
| `typedef_ref` | `TypeKind.TYPEDEF` with invalid canonical type | unusual typedef fallback |
| `function_proto` | `TypeKind.FUNCTIONPROTO` | direct function prototype type |
| `function_noproto` | `TypeKind.FUNCTIONNOPROTO` | K&R-style function type |
| `unexposed_type` | `TypeKind.UNEXPOSED` | compiler-specific unexposed type |
| `type_<KIND>` | fallback for unclassified `TypeKind` | e.g. `type_LVALUEREFERENCE` |

#### Function-only parameter overrides

These are not emitted directly by `classify_type()`. They are assigned by `scan_function()` when the parameter's canonical type is a record passed by value.

| Base tag | Trigger | Example |
|---|---|---|
| `struct_by_value` | Canonical parameter type is a struct record | `void draw(struct Rect r)` |
| `union_by_value` | Canonical parameter type is a union record | `void set(union Value v)` |

### Modifier Tags

Modifier tags come from qualifiers, typedef layers, and recursive element or pointee analysis. The list below is exhaustive for the current implementation.

| Modifier tag | Source AST pattern | Example |
|---|---|---|
| `typedef_ref` | `TYPEDEF` whose canonical type is valid | `size_t n` |
| `typedef_ptr` | pointer whose pointee classification contains `typedef_ref` | `size_t *n` |
| `ptr_to_const` | pointer whose pointee is const-qualified, except the special `const_char_ptr` case where const is folded into the base | `const Foo *p` |
| `const_ptr` | pointer type itself is const-qualified | `int *const p` |
| `ptr_to_volatile` | pointer whose pointee is volatile-qualified | `volatile Foo *p` |
| `const_qualified` | type itself is const-qualified | `const int n` |
| `volatile_qualified` | type itself is volatile-qualified | `volatile int n` |
| `fixed_array_of_<base>` | constant array whose element type normalizes to `<base>` | `float a[4]` -> `fixed_array_of_primitive_float` |
| `fixed_array_of_typedef` | constant array whose element classification contains `typedef_ref` | `size_t a[4]` |
| `multidim_array` | constant array whose element base is itself `fixed_array` | `int a[3][3]` |
| `incomplete_array_of_<base>` | incomplete array whose element type normalizes to `<base>` | `char *argv[]` |
| `incomplete_array_of_typedef` | incomplete array whose element classification contains `typedef_ref` | `size_t a[]` |
| `variable_length_array_of_<base>` | VLA whose element type normalizes to `<base>` | `int a[n]` |
| `variable_length_array_of_typedef` | VLA whose element classification contains `typedef_ref` | typedef-backed VLA |
| `struct_by_value_small` | function parameter override: struct-by-value size `<= 16` | `struct Point p` |
| `struct_by_value_large` | function parameter override: struct-by-value size `> 16` | `struct Big p` |
| `union_by_value_small` | function parameter override: union-by-value size `<= 16` | `union Value v` |
| `union_by_value_large` | function parameter override: union-by-value size `> 16` | `union BigValue v` |

### About `const_char_ptr` vs `ptr_to_const`

`const_char_ptr` already includes the pointee-const information in the base tag. That is why the scanner now emits:

- `base_tag=const_char_ptr`
- no `ptr_to_const` modifier

For other pointer kinds, the constness stays separate:

- `const Foo *` -> `base_tag=struct_ptr` or `opaque_ptr`, plus `modifier_tags=[ptr_to_const]`
- `const int *` -> `base_tag=primitive_int_ptr`, plus `modifier_tags=[ptr_to_const]`

The special-casing exists because C string pointers are common enough to deserve their own dedicated base tag.

## Interpreting The Report

For test planning, the primary views are:

- `stats.function_groups.function_x_return`
- `stats.function_groups.function_x_param`

Use `full_signature` only for edge cases where parameter interaction matters and pairwise grouping is too coarse.

## Example Queries

Top `function x return` groups:

```bash
python3 - <<'PY'
import json
with open("sysroot_grammar_report.json") as f:
    data = json.load(f)
for k, v in list(data["stats"]["function_groups"]["function_x_return"].items())[:30]:
    print(f"{v:6d}  {k}")
PY
```

Top `function x param` groups:

```bash
python3 - <<'PY'
import json
with open("sysroot_grammar_report.json") as f:
    data = json.load(f)
for k, v in list(data["stats"]["function_groups"]["function_x_param"].items())[:50]:
    print(f"{v:6d}  {k}")
PY
```

Inspect one function:

```bash
python3 - <<'PY'
import json

name = "HMS_ModuleInstall_GetInstalledModule"

with open("sysroot_grammar_report.json") as f:
    data = json.load(f)

for fn in data["functions"]:
    if fn["name"] == name:
        print("function_group_key:", fn["function_group_key"])
        print("return:", fn["return"])
        print("parameters:", fn["parameters"])
        print("matrix_keys:", fn["matrix_keys"])
        break
PY
```
# Sysroot Function Group Scanner

Scans OpenHarmony and HMS sysroot headers annotated with `@addtogroup` and produces a function-centered JSON report for Kotlin/Native `cinterop` test planning.

The report is intentionally grouped around:

- function-level flags
- return type groups
- per-parameter type groups

## Prerequisites

- Python 3.10+
- DevEco Studio installed at `/Applications/DevEco-Studio.app/`
- `libclang` Python bindings: `pip install libclang`

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install libclang
```

## Usage

Full scan:

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

```text
scan_sysroot_grammar.py   # CLI entry point
scanner/
  config.py               # SDK paths, constants
  helpers.py              # FileCache, path utilities
  coverage.py             # AST coverage tracker
  classify.py             # Type normalization
  scanners.py             # Function scanner and declaration scanners
  walker.py               # AST walker
  parse.py                # Header discovery, umbrella generation, TU parsing
```

## Output

The JSON report contains:

- `functions[]` -- one object per function declaration
- `coverage` -- AST coverage report
- `stats.total_functions` -- total number of function entries in the report
- `stats.function_groups` -- precomputed grouping counts

`stats.function_groups` contains:

- `function_x_return`
- `function_x_param`
- `full_signature`
- `return_shapes`
- `param_shapes`

## How Scanning Works

### Step 1: Select the API surface

Only headers containing `@addtogroup` are treated as public API. In a full run, the scanner discovers those headers under the OpenHarmony and HMS sysroots.

### Step 2: Build the parse input

For a full run, the scanner generates an umbrella header that includes all API headers. For `--single`, it parses only the requested header.

Example umbrella fragment:

```c
#include "/path/to/database/rdb/relational_store.h"
#include "/path/to/AppGalleryKit/module_install.h"
```

### Step 3: Parse with libclang

The umbrella or single header is parsed with the OpenHarmony target/sysroot flags. Diagnostics are written to stderr.

### Step 4: Walk the AST

The walker still visits all top-level declarations for coverage accounting, but the JSON payload keeps only `FUNCTION_DECL` entries. For `--single`, the output is filtered back down to declarations whose source location is the requested header itself, so transitive includes do not pollute the grouping matrix.

### Step 5: Normalize each function

Each function is split into three grouping layers:

1. `function_group_key`
2. `return`
3. `parameters[]`

The return and each parameter have:

- one `base_tag`
- zero or more `modifier_tags`
- one readable `key`

### Step 6: Emit matrix-ready keys

Each function entry contains:

- `matrix_keys.function_x_return`
- `matrix_keys.function_x_params[]`
- `matrix_keys.full_signature`

This is the main data for test-matrix construction.

## Example

For:

```c
ModuleInstall_ErrCode HMS_ModuleInstall_GetInstalledModule(
    const char *moduleName,
    unsigned int length,
    ModuleInstall_InstalledModule **installedModule
);
```

the scanner emits:

```json
{
  "kind": "function",
  "name": "HMS_ModuleInstall_GetInstalledModule",
  "file": "AppGalleryKit/module_install.h",
  "line": 177,
  "function_group_key": "plain_function",
  "function_flags": [],
  "return": {
    "c_type": "ModuleInstall_ErrCode",
    "base_tag": "enum",
    "modifier_tags": ["typedef_ref"],
    "key": "enum | mod:typedef_ref"
  },
  "parameters": [
    {
      "index": 0,
      "name": "moduleName",
      "c_type": "const char *",
      "base_tag": "const_char_ptr",
      "modifier_tags": ["ptr_to_const"],
      "key": "const_char_ptr | mod:ptr_to_const"
    },
    {
      "index": 1,
      "name": "length",
      "c_type": "unsigned int",
      "base_tag": "primitive_uint",
      "modifier_tags": [],
      "key": "primitive_uint"
    },
    {
      "index": 2,
      "name": "installedModule",
      "c_type": "ModuleInstall_InstalledModule **",
      "base_tag": "double_ptr",
      "modifier_tags": [],
      "key": "double_ptr"
    }
  ],
  "matrix_keys": {
    "function_x_return": "plain_function ; ret=enum | mod:typedef_ref",
    "function_x_params": [
      "plain_function ; param=const_char_ptr | mod:ptr_to_const",
      "plain_function ; param=primitive_uint",
      "plain_function ; param=double_ptr"
    ],
    "full_signature": "plain_function ; ret=enum | mod:typedef_ref ; param[0]=const_char_ptr | mod:ptr_to_const ; param[1]=primitive_uint ; param[2]=double_ptr"
  }
}
```

## Group Reference

### Function Group Key

`function_group_key` is built from function-level flags. Common values:

- `plain_function`
- `void_return`
- `ptr_return`
- `variadic`
- `has_unnamed_param`
- combinations such as `has_unnamed_param + ptr_return`

### Common Base Tags

Common `base_tag` values include:

- primitives: `primitive_int`, `primitive_uint`, `primitive_ulong`, `primitive_bool`, `primitive_float`, `primitive_double`
- primitive pointers: `primitive_int_ptr`, `primitive_uint_ptr`, `primitive_bool_ptr`, `primitive_ulong_ptr`
- character/string pointers: `char_ptr`, `const_char_ptr`
- opaque/data pointers: `void_ptr`, `struct_ptr`, `opaque_ptr`, `double_ptr`
- enums and callbacks: `enum`, `enum_ptr`, `function_pointer`, `function_proto`
- aggregates and arrays: `record`, `fixed_array`, `incomplete_array`

### Common Modifier Tags

Common `modifier_tags` include:

- `typedef_ref`
- `typedef_ptr`
- `ptr_to_const`
- `const_ptr`
- `const_qualified`

## Interpreting The Report

For test planning, the primary views are:

- `stats.function_groups.function_x_return`
- `stats.function_groups.function_x_param`

Use `full_signature` only for edge cases where parameter interaction matters and pairwise grouping is too coarse.

## Example Queries

Top `function x return` groups:

```bash
python3 - <<'PY'
import json
with open("sysroot_grammar_report.json") as f:
    data = json.load(f)
for k, v in list(data["stats"]["function_groups"]["function_x_return"].items())[:30]:
    print(f"{v:6d}  {k}")
PY
```

Top `function x param` groups:

```bash
python3 - <<'PY'
import json
with open("sysroot_grammar_report.json") as f:
    data = json.load(f)
for k, v in list(data["stats"]["function_groups"]["function_x_param"].items())[:50]:
    print(f"{v:6d}  {k}")
PY
```

Inspect one function:

```bash
python3 - <<'PY'
import json

name = "HMS_ModuleInstall_GetInstalledModule"

with open("sysroot_grammar_report.json") as f:
    data = json.load(f)

for fn in data["functions"]:
    if fn["name"] == name:
        print("function_group_key:", fn["function_group_key"])
        print("return:", fn["return"])
        print("parameters:", fn["parameters"])
        print("matrix_keys:", fn["matrix_keys"])
        break
PY
```
# Sysroot Grammar Scanner

Scans OpenHarmony and HMS sysroot headers (those annotated with `@addtogroup`) for C/C++ grammar patterns relevant to Kotlin Native cinterop testing. Produces a searchable JSON database where every declaration carries grammar tags and verbatim source text, plus an AST coverage report on stderr.

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
- `stats.tag_counts` -- raw tag frequency counts
- `stats.function_groups` -- precomputed function x return/param grouping counts

## How Scanning Works

The scanner is designed to reduce the Kotlin/Native `cinterop` test matrix by grouping APIs around function shape, return type, and per-parameter type.

### Step 1: Select the API surface

Only headers containing `@addtogroup` are treated as public API. In a full run, the scanner finds those headers in the OpenHarmony and HMS sysroots.

### Step 2: Build the parse input

For a full run, the scanner generates an umbrella header that `#include`s all selected API headers. For `--single`, it parses only the requested header. The output for `--single` is then filtered back down to declarations whose source location is the requested header itself, so transitive includes do not pollute the grouping matrix.

Example umbrella fragment:

```c
#include "/path/to/database/rdb/relational_store.h"
#include "/path/to/AppGalleryKit/module_install.h"
```

### Step 3: Parse with libclang

The umbrella or single header is parsed with the OpenHarmony target/sysroot flags. Diagnostics are written to stderr, and the AST is traversed.

### Step 4: Catalog declarations

The walker records top-level declarations such as functions, structs, enums, typedefs, variables, and macros. For wrapper-test design, the most important kind is `FUNCTION_DECL`.

### Step 5: Normalize each function into grouping pieces

Each function is split into:

1. function-level flags
2. one normalized return group
3. one normalized parameter group for each parameter

Each return or parameter group has:

- one base tag
- zero or more modifier tags

This keeps grouping centered on `function x return/param types` instead of a flat tag bag.

### Step 6: Emit matrix-ready keys

Each function entry contains precomputed grouping keys:

- `details.function_group_key`
- `details.return_group`
- `details.param_groups`
- `details.matrix_keys.function_x_return`
- `details.matrix_keys.function_x_params[]`
- `details.matrix_keys.full_signature`

The report also aggregates these under `stats.function_groups`.

### Example

For:

```c
ModuleInstall_ErrCode HMS_ModuleInstall_GetInstalledModule(
    const char *moduleName,
    unsigned int length,
    ModuleInstall_InstalledModule **installedModule
);
```

The scanner emits a function-centered grouping like:

```json
{
  "function_group_key": "plain_function",
  "return_group": {
    "base_tag": "enum",
    "modifier_tags": ["typedef_ref"],
    "key": "enum | mod:typedef_ref"
  },
  "param_groups": [
    {
      "index": 0,
      "key": "const_char_ptr | mod:ptr_to_const"
    },
    {
      "index": 1,
      "key": "primitive_uint"
    },
    {
      "index": 2,
      "key": "double_ptr"
    }
  ],
  "matrix_keys": {
    "function_x_return": "plain_function ; ret=enum | mod:typedef_ref",
    "function_x_params": [
      "plain_function ; param=const_char_ptr | mod:ptr_to_const",
      "plain_function ; param=primitive_uint",
      "plain_function ; param=double_ptr"
    ]
  }
}
```

This makes it easy to cover:

- each observed `function x return type` combination
- each observed `function x parameter type` combination

without making every distinct full signature your primary test bucket.

## Grammar Tag Reference

Every declaration in the output carries a `tags` array describing what C/C++ grammar constructs it exercises. These are the tags the scanner can produce, grouped by category.

### Function

| Tag | Description | Example |
|-----|-------------|---------|
| `void_return` | Returns `void` | `void OH_Drawing_CanvasSave(OH_Drawing_Canvas* canvas)` |
| `ptr_return` | Returns a pointer type | `OH_Drawing_Canvas* OH_Drawing_CanvasCreate(void)` |
| `struct_return_by_value` | Returns a struct/union by value | `struct Point2D get_position(void)` |
| `union_return_by_value` | Returns a union by value | `union Value get_value(void)` |
| `struct_return_small` | Returns struct by value, size <= 16 bytes (register-passed on AArch64) | `OH_Drawing_Point2D make_point(float x, float y)` |
| `struct_return_large` | Returns struct by value, size > 16 bytes (indirect return) | `struct LargeStruct get_large_data(void)` |
| `union_return_small` | Returns union by value, size <= 16 bytes | `union SmallValue get_value(void)` |
| `union_return_large` | Returns union by value, size > 16 bytes | `union LargeValue get_value(void)` |
| `variadic` | Variadic function (`...`) | `int printf(const char* format, ...)` |
| `static_inline_with_body` | `static inline` function with body in header | `static inline int min(int a, int b) { return a < b ? a : b; }` |
| `inline_non_static` | Non-static `inline` function | `inline void swap(int* a, int* b)` |
| `has_unnamed_param` | Has at least one unnamed parameter | `void func(int, float unnamed_param)` |

Function attributes are still collected under `details.attributes` / `details.attribute_tags`, but metadata-only annotations such as availability, visibility, and deprecation are no longer promoted into top-level function tags.

### Function Return Type (prefixed `ret_`)

Each return contributes:

- exactly one base tag: `ret_<base>`
- zero or more modifiers: `ret_mod_<modifier>`

This keeps the matrix stable while still preserving aliasing and qualifiers.

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
| `ret_primitive_uint_ptr` | Returns pointer to primitive integer data | `uint32_t* get_ids(void)` |
| `ret_enum` | Returns an enum type | `OH_Drawing_ErrorCode OH_Drawing_CanvasDrawPixelMapNine(...)` |
| `ret_enum_ptr` | Returns pointer to enum data | `VkResult* get_status_out(void)` |
| `ret_struct_ptr` | Returns pointer to defined struct | `struct tm* localtime(const time_t* timer)` |
| `ret_opaque_ptr` | Returns pointer to forward-declared struct | `OH_Drawing_Canvas* OH_Drawing_CanvasCreate(void)` |
| `ret_void_ptr` | Returns `void *` | `void* malloc(size_t size)` |
| `ret_double_ptr` | Returns pointer-to-pointer | `char** environ` |
| `ret_ptr` | Returns generic pointer | `const void* get_data(void)` |
| `ret_function_pointer` | Returns a function pointer | `int (*get_comparator(void))(const void*, const void*)` |
| `ret_mod_typedef_ref` | Return spelling is a typedef alias | `size_t get_size(void)` |
| `ret_mod_typedef_ptr` | Return pointer pointee spelling is a typedef alias | `uint32_t* get_ids(void)` |
| `ret_mod_ptr_to_const` | Returns pointer to `const` | `const char* getenv(const char* name)` |
| `ret_mod_const_qualified` | Return type itself is `const` qualified | `const int get_value(void)` |

### Function Parameter Types (prefixed `param_`)

Each parameter contributes:

- exactly one base tag: `param_<base>`
- zero or more modifiers: `param_mod_<modifier>`

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
| `param_primitive_bool_ptr` | Pointer to primitive bool data | `void set_enabled(bool *enabled)` |
| `param_primitive_uint_ptr` | Pointer to primitive integer data | `void next_id(uint32_t *id)` |
| `param_enum` | Parameter of enum type | `void OH_Drawing_CanvasDrawColor(..., OH_Drawing_BlendMode blendMode)` |
| `param_enum_ptr` | Pointer to enum data | `void next_state(enum State *state)` |
| `param_struct_ptr` | Pointer to defined struct | `void localtime_r(const time_t* timer, struct tm* result)` |
| `param_opaque_ptr` | Pointer to forward-declared (opaque) struct | `void OH_Drawing_CanvasDestroy(OH_Drawing_Canvas* canvas)` |
| `param_void_ptr` | `void *` parameter | `void* memcpy(void* dest, const void* src, size_t n)` |
| `param_double_ptr` | Pointer-to-pointer parameter | `int main(int argc, char** argv)` |
| `param_ptr` | Generic pointer parameter | `void process_data(const void* data)` |
| `param_char_ptr` | `char *` parameter | `char* strcpy(char* dest, const char* src)` |
| `param_const_char_ptr` | `const char *` parameter | `size_t strlen(const char* str)` |
| `param_function_pointer` | Function pointer parameter | `void qsort(void* base, size_t n, size_t size, int (*compar)(const void*, const void*))` |
| `param_function_proto` | Function prototype parameter | `void register_callback(void (*callback)(int))` |
| `param_incomplete_array` | Incomplete array parameter (`T[]`) | `OH_NN_ReturnCode HMS_HiAISingleOpExecutor_Execute(..., HiAI_SingleOpTensor* input[])` |
| `param_fixed_array` | Fixed-size array parameter | `void process_array(int arr[10])` |
| `param_record` | Struct/union parameter by value | `void print_point(struct Point p)` |
| `param_struct_by_value` | Struct passed by value | `void draw_rect(struct Rect rect)` |
| `param_union_by_value` | Union passed by value | `void set_value(union Value value)` |
| `param_struct_by_value_small` | Struct by value, size <= 16 bytes | `void OH_Drawing_CanvasDrawShadow(..., OH_Drawing_Point3D planeParams)` |
| `param_struct_by_value_large` | Struct by value, size > 16 bytes | `void HMS_HiAISingleOpDescriptor_CreateConvolution(HiAISingleOpDescriptor_ConvolutionParam param)` |
| `param_union_by_value_small` | Union by value, size <= 16 bytes | `void set_value(union SmallValue value)` |
| `param_union_by_value_large` | Union by value, size > 16 bytes | `void set_value(union LargeValue value)` |
| `param_type_LVALUEREFERENCE` | C++ lvalue reference parameter (`T&`) | `void swap(int& a, int& b)` |
| `param_mod_typedef_ref` | Parameter spelling is a typedef alias | `void set_size(size_t size)` |
| `param_mod_typedef_ptr` | Parameter pointer pointee spelling is a typedef alias | `void next_id(uint32_t *id)` |
| `param_mod_ptr_to_const` | Pointer to `const` data | `void OH_Drawing_CanvasAttachPen(..., const OH_Drawing_Pen* pen)` |
| `param_mod_const_ptr` | `const` pointer (`T *const`) | `void func(int* const ptr)` |
| `param_mod_const_qualified` | `const`-qualified parameter | `void func(const int value)` |

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

The enumerator count is stored as a numeric field `details.value_count` in the JSON output rather than as a tag, since the count is a continuous value (ranges from 0 to 1235 in this sysroot) and would generate excessive unique tags.

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

When the scanner traverses the Clang AST, every node is classified into one of four dispositions: **handled** (produces output), **child-handled** (processed by a parent handler), **attribute** (mapped to `attr_*` tags), or **skipped** (intentionally ignored). The coverage report on stderr proves no node type falls through unhandled.

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

These are never visited at the top level of the walker. Instead, each parent handler iterates over its children and processes these kinds directly.

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

These AST node kinds carry no grammar information relevant to cinterop and are skipped without producing output.

| CursorKind | Count | Why skipped |
|---|---|---|
| `MACRO_INSTANTIATION` | ~6600 | Records where a `#define` macro is *used* (expanded) in source code. The scanner only cares about macro *definitions* (`MACRO_DEFINITION`), not their expansion sites. Macro instantiation sites carry no type or signature information -- they are just textual expansion points tracked by the preprocessor. Scanning these would produce thousands of duplicate entries with no grammar value. |
| `TYPE_REF` | ~5000 | A reference to a previously declared type, e.g. when `OH_Drawing_Canvas` appears as a parameter type, libclang creates a `TYPE_REF` child pointing back to the struct declaration. These are internal cross-reference links in the AST -- the actual type information is already fully captured when we classify the parameter's type via `classify_type()`. Processing them would double-count every type mention. |
| `INCLUSION_DIRECTIVE` | ~1300 | `#include "foo.h"` directives. These are preprocessor bookkeeping that tells libclang which file was included. The scanner already processes all declarations from included files via the umbrella header -- the include directives themselves carry no declaration or type grammar. |
| `UNEXPOSED_DECL` | ~78 | Declarations that libclang cannot fully represent in its public API. In this sysroot these are typically `extern "C"` blocks that Clang partially models as `UNEXPOSED_DECL` instead of `LINKAGE_SPEC` (a known libclang quirk). The actual declarations *inside* these blocks are still visited as children, so no content is lost. |

### TypeKind and AttributeKind

TypeKinds (28 encountered) and AttributeKinds (6 encountered) are not skipped -- they are all processed by `classify_type()` and the attribute-handling logic respectively. Every TypeKind maps to one or more grammar tags via the `PRIMITIVE_MAP` and pointer/array/record classification logic in `scanner/classify.py`. Every AttributeKind maps to an `attr_*` tag as shown in the table above.
