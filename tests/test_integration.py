#!/usr/bin/env python3
"""
Integration test: parse synthetic test headers with the grammar scanner
and verify that every expected grammar tag is produced.

Usage:
    python3 tests/test_integration.py 2>/tmp/test_integration_stderr.log

Exit code 0 = all tags found.  Non-zero = some tags missing.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
TEST_HEADER = REPO / "tests" / "test_headers" / "test_all_grammar.h"

EXPECTED_TAGS = {
    # ── Function-level tags ──────────────────────────────────────────
    "void_return",
    "ptr_return",
    "struct_return_by_value",
    "struct_return_small",
    "struct_return_large",
    "variadic",
    "static_inline_with_body",
    "inline_non_static",
    "has_unnamed_param",
    "has_attributes",

    # ── Attribute tags (on functions) ────────────────────────────────
    "attr_deprecated",
    "attr_visibility",
    "attr_format",
    "attr_noreturn",
    "attr_constructor",
    "attr_const",

    # ── Return-type tags (ret_ prefix) ──────────────────────────────
    "ret_primitive_bool",
    "ret_primitive_schar",
    "ret_primitive_uchar",
    "ret_primitive_short",
    "ret_primitive_ushort",
    "ret_primitive_int",
    "ret_primitive_uint",
    "ret_primitive_long",
    "ret_primitive_ulong",
    "ret_primitive_longlong",
    "ret_primitive_ulonglong",
    "ret_primitive_float",
    "ret_primitive_double",
    "ret_primitive_longdouble",
    "ret_primitive_wchar",
    "ret_primitive_char16",
    "ret_primitive_char32",
    "ret_complex",
    "ret_void_ptr",
    "ret_char_ptr",
    "ret_const_char_ptr",
    "ret_double_ptr",
    "ret_function_pointer",
    "ret_struct_ptr",
    "ret_opaque_ptr",
    "ret_ptr",
    "ret_ptr_to_const",
    "ret_enum",
    "ret_typedef_ref",

    # ── Parameter-type tags (param_ prefix) ─────────────────────────
    "param_primitive_bool",
    "param_primitive_schar",
    "param_primitive_uchar",
    "param_primitive_short",
    "param_primitive_ushort",
    "param_primitive_int",
    "param_primitive_uint",
    "param_primitive_long",
    "param_primitive_ulong",
    "param_primitive_longlong",
    "param_primitive_ulonglong",
    "param_primitive_float",
    "param_primitive_double",
    "param_primitive_longdouble",
    "param_primitive_wchar",
    "param_primitive_char16",
    "param_primitive_char32",
    "param_void_ptr",
    "param_char_ptr",
    "param_const_char_ptr",
    "param_double_ptr",
    "param_function_pointer",
    "param_struct_ptr",
    "param_opaque_ptr",
    "param_ptr",
    "param_ptr_to_const",
    "param_const_ptr",
    "param_ptr_to_volatile",
    "param_struct_by_value",
    "param_struct_by_value_small",
    "param_struct_by_value_large",
    "param_fixed_array",
    "param_incomplete_array",
    "param_enum",
    "param_typedef_ref",
    "param_volatile_qualified",

    # ── Struct / Union tags ─────────────────────────────────────────
    "struct",
    "union",
    "opaque",
    "anonymous",
    "size_small",
    "size_medium",
    "size_large",
    "has_bitfield",
    "has_fixed_array_field",
    "has_multidim_array_field",
    "has_flexible_array_member",
    "has_function_pointer_field",
    "has_struct_by_value_field",
    "has_union_field",
    "has_nested_struct",
    "has_nested_union",
    "has_anonymous_member",
    "vtable_like",
    "self_referential",
    "pragma_packed",
    "attr_packed",
    "attr_aligned",

    # ── Enum tags ───────────────────────────────────────────────────
    "named_enum",
    "anonymous_enum",
    "has_negative_values",
    "has_explicit_values",
    "large_range",

    # ── Typedef tags ────────────────────────────────────────────────
    "to_struct",
    "to_union",
    "to_enum",
    "to_opaque",
    "to_function_pointer",
    "to_pointer",
    "to_primitive",
    "to_array",
    "to_void",
    "chain",
    "type_alias",

    # ── Variable tags ───────────────────────────────────────────────
    "extern_var",
    "static_var",
    "const_var",
    "volatile_var",
    "thread_local_var",
    "const_qualified",

    # ── Macro tags ──────────────────────────────────────────────────
    "object_like_macro",
    "function_like_macro",
    "empty_macro",
    "macro_uses_builtin",
    "macro_uses_typeof",
    "macro_uses_extension",

    # ── Extra tags that may appear on various entries ──────────────
    "param_const_qualified",
    "param_record",

    # ── Type classification tags (appear on variable entries) ────────
    "primitive_int",
    "record",
    "enum",
    "typedef_ref",
    "fixed_array",
    "void_ptr",
    "char_ptr",
    "const_char_ptr",
    "struct_ptr",
    "opaque_ptr",
    "ptr",
    "function_pointer",
    "double_ptr",
    "ptr_to_const",
    "const_ptr",
    "ptr_to_volatile",
    "volatile_qualified",
}

# Tags that are inherently difficult to trigger from test headers, or depend
# on platform / libclang internals.  We document them but don't fail the
# test if they are absent.
OPTIONAL_TAGS = {
    "function_noproto",       # K&R style — invalid in C++ mode
    "variable_length_array",  # C99 VLA — not valid in C++17
    "unexposed_type",         # depends on libclang internals
    "multidim_array",         # classify_type level on variables, needs nested arrays
    "complex",                # _Complex may not be available in C++ mode
    "ret_complex",            # depends on complex being available
    "attr_asm_label",         # requires specific __asm__("label") syntax
    "attr_availability",      # requires OHOS-specific attribute syntax
    "inline_non_static",      # C++ may treat inline differently

    # aarch64: char is unsigned → CHAR_S never appears, so primitive_char
    # (which maps to CHAR_S) won't be produced.  On x86, char is signed.
    "ret_primitive_char",
    "param_primitive_char",

    # void/opaque_record/function_proto can't appear as top-level entry tags:
    # void → can't be a variable type
    # opaque_record → can't declare var of incomplete type
    # function_proto → can't have var of function type
    "void",
    "opaque_record",
    "function_proto",
}


def run_scanner(header_path: str) -> dict:
    cmd = [
        sys.executable,
        str(REPO / "scan_sysroot_grammar.py"),
        "--single", header_path,
    ]
    result = subprocess.run(
        cmd, capture_output=True, text=True, cwd=str(REPO),
    )
    stderr_log = Path("/tmp/test_integration_stderr.log")
    stderr_log.write_text(result.stderr)
    if result.returncode != 0:
        print(f"ERROR: scanner exited {result.returncode}", file=sys.stderr)
        print(result.stderr, file=sys.stderr)
        sys.exit(2)
    return json.loads(result.stdout)


def collect_all_tags(report: dict) -> set:
    tags = set()
    for entry in report.get("entries", []):
        tags.update(entry.get("tags", []))
    return tags


def main():
    print(f"Test header: {TEST_HEADER}")
    print(f"Expected tags: {len(EXPECTED_TAGS)} required, {len(OPTIONAL_TAGS)} optional")
    print()

    report = run_scanner(str(TEST_HEADER))
    found_tags = collect_all_tags(report)
    entry_count = len(report.get("entries", []))

    print(f"Scanner produced {entry_count} entries with {len(found_tags)} unique tags")
    print()

    required_tags = EXPECTED_TAGS - OPTIONAL_TAGS
    optional_found = found_tags & OPTIONAL_TAGS
    required_found = found_tags & required_tags
    required_missing = required_tags - found_tags
    extra_tags = found_tags - EXPECTED_TAGS - OPTIONAL_TAGS

    print(f"Required tags: {len(required_found)}/{len(required_tags)} found")
    print(f"Optional tags: {len(optional_found)}/{len(OPTIONAL_TAGS)} found")
    if extra_tags:
        print(f"Extra tags (produced but not in expected set): {len(extra_tags)}")
        for t in sorted(extra_tags):
            print(f"  + {t}")
    print()

    if required_missing:
        print(f"MISSING required tags ({len(required_missing)}):")
        for t in sorted(required_missing):
            print(f"  - {t}")
        print()
        print("FAIL: not all required tags were produced")
        return 1
    else:
        print("PASS: all required grammar tags detected")

    if optional_found:
        print(f"\nOptional tags also detected: {sorted(optional_found)}")
    optional_missing = OPTIONAL_TAGS - found_tags
    if optional_missing:
        print(f"Optional tags not detected (OK): {sorted(optional_missing)}")

    print(f"\n{'='*60}")
    print(f"RESULT: PASS  ({len(required_found)} required + "
          f"{len(optional_found)} optional = {len(found_tags)} total tags)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
