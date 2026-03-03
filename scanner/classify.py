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


def classify_type(t, cov: Coverage, depth=0) -> dict:
    """Return {"tags": set[str], "type_str": str} for a clang Type."""
    if depth > 10 or t is None or t.kind == ci.TypeKind.INVALID:
        return {"tags": set(), "type_str": ""}
    cov.count_type(t)
    tags: set[str] = set()
    kind = t.kind

    if kind in PRIMITIVE_MAP:
        tags.add(PRIMITIVE_MAP[kind])
    elif kind == ci.TypeKind.COMPLEX:
        tags.add("complex")
    elif kind == ci.TypeKind.POINTER:
        pointee = t.get_pointee()
        classify_type(pointee, cov, depth + 1)
        pk = pointee.get_canonical().kind
        if pk == ci.TypeKind.VOID:
            tags.add("void_ptr")
        elif pk in (ci.TypeKind.CHAR_S, ci.TypeKind.SCHAR,
                    ci.TypeKind.CHAR_U, ci.TypeKind.UCHAR):
            tags.add("const_char_ptr" if pointee.is_const_qualified() else "char_ptr")
        elif pk == ci.TypeKind.POINTER:
            tags.add("double_ptr")
        elif pk == ci.TypeKind.FUNCTIONPROTO:
            tags.add("function_pointer")
        elif pk == ci.TypeKind.RECORD:
            decl = pointee.get_canonical().get_declaration()
            tags.add("opaque_ptr" if (decl and not decl.is_definition()) else "struct_ptr")
        else:
            tags.add("ptr")
        if pointee.is_const_qualified():
            tags.add("ptr_to_const")
        if t.is_const_qualified():
            tags.add("const_ptr")
        if pointee.is_volatile_qualified():
            tags.add("ptr_to_volatile")
    elif kind == ci.TypeKind.CONSTANTARRAY:
        tags.add("fixed_array")
        inner = classify_type(t.element_type, cov, depth + 1)
        if "fixed_array" in inner["tags"]:
            tags.add("multidim_array")
    elif kind == ci.TypeKind.INCOMPLETEARRAY:
        tags.add("incomplete_array")
    elif kind == ci.TypeKind.VARIABLEARRAY:
        tags.add("variable_length_array")
    elif kind == ci.TypeKind.RECORD:
        decl = t.get_declaration()
        tags.add("opaque_record" if (decl and not decl.is_definition()) else "record")
    elif kind == ci.TypeKind.ENUM:
        tags.add("enum")
    elif kind == ci.TypeKind.TYPEDEF:
        tags.add("typedef_ref")
        canon = t.get_canonical()
        if canon.kind != ci.TypeKind.INVALID:
            tags.update(classify_type(canon, cov, depth + 1)["tags"])
    elif kind == ci.TypeKind.ELABORATED:
        named = t.get_named_type()
        if named.kind != ci.TypeKind.INVALID:
            tags.update(classify_type(named, cov, depth + 1)["tags"])
    elif kind == ci.TypeKind.FUNCTIONPROTO:
        tags.add("function_proto")
    elif kind == ci.TypeKind.FUNCTIONNOPROTO:
        tags.add("function_noproto")
    elif kind == ci.TypeKind.UNEXPOSED:
        tags.add("unexposed_type")
    else:
        tags.add(f"type_{kind.name}")

    if t.is_const_qualified():
        tags.add("const_qualified")
    if t.is_volatile_qualified():
        tags.add("volatile_qualified")
    return {"tags": tags, "type_str": t.spelling}
