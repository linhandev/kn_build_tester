#!/usr/bin/env python3
"""
Run the scanner on each test header and validate that the expected
grammar tags appear in the output.
"""
import sys
import os
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import clang.cindex as ci
from scanner.helpers import FileCache, relative_path
from scanner.coverage import Coverage
from scanner.walker import walk
from scanner.parse import parse_tu


TEST_DIR = Path(__file__).resolve().parent / "test_headers"

PASS = 0
FAIL = 0


def scan_header(header_path: str) -> list[dict]:
    hdr = str(Path(header_path).resolve())
    allow_paths = frozenset({str(Path(hdr).parent) + "/"})
    index = ci.Index.create()
    fc = FileCache()
    cov = Coverage()
    entries: list[dict] = []
    atg = {hdr}
    tu = parse_tu(index, hdr)
    walk(tu.cursor, entries, fc, atg, cov, allow_paths=allow_paths)
    primary_rel = relative_path(hdr)
    return [e for e in entries if e.get("file") == primary_rel]


def find_function(entries, name):
    for e in entries:
        if e.get("kind") == "function" and e.get("name") == name:
            return e
    return None


def find_entry(entries, kind, name):
    for e in entries:
        if e.get("kind") == kind and e.get("name") == name:
            return e
    return None


def check(label, condition, detail=""):
    global PASS, FAIL
    if condition:
        PASS += 1
        print(f"  PASS  {label}")
    else:
        FAIL += 1
        msg = f"  FAIL  {label}"
        if detail:
            msg += f"  -- {detail}"
        print(msg)


def validate_primitives(entries):
    print("\n=== test_primitives.h ===")

    primitives = [
        ("ret_bool", "primitive_bool"), ("ret_char", "primitive_uchar"),
        ("ret_schar", "primitive_schar"), ("ret_uchar", "primitive_uchar"),
        ("ret_short", "primitive_short"), ("ret_ushort", "primitive_ushort"),
        ("ret_int", "primitive_int"), ("ret_uint", "primitive_uint"),
        ("ret_long", "primitive_long"), ("ret_ulong", "primitive_ulong"),
        ("ret_longlong", "primitive_longlong"), ("ret_ulonglong", "primitive_ulonglong"),
        ("ret_float", "primitive_float"), ("ret_double", "primitive_double"),
        ("ret_longdouble", "primitive_longdouble"), ("ret_wchar", "primitive_wchar"),
        ("ret_char16", "primitive_char16"), ("ret_char32", "primitive_char32"),
        ("ret_complex", "complex"),
    ]
    for fname, expected_ret in primitives:
        fn = find_function(entries, fname)
        check(f"{fname} return base_tag",
              fn and fn["return"]["base_tag"] == expected_ret,
              f"expected {expected_ret}, got {fn['return']['base_tag'] if fn else 'NOT FOUND'}")

    param_primitives = [
        ("take_bool", "primitive_bool"), ("take_char", "primitive_uchar"),
        ("take_schar", "primitive_schar"), ("take_uchar", "primitive_uchar"),
        ("take_short", "primitive_short"), ("take_ushort", "primitive_ushort"),
        ("take_int", "primitive_int"), ("take_uint", "primitive_uint"),
        ("take_long", "primitive_long"), ("take_ulong", "primitive_ulong"),
        ("take_longlong", "primitive_longlong"), ("take_ulonglong", "primitive_ulonglong"),
        ("take_float", "primitive_float"), ("take_double", "primitive_double"),
        ("take_longdouble", "primitive_longdouble"), ("take_wchar", "primitive_wchar"),
        ("take_char16", "primitive_char16"), ("take_char32", "primitive_char32"),
    ]
    for fname, expected_param in param_primitives:
        fn = find_function(entries, fname)
        check(f"{fname} param[0] base_tag",
              fn and fn["parameters"][0]["base_tag"] == expected_param,
              f"expected {expected_param}, got {fn['parameters'][0]['base_tag'] if fn else 'NOT FOUND'}")

    fn = find_function(entries, "void_func")
    check("void_func return base_tag",
          fn and fn["return"]["base_tag"] == "void",
          f"got {fn['return']['base_tag'] if fn else 'NOT FOUND'}")


