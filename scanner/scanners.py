import clang.cindex as ci

from .coverage import Coverage
from .classify import classify_type
from .helpers import FileCache, relative_path, sysroot_of


def _is_anonymous(cursor) -> bool:
    """Clang C++ mode gives unnamed types a descriptive spelling like
    '(unnamed enum at path:line:col)' instead of an empty string."""
    name = cursor.spelling
    return not name or name.startswith("(unnamed ") or name.startswith("(anonymous ")


def _loc(cursor, addtogroup_files):
    loc = cursor.location
    filepath = str(loc.file) if loc.file else ""
    return {
        "file": relative_path(filepath),
        "line": loc.line if loc.file else 0,
        "sysroot": sysroot_of(filepath),
        "is_addtogroup_file": filepath in addtogroup_files,
    }


def scan_function(cursor, fc: FileCache, atg, cov: Coverage):
    tags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)

    ret = cursor.result_type
    ri = classify_type(ret, cov)
    canon_ret = ret.get_canonical()
    if canon_ret.kind == ci.TypeKind.VOID:
        tags.add("void_return")
    elif canon_ret.kind == ci.TypeKind.POINTER:
        tags.add("ptr_return")
        tags.update(f"ret_{t}" for t in ri["tags"])
    elif canon_ret.kind == ci.TypeKind.RECORD:
        tags.add("struct_return_by_value")
        sz = canon_ret.get_size()
        if 0 < sz <= 16:
            tags.add("struct_return_small")
        elif sz > 16:
            tags.add("struct_return_large")
    else:
        tags.update(f"ret_{t}" for t in ri["tags"])

    try:
        if cursor.type.is_function_variadic():
            tags.add("variadic")
    except Exception:
        pass

    stripped = source.lstrip()
    if stripped.startswith("static inline") or stripped.startswith("static __inline"):
        tags.add("static_inline_with_body")
    elif stripped.startswith("inline "):
        tags.add("inline_non_static")

    params = []
    for arg in cursor.get_arguments():
        at = arg.type
        ai = classify_type(at, cov)
        ptags = set(ai["tags"])
        pname = arg.spelling or ""
        if not pname:
            tags.add("has_unnamed_param")
        ac = at.get_canonical()
        if ac.kind == ci.TypeKind.RECORD:
            ptags.add("struct_by_value")
            sz = ac.get_size()
            if 0 < sz <= 16:
                ptags.add("struct_by_value_small")
            elif sz > 16:
                ptags.add("struct_by_value_large")
        tags.update(f"param_{t}" for t in ptags)
        params.append({"name": pname, "type": at.spelling, "tags": sorted(ptags)})

    attrs = []
    src_lower = source.lower()
    for child in cursor.get_children():
        cov.count_cursor(child.kind.name)
        if child.kind.is_attribute():
            cov.count_attr(child.kind.name)
            sp = child.spelling or child.displayname or ""
            if not sp or sp == child.kind.name:
                sp = fc.source_text(child, limit=300)
            attrs.append(sp)
            sl = sp.lower()
            attr_kind_name = child.kind.name
            if "deprecated" in sl:
                tags.add("attr_deprecated")
            elif "visibility" in sl or attr_kind_name == "VISIBILITY_ATTR" or sp.strip() == "default":
                tags.add("attr_visibility")
            elif "availability" in sl or "ohos" in sl or "introduced" in sl:
                tags.add("attr_availability")
            elif "format" in sl:
                tags.add("attr_format")
            elif "noreturn" in sl or "_noreturn" in sl:
                tags.add("attr_noreturn")
            elif "constructor" in sl:
                tags.add("attr_constructor")
            elif "packed" in sl or attr_kind_name == "PACKED_ATTR":
                tags.add("attr_packed")
            elif "aligned" in sl or attr_kind_name == "ALIGNED_ATTR":
                tags.add("attr_aligned")
            elif attr_kind_name == "CONST_ATTR":
                tags.add("attr_const")
            elif attr_kind_name == "ASM_LABEL_ATTR":
                tags.add("attr_asm_label")
            elif "__availability__" in src_lower or "introduced=" in src_lower:
                tags.add("attr_availability")
            elif "__deprecated__" in src_lower:
                tags.add("attr_deprecated")
            elif "NAPI_INNER_EXTERN" in source or "NAPI_EXTERN" in source:
                tags.add("attr_visibility")
            else:
                tags.add(f"attr_{attr_kind_name}")
    if attrs:
        tags.add("has_attributes")

    # Some attributes produce UNEXPOSED_ATTR in C++ mode; detect via source
    for attr_name, patterns in (
        ("attr_noreturn", ("noreturn))", "[[noreturn]]", "_Noreturn")),
        ("attr_constructor", ("constructor))",)),
        ("attr_deprecated", ("deprecated",)),
    ):
        if attr_name not in tags:
            if any(p in src_lower for p in patterns):
                tags.add(attr_name)
                tags.add("has_attributes")

    return {**info, "kind": "function", "name": cursor.spelling,
            "tags": sorted(tags), "source": source,
            "details": {"return_type": ret.spelling, "return_tags": sorted(ri["tags"]),
                        "params": params, "attributes": attrs}}


