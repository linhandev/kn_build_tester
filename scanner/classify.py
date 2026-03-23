import clang.cindex as ci

from .coverage import Coverage


PRIMITIVE_MAP = {
    ci.TypeKind.VOID: "void",
    ci.TypeKind.BOOL: "primitive_bool",
    ci.TypeKind.CHAR_U: "primitive_uchar", ci.TypeKind.UCHAR: "primitive_uchar",
    ci.TypeKind.CHAR_S: "primitive_char",  ci.TypeKind.SCHAR: "primitive_schar",
    ci.TypeKind.CHAR16: "primitive_char16", ci.TypeKind.CHAR32: "primitive_char32",
    ci.TypeKind.WCHAR: "primitive_wchar",
    ci.TypeKind.SHORT: "primitive_short",  ci.TypeKind.USHORT: "primitive_ushort",
    ci.TypeKind.INT: "primitive_int",      ci.TypeKind.UINT: "primitive_uint",
    ci.TypeKind.LONG: "primitive_long",    ci.TypeKind.ULONG: "primitive_ulong",
    ci.TypeKind.LONGLONG: "primitive_longlong",
    ci.TypeKind.ULONGLONG: "primitive_ulonglong",
    ci.TypeKind.FLOAT: "primitive_float",  ci.TypeKind.DOUBLE: "primitive_double",
    ci.TypeKind.LONGDOUBLE: "primitive_longdouble",
}


def _result(base_tag: str, modifier_tags: set[str], type_str: str,
            sizeof: int = -1, element_count: int = -1) -> dict:
    tags = set(modifier_tags)
    if base_tag:
        tags.add(base_tag)
    return {
        "tags": tags,
        "base_tag": base_tag,
        "modifier_tags": set(modifier_tags),
        "type_str": type_str,
        "sizeof": sizeof,
        "element_count": element_count,
    }


def _pointer_base(pointee, inner: dict) -> str:
    pk = pointee.get_canonical().kind
    if pk == ci.TypeKind.VOID:
        return "void_ptr"
    if pk in (ci.TypeKind.CHAR_S, ci.TypeKind.SCHAR,
              ci.TypeKind.CHAR_U, ci.TypeKind.UCHAR):
        return "const_char_ptr" if pointee.is_const_qualified() else "char_ptr"
    if pk == ci.TypeKind.POINTER:
        return "double_ptr"
    if pk == ci.TypeKind.FUNCTIONPROTO:
        return "function_pointer"
    if pk == ci.TypeKind.RECORD:
        decl = pointee.get_canonical().get_declaration()
        return "opaque_ptr" if (decl and not decl.is_definition()) else "struct_ptr"
    if pk == ci.TypeKind.ENUM:
        return "enum_ptr"
    if inner["base_tag"] and inner["base_tag"].startswith("primitive_"):
        return f"{inner['base_tag']}_ptr"
    return "ptr"