def validate_pointers(entries):
    print("\n=== test_pointers.h ===")

    cases = [
        ("ret_void_ptr", "ret", "void_ptr", []),
        ("take_void_ptr", "p0", "void_ptr", []),
        ("ret_char_ptr", "ret", "char_ptr", []),
        ("take_char_ptr", "p0", "char_ptr", []),
        ("ret_const_char_ptr", "ret", "const_char_ptr", []),
        ("take_const_char_ptr", "p0", "const_char_ptr", []),
        ("ret_double_ptr", "ret", "double_ptr", []),
        ("take_double_ptr", "p0", "double_ptr", []),
        ("take_func_ptr", "p0", "function_pointer", []),
        ("ret_func_ptr", "ret", "function_pointer", ["typedef_ref"]),
        ("ret_struct_ptr", "ret", "struct_ptr", []),
        ("take_struct_ptr", "p0", "struct_ptr", []),
        ("ret_opaque_ptr", "ret", "opaque_ptr", []),
        ("take_opaque_ptr", "p0", "opaque_ptr", []),
        ("ret_int_ptr", "ret", "primitive_int_ptr", []),
        ("take_int_ptr", "p0", "primitive_int_ptr", []),
        ("take_ptr_to_const", "p0", "primitive_int_ptr", ["ptr_to_const"]),
        ("take_const_ptr", "p0", "primitive_int_ptr", ["const_ptr"]),
        ("take_ptr_to_volatile", "p0", "primitive_int_ptr", ["ptr_to_volatile"]),
    ]
    for fname, pos, expected_base, expected_mods in cases:
        fn = find_function(entries, fname)
        if pos == "ret":
            actual_base = fn["return"]["base_tag"] if fn else "NOT FOUND"
            actual_mods = fn["return"]["modifier_tags"] if fn else []
        else:
            actual_base = fn["parameters"][0]["base_tag"] if fn else "NOT FOUND"
            actual_mods = fn["parameters"][0]["modifier_tags"] if fn else []
        check(f"{fname} {pos} base_tag={expected_base}",
              fn and actual_base == expected_base,
              f"got {actual_base}")
        for mod in expected_mods:
            check(f"{fname} {pos} has modifier {mod}",
                  mod in actual_mods,
                  f"modifiers={actual_mods}")