def scan_record(cursor, fc: FileCache, atg, cov: Coverage):
    is_union = cursor.kind == ci.CursorKind.UNION_DECL
    tags: set[str] = set()
    tags.add("union" if is_union else "struct")
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)

    if not cursor.is_definition():
        tags.add("opaque")
        display_name = "(anonymous)" if _is_anonymous(cursor) else cursor.spelling
        return {**info, "kind": "union" if is_union else "struct",
                "name": display_name, "tags": sorted(tags),
                "source": source, "details": {"sizeof": -1, "fields": []}}

    try:
        size = cursor.type.get_size()
    except Exception:
        size = -1
    if size >= 0:
        if size <= 16:
            tags.add("size_small")
        elif size <= 64:
            tags.add("size_medium")
        else:
            tags.add("size_large")

    fields = []
    all_fp = True
    has_any_field = False
    for child in cursor.get_children():
        cov.count_cursor(child.kind.name)
        if child.kind == ci.CursorKind.FIELD_DECL:
            has_any_field = True
            ft = child.type
            fi = classify_type(ft, cov)
            ftags = set(fi["tags"])
            if child.is_bitfield():
                tags.add("has_bitfield")
                ftags.add(f"bitfield_width_{child.get_bitfield_width()}")
            fc_ = ft.get_canonical()
            if fc_.kind == ci.TypeKind.CONSTANTARRAY:
                tags.add("has_fixed_array_field")
                if fc_.element_type.get_canonical().kind == ci.TypeKind.CONSTANTARRAY:
                    tags.add("has_multidim_array_field")
                all_fp = False
            elif fc_.kind == ci.TypeKind.INCOMPLETEARRAY:
                tags.add("has_flexible_array_member")
                all_fp = False
            elif fc_.kind == ci.TypeKind.POINTER and fc_.get_pointee().get_canonical().kind == ci.TypeKind.FUNCTIONPROTO:
                tags.add("has_function_pointer_field")
            elif fc_.kind == ci.TypeKind.FUNCTIONPROTO:
                tags.add("has_function_pointer_field")
            elif fc_.kind == ci.TypeKind.RECORD:
                decl = fc_.get_declaration()
                if decl and decl.kind == ci.CursorKind.UNION_DECL:
                    tags.add("has_union_field")
                else:
                    tags.add("has_struct_by_value_field")
                all_fp = False
            else:
                all_fp = False
            fields.append({"name": child.spelling or "(anon)", "type": ft.spelling,
                           "tags": sorted(ftags)})
        elif child.kind == ci.CursorKind.STRUCT_DECL:
            tags.add("has_nested_struct")
            if _is_anonymous(child):
                tags.add("has_anonymous_member")
        elif child.kind == ci.CursorKind.UNION_DECL:
            tags.add("has_nested_union")
            if _is_anonymous(child):
                tags.add("has_anonymous_member")
        elif child.kind.is_attribute():
            cov.count_attr(child.kind.name)
            sp = (child.spelling or child.kind.name).lower()
            if "packed" in sp:
                tags.add("attr_packed")
            elif "aligned" in sp:
                tags.add("attr_aligned")

    if has_any_field and all_fp and fields:
        tags.add("vtable_like")
    if _is_anonymous(cursor):
        tags.add("anonymous")
    for f in fields:
        if cursor.spelling and cursor.spelling in f["type"] and "*" in f["type"]:
            tags.add("self_referential")
            break
    if "#pragma pack" in source:
        tags.add("pragma_packed")
    else:
        ext = cursor.extent
        if ext.start.file:
            full = fc.read(str(ext.start.file))
            pre_start = max(0, ext.start.offset - 200)
            pre_text = full[pre_start:ext.start.offset]
            if "#pragma pack" in pre_text:
                tags.add("pragma_packed")

    display_name = "(anonymous)" if _is_anonymous(cursor) else cursor.spelling
    return {**info, "kind": "union" if is_union else "struct",
            "name": display_name, "tags": sorted(tags),
            "source": source, "details": {"sizeof": size, "fields": fields}}