def classify_type(t, cov: Coverage, depth=0) -> dict:
    """Return shape-aware tags for a clang Type."""
    if depth > 10 or t is None or t.kind == ci.TypeKind.INVALID:
        return _result("", set(), "")
    cov.count_type(t)
    kind = t.kind

    if kind in PRIMITIVE_MAP:
        base_tag = PRIMITIVE_MAP[kind]
        modifier_tags: set[str] = set()
    elif kind == ci.TypeKind.COMPLEX:
        base_tag = "complex"
        modifier_tags = set()
    elif kind == ci.TypeKind.POINTER:
        pointee = t.get_pointee()
        inner = classify_type(pointee, cov, depth + 1)
        base_tag = _pointer_base(pointee, inner)
        modifier_tags = set()
        if "typedef_ref" in inner["tags"]:
            modifier_tags.add("typedef_ptr")
        if pointee.is_const_qualified() and base_tag != "const_char_ptr":
            modifier_tags.add("ptr_to_const")
        if t.is_const_qualified():
            modifier_tags.add("const_ptr")
        if pointee.is_volatile_qualified():
            modifier_tags.add("ptr_to_volatile")
    elif kind == ci.TypeKind.CONSTANTARRAY:
        base_tag = "fixed_array"
        modifier_tags = set()
        inner = classify_type(t.element_type, cov, depth + 1)
        if inner["base_tag"]:
            modifier_tags.add(f"fixed_array_of_{inner['base_tag']}")
        if "typedef_ref" in inner["tags"]:
            modifier_tags.add("fixed_array_of_typedef")
        if inner["base_tag"] == "fixed_array":
            modifier_tags.add("multidim_array")
        try:
            element_count = t.element_count
        except Exception:
            element_count = -1
        try:
            sizeof = t.get_size()
        except Exception:
            sizeof = -1
        if t.is_const_qualified():
            modifier_tags.add("const_qualified")
        if t.is_volatile_qualified():
            modifier_tags.add("volatile_qualified")
        return _result(base_tag, modifier_tags, t.spelling,
                       sizeof=sizeof, element_count=element_count)
    elif kind == ci.TypeKind.INCOMPLETEARRAY:
        base_tag = "incomplete_array"
        modifier_tags = set()
        inner = classify_type(t.element_type, cov, depth + 1)
        if inner["base_tag"]:
            modifier_tags.add(f"incomplete_array_of_{inner['base_tag']}")
        if "typedef_ref" in inner["tags"]:
            modifier_tags.add("incomplete_array_of_typedef")
    elif kind == ci.TypeKind.VARIABLEARRAY:
        base_tag = "variable_length_array"
        modifier_tags = set()
        inner = classify_type(t.element_type, cov, depth + 1)
        if inner["base_tag"]:
            modifier_tags.add(f"variable_length_array_of_{inner['base_tag']}")
        if "typedef_ref" in inner["tags"]:
            modifier_tags.add("variable_length_array_of_typedef")
    elif kind == ci.TypeKind.RECORD:
        decl = t.get_declaration()
        base_tag = "opaque_record" if (decl and not decl.is_definition()) else "record"
        modifier_tags = set()
        try:
            sizeof = t.get_size()
        except Exception:
            sizeof = -1
        if t.is_const_qualified():
            modifier_tags.add("const_qualified")
        if t.is_volatile_qualified():
            modifier_tags.add("volatile_qualified")
        return _result(base_tag, modifier_tags, t.spelling, sizeof=sizeof)
    elif kind == ci.TypeKind.ENUM:
        base_tag = "enum"
        modifier_tags = set()
    elif kind == ci.TypeKind.TYPEDEF:
        canon = t.get_canonical()
        if canon.kind != ci.TypeKind.INVALID:
            inner = classify_type(canon, cov, depth + 1)
            base_tag = inner["base_tag"] or "typedef_ref"
            modifier_tags = set(inner["modifier_tags"])
            modifier_tags.add("typedef_ref")
        else:
            base_tag = "typedef_ref"
            modifier_tags = set()
    elif kind == ci.TypeKind.ELABORATED:
        named = t.get_named_type()
        if named.kind != ci.TypeKind.INVALID:
            inner = classify_type(named, cov, depth + 1)
            base_tag = inner["base_tag"]
            modifier_tags = set(inner["modifier_tags"])
        else:
            base_tag = ""
            modifier_tags = set()
    elif kind == ci.TypeKind.FUNCTIONPROTO:
        base_tag = "function_proto"
        modifier_tags = set()
    elif kind == ci.TypeKind.FUNCTIONNOPROTO:
        base_tag = "function_noproto"
        modifier_tags = set()
    elif kind == ci.TypeKind.UNEXPOSED:
        base_tag = "unexposed_type"
        modifier_tags = set()
    else:
        base_tag = f"type_{kind.name}"
        modifier_tags = set()

    if t.is_const_qualified():
        modifier_tags.add("const_qualified")
    if t.is_volatile_qualified():
        modifier_tags.add("volatile_qualified")
    return _result(base_tag, modifier_tags, t.spelling)