def validate_arrays(entries):
    print("\n=== test_arrays.h ===")

    fn = find_function(entries, "take_fixed_array")
    if fn:
        p = fn["parameters"][0]
        check("take_fixed_array p0 base_tag=fixed_array",
              p["base_tag"] == "fixed_array", f"got {p['base_tag']}")
        check("take_fixed_array p0 element_count=10",
              p["element_count"] == 10, f"got {p['element_count']}")
        check("take_fixed_array p0 sizeof=40",
              p["sizeof"] == 40, f"got {p['sizeof']}")
    else:
        check("take_fixed_array found", False, "NOT FOUND")

    fn = find_function(entries, "take_incomplete_array")
    if fn:
        p = fn["parameters"][0]
        check("take_incomplete_array p0 base_tag=incomplete_array",
              p["base_tag"] == "incomplete_array", f"got {p['base_tag']}")
    else:
        check("take_incomplete_array found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithFixedArray")
    if s:
        check("WithFixedArray has_fixed_array_field",
              "has_fixed_array_field" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithFixedArray found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithMultidimArray")
    if s:
        check("WithMultidimArray has_multidim_array_field",
              "has_multidim_array_field" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithMultidimArray found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithFlexArray")
    if s:
        check("WithFlexArray has_flexible_array_member",
              "has_flexible_array_member" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithFlexArray found", False, "NOT FOUND")


def validate_structs(entries):
    print("\n=== test_structs.h ===")

    s = find_entry(entries, "struct", "SmallStruct")
    if s:
        check("SmallStruct size_small", "size_small" in s.get("tags", []),
              f"tags={s.get('tags')}")
        check("SmallStruct sizeof=8",
              s.get("details", {}).get("sizeof") == 8,
              f"got {s.get('details', {}).get('sizeof')}")
    else:
        check("SmallStruct found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "MediumStruct")
    if s:
        check("MediumStruct size_medium", "size_medium" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("MediumStruct found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "LargeStruct")
    if s:
        check("LargeStruct size_large", "size_large" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("LargeStruct found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "OpaqueRecord")
    if s:
        check("OpaqueRecord opaque", "opaque" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("OpaqueRecord found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithBitfield")
    if s:
        check("WithBitfield has_bitfield", "has_bitfield" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithBitfield found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "Outer")
    if s:
        check("Outer has_nested_struct", "has_nested_struct" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("Outer found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithAnonMember")
    if s:
        check("WithAnonMember has_anonymous_member",
              "has_anonymous_member" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithAnonMember found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "VTable")
    if s:
        check("VTable vtable_like", "vtable_like" in s.get("tags", []),
              f"tags={s.get('tags')}")
        check("VTable has_function_pointer_field",
              "has_function_pointer_field" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("VTable found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithStructField")
    if s:
        check("WithStructField has_struct_by_value_field",
              "has_struct_by_value_field" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithStructField found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "WithUnionField")
    if s:
        check("WithUnionField has_union_field",
              "has_union_field" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("WithUnionField found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "ListNode")
    if s:
        check("ListNode self_referential", "self_referential" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("ListNode found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "PragmaPacked")
    if s:
        check("PragmaPacked pragma_packed", "pragma_packed" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("PragmaPacked found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "AttrPacked")
    if s:
        check("AttrPacked attr_packed", "attr_packed" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("AttrPacked found", False, "NOT FOUND")

    s = find_entry(entries, "struct", "AttrAligned")
    if s:
        check("AttrAligned attr_aligned", "attr_aligned" in s.get("tags", []),
              f"tags={s.get('tags')}")
    else:
        check("AttrAligned found", False, "NOT FOUND")

    # struct return by value
    fn = find_function(entries, "ret_small_struct")
    if fn:
        ret = fn["return"]
        check("ret_small_struct return base=struct_by_value",
              ret["base_tag"] == "struct_by_value", f"got {ret['base_tag']}")
        check("ret_small_struct return abi_register_return",
              "abi_register_return" in ret["modifier_tags"],
              f"mods={ret['modifier_tags']}")
        check("ret_small_struct return sizeof=8",
              ret["sizeof"] == 8, f"got {ret['sizeof']}")
    else:
        check("ret_small_struct found", False, "NOT FOUND")

    fn = find_function(entries, "ret_large_struct")
    if fn:
        ret = fn["return"]
        check("ret_large_struct return base=struct_by_value",
              ret["base_tag"] == "struct_by_value", f"got {ret['base_tag']}")
        check("ret_large_struct return abi_indirect_return",
              "abi_indirect_return" in ret["modifier_tags"],
              f"mods={ret['modifier_tags']}")
        check("ret_large_struct return sizeof=128",
              ret["sizeof"] == 128, f"got {ret['sizeof']}")
    else:
        check("ret_large_struct found", False, "NOT FOUND")

    # struct param by value
    fn = find_function(entries, "take_small_struct")
    if fn:
        p = fn["parameters"][0]
        check("take_small_struct p0 base=struct_by_value",
              p["base_tag"] == "struct_by_value", f"got {p['base_tag']}")
        check("take_small_struct p0 struct_by_value_small",
              "struct_by_value_small" in p["modifier_tags"],
              f"mods={p['modifier_tags']}")
        check("take_small_struct p0 sizeof=8",
              p["sizeof"] == 8, f"got {p['sizeof']}")
    else:
        check("take_small_struct found", False, "NOT FOUND")

    fn = find_function(entries, "take_large_struct")
    if fn:
        p = fn["parameters"][0]
        check("take_large_struct p0 base=struct_by_value",
              p["base_tag"] == "struct_by_value", f"got {p['base_tag']}")
        check("take_large_struct p0 struct_by_value_large",
              "struct_by_value_large" in p["modifier_tags"],
              f"mods={p['modifier_tags']}")
        check("take_large_struct p0 sizeof=128",
              p["sizeof"] == 128, f"got {p['sizeof']}")
    else:
        check("take_large_struct found", False, "NOT FOUND")


def validate_functions(entries):
    print("\n=== test_functions.h ===")

    fn = find_function(entries, "my_printf")
    if fn:
        check("my_printf variadic flag",
              "variadic" in fn["function_flags"],
              f"flags={fn['function_flags']}")
        check("my_printf function_group_key",
              fn["function_group_key"] == "variadic",
              f"got {fn['function_group_key']}")
    else:
        check("my_printf found", False, "NOT FOUND")

    fn = find_function(entries, "add_inline")
    if fn:
        check("add_inline static_inline_with_body",
              "static_inline_with_body" in fn["function_flags"],
              f"flags={fn['function_flags']}")
    else:
        check("add_inline found", False, "NOT FOUND")

    fn = find_function(entries, "mul_inline")
    if fn:
        check("mul_inline inline_non_static",
              "inline_non_static" in fn["function_flags"],
              f"flags={fn['function_flags']}")
    else:
        check("mul_inline found", False, "NOT FOUND")

    fn = find_function(entries, "unnamed_param")
    if fn:
        check("unnamed_param has_unnamed_param",
              "has_unnamed_param" in fn["function_flags"],
              f"flags={fn['function_flags']}")
    else:
        check("unnamed_param found", False, "NOT FOUND")

    fn = find_function(entries, "register_callback")
    if fn:
        p = fn["parameters"][0]
        check("register_callback p0 base=function_pointer",
              p["base_tag"] == "function_pointer",
              f"got {p['base_tag']}")
    else:
        check("register_callback found", False, "NOT FOUND")

    fn = find_function(entries, "take_volatile_int")
    if fn:
        p = fn["parameters"][0]
        check("take_volatile_int p0 volatile_qualified",
              "volatile_qualified" in p["modifier_tags"],
              f"mods={p['modifier_tags']}")
    else:
        check("take_volatile_int found", False, "NOT FOUND")

    fn = find_function(entries, "ret_typedef_func")
    if fn:
        check("ret_typedef_func return typedef_ref",
              "typedef_ref" in fn["return"]["modifier_tags"],
              f"mods={fn['return']['modifier_tags']}")
    else:
        check("ret_typedef_func found", False, "NOT FOUND")

    fn = find_function(entries, "ret_enum_func")
    if fn:
        check("ret_enum_func return base=enum",
              fn["return"]["base_tag"] == "enum",
              f"got {fn['return']['base_tag']}")
    else:
        check("ret_enum_func found", False, "NOT FOUND")


def validate_attributes(entries):
    print("\n=== test_attributes.h ===")

    fn = find_function(entries, "old_func")
    if fn:
        check("old_func deprecated spelling captured",
              any("deprecated" in s for s in fn.get("attributes", {}).get("spellings", [])),
              f"spellings={fn.get('attributes', {}).get('spellings')}")
        check("old_func attr_deprecated is metadata (not in semantic tags)",
              "attr_deprecated" not in fn.get("attributes", {}).get("tags", []),
              f"attr_tags={fn.get('attributes', {}).get('tags')}")
    else:
        check("old_func found", False, "NOT FOUND")

    fn = find_function(entries, "visible_func")
    if fn:
        attr_tags = fn.get("attributes", {}).get("tags", [])
        check("visible_func attr_visibility is metadata (not in semantic tags)",
              "attr_visibility" not in attr_tags,
              f"attr_tags={attr_tags}")
    else:
        check("visible_func found", False, "NOT FOUND")

    fn = find_function(entries, "log_message")
    if fn:
        check("log_message variadic",
              "variadic" in fn["function_flags"],
              f"flags={fn['function_flags']}")
        check("log_message attr_format",
              "attr_format" in fn.get("attributes", {}).get("tags", []),
              f"attr_tags={fn.get('attributes', {}).get('tags')}")
    else:
        check("log_message found", False, "NOT FOUND")

    fn = find_function(entries, "abort_now")
    if fn:
        check("abort_now attr_noreturn",
              "attr_noreturn" in fn.get("attributes", {}).get("tags", []),
              f"attr_tags={fn.get('attributes', {}).get('tags')}")
    else:
        check("abort_now found", False, "NOT FOUND")

    fn = find_function(entries, "square")
    if fn:
        check("square attr_const",
              "attr_const" in fn.get("attributes", {}).get("tags", []),
              f"attr_tags={fn.get('attributes', {}).get('tags')}")
    else:
        check("square found", False, "NOT FOUND")


def main():
    headers = [
        ("test_primitives.h", validate_primitives),
        ("test_pointers.h", validate_pointers),
        ("test_arrays.h", validate_arrays),
        ("test_structs.h", validate_structs),
        ("test_functions.h", validate_functions),
        ("test_attributes.h", validate_attributes),
    ]

    for header_name, validator in headers:
        path = TEST_DIR / header_name
        if not path.exists():
            print(f"\nSKIP  {header_name} -- not found")
            continue
        entries = scan_header(str(path))
        validator(entries)

    print(f"\n{'='*50}")
    print(f"TOTAL: {PASS} passed, {FAIL} failed")
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