def scan_enum(cursor, fc: FileCache, atg, cov: Coverage):
    tags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)
    tags.add("anonymous_enum" if _is_anonymous(cursor) else "named_enum")
    values = []
    for child in cursor.get_children():
        cov.count_cursor(child.kind.name)
        if child.kind == ci.CursorKind.ENUM_CONSTANT_DECL:
            val = child.enum_value
            values.append({"name": child.spelling, "value": val})
    if any(v["value"] < 0 for v in values):
        tags.add("has_negative_values")
    if "=" in source:
        tags.add("has_explicit_values")
    vals = [v["value"] for v in values]
    if vals and (max(vals) - min(vals)) > 2**31:
        tags.add("large_range")
    display_name = "(anonymous)" if _is_anonymous(cursor) else cursor.spelling
    return {**info, "kind": "enum", "name": display_name,
            "tags": sorted(tags), "source": source,
            "details": {"values": values, "value_count": len(values),
                        "min": min(vals, default=0), "max": max(vals, default=0)}}


def scan_typedef(cursor, fc: FileCache, atg, cov: Coverage):
    tags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)
    if cursor.kind == ci.CursorKind.TYPE_ALIAS_DECL:
        tags.add("type_alias")
    try:
        ut = cursor.underlying_typedef_type
    except Exception:
        ut = cursor.type
    classify_type(ut, cov)
    canon = ut.get_canonical()
    utk = ut.kind
    # ELABORATED wraps another type; unwrap to detect typedef chains
    if utk == ci.TypeKind.ELABORATED:
        named = ut.get_named_type()
        if named.kind == ci.TypeKind.TYPEDEF:
            utk = ci.TypeKind.TYPEDEF
    if utk == ci.TypeKind.POINTER:
        pt = ut.get_pointee()
        tags.add("to_function_pointer" if pt.get_canonical().kind == ci.TypeKind.FUNCTIONPROTO else "to_pointer")
    elif utk in (ci.TypeKind.RECORD, ci.TypeKind.ELABORATED):
        decl = canon.get_declaration()
        if decl and decl.is_definition():
            if decl.kind == ci.CursorKind.UNION_DECL:
                tags.add("to_union")
            elif decl.kind == ci.CursorKind.ENUM_DECL:
                tags.add("to_enum")
            else:
                tags.add("to_struct")
        else:
            tags.add("to_opaque")
    elif utk == ci.TypeKind.ENUM:
        tags.add("to_enum")
    elif utk == ci.TypeKind.TYPEDEF:
        tags.add("chain")
    elif utk == ci.TypeKind.FUNCTIONPROTO:
        tags.add("to_function_pointer")
    elif utk in (ci.TypeKind.CONSTANTARRAY, ci.TypeKind.INCOMPLETEARRAY):
        tags.add("to_array")
    elif utk == ci.TypeKind.VOID:
        tags.add("to_void")
    elif utk.name in ("INT", "UINT", "SHORT", "USHORT", "LONG", "ULONG",
                       "LONGLONG", "ULONGLONG", "CHAR_S", "CHAR_U", "SCHAR",
                       "UCHAR", "BOOL", "FLOAT", "DOUBLE", "LONGDOUBLE",
                       "WCHAR", "CHAR16", "CHAR32"):
        tags.add("to_primitive")
    else:
        tags.add(f"to_{utk.name.lower()}")
    return {**info, "kind": "typedef", "name": cursor.spelling,
            "tags": sorted(tags), "source": source,
            "details": {"underlying_type": ut.spelling, "canonical_type": canon.spelling}}


def scan_var(cursor, fc: FileCache, atg, cov: Coverage):
    tags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)
    vt = cursor.type
    vi = classify_type(vt, cov)
    tags.update(vi["tags"])
    try:
        sc = cursor.storage_class
        if sc == ci.StorageClass.EXTERN:
            tags.add("extern_var")
        elif sc == ci.StorageClass.STATIC:
            tags.add("static_var")
    except Exception:
        pass
    if vt.is_const_qualified():
        tags.add("const_var")
    if vt.is_volatile_qualified():
        tags.add("volatile_var")
    if "_Thread_local" in source or "thread_local" in source:
        tags.add("thread_local_var")
    return {**info, "kind": "variable", "name": cursor.spelling,
            "tags": sorted(tags), "source": source,
            "details": {"type": vt.spelling, "type_tags": sorted(vi["tags"])}}


def scan_macro(cursor, fc: FileCache, atg, cov: Coverage):
    tags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor, limit=500)
    tokens = list(cursor.get_tokens())
    if len(tokens) >= 2:
        name_tok = tokens[0]
        next_tok = tokens[1]
        if (next_tok.spelling == "("
                and next_tok.extent.start.offset == name_tok.extent.end.offset):
            tags.add("function_like_macro")
        else:
            tags.add("object_like_macro")
        body = " ".join(t.spelling for t in tokens[1:])
        if "__builtin_" in body:
            tags.add("macro_uses_builtin")
        if "__typeof" in body:
            tags.add("macro_uses_typeof")
        if "__extension__" in body:
            tags.add("macro_uses_extension")
    else:
        tags.add("empty_macro")
    return {**info, "kind": "macro", "name": cursor.spelling,
            "tags": sorted(tags), "source": source, "details": {}}
