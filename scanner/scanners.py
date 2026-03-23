import clang.cindex as ci

from .coverage import Coverage
from .classify import classify_type
from .helpers import FileCache, relative_path, sysroot_of


METADATA_ATTR_TAGS = frozenset({
    "attr_availability",
    "attr_visibility",
    "attr_deprecated",
})

FUNCTION_GROUP_TAGS = frozenset({
    "variadic",
    "static_inline_with_body",
    "inline_non_static",
    "has_unnamed_param",
})


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


def _emit_shape_tags(prefix: str, info: dict) -> set[str]:
    tags: set[str] = set()
    if info["base_tag"]:
        tags.add(f"{prefix}_{info['base_tag']}")
    tags.update(f"{prefix}_mod_{t}" for t in sorted(info["modifier_tags"]))
    return tags


def _shape_key(info: dict) -> str:
    parts = []
    if info["base_tag"]:
        parts.append(info["base_tag"])
    parts.extend(f"mod:{tag}" for tag in sorted(info["modifier_tags"]))
    return " | ".join(parts) if parts else "(none)"


def _function_group_key(tags: set[str]) -> str:
    function_tags = sorted(t for t in tags if t in FUNCTION_GROUP_TAGS)
    return " + ".join(function_tags) if function_tags else "plain_function"


def _attribute_tag(attr_kind_name: str, spelling: str, source_lower: str) -> str:
    sl = spelling.lower()
    if "deprecated" in sl:
        return "attr_deprecated"
    if "visibility" in sl or attr_kind_name == "VISIBILITY_ATTR" or spelling.strip() == "default":
        return "attr_visibility"
    if "availability" in sl or "ohos" in sl or "introduced" in sl:
        return "attr_availability"
    if "format" in sl:
        return "attr_format"
    if "noreturn" in sl or "_noreturn" in sl:
        return "attr_noreturn"
    if "constructor" in sl:
        return "attr_constructor"
    if "packed" in sl or attr_kind_name == "PACKED_ATTR":
        return "attr_packed"
    if "aligned" in sl or attr_kind_name == "ALIGNED_ATTR":
        return "attr_aligned"
    if attr_kind_name == "CONST_ATTR":
        return "attr_const"
    if attr_kind_name == "ASM_LABEL_ATTR":
        return "attr_asm_label"
    if "__availability__" in source_lower or "introduced=" in source_lower:
        return "attr_availability"
    if "__deprecated__" in source_lower:
        return "attr_deprecated"
    if "napi_inner_extern" in source_lower or "napi_extern" in source_lower:
        return "attr_visibility"
    return f"attr_{attr_kind_name}"


def _log_unhandled_child(child, fc: FileCache, cov: Coverage):
    loc = child.location
    filepath = str(loc.file) if loc.file else ""
    cov.log_unhandled(
        child.kind.name,
        filepath,
        loc.line if loc.file else 0,
        fc.source_text(child, limit=200),
    )


def scan_function(cursor, fc: FileCache, atg, cov: Coverage):
    function_flags: set[str] = set()
    info = _loc(cursor, atg)
    source = fc.source_text(cursor)

    ret = cursor.result_type
    ri = classify_type(ret, cov)
    canon_ret = ret.get_canonical()
    ret_modifiers: set[str] = set()
    ret_sizeof = -1
    if canon_ret.kind == ci.TypeKind.RECORD:
        decl = canon_ret.get_declaration()
        is_union = decl and decl.kind == ci.CursorKind.UNION_DECL
        ri["base_tag"] = "union_by_value" if is_union else "struct_by_value"
        sz = canon_ret.get_size()
        ret_sizeof = sz
        if 0 < sz <= 16:
            ret_modifiers.add("abi_register_return")
        elif sz > 16:
            ret_modifiers.add("abi_indirect_return")

    try:
        if cursor.type.is_function_variadic():
            function_flags.add("variadic")
    except Exception:
        pass

    stripped = source.lstrip()
    if stripped.startswith("static inline") or stripped.startswith("static __inline"):
        function_flags.add("static_inline_with_body")
    elif stripped.startswith("inline "):
        function_flags.add("inline_non_static")

    params = []
    for arg in cursor.get_arguments():
        at = arg.type
        ai = classify_type(at, cov)
        pname = arg.spelling or ""
        if not pname:
            function_flags.add("has_unnamed_param")
        ac = at.get_canonical()
        param_sizeof = ai.get("sizeof", -1)
        param_element_count = ai.get("element_count", -1)
        param_group = {
            "name": pname,
            "type": at.spelling,
            "base_tag": ai["base_tag"],
            "modifier_tags": sorted(ai["modifier_tags"]),
            "sizeof": param_sizeof,
            "element_count": param_element_count,
        }
        if ac.kind == ci.TypeKind.RECORD:
            decl = ac.get_declaration()
            is_union = decl and decl.kind == ci.CursorKind.UNION_DECL
            param_group["base_tag"] = "union_by_value" if is_union else "struct_by_value"
            sz = ac.get_size()
            param_group["sizeof"] = sz
            if 0 < sz <= 16:
                param_group["modifier_tags"].append(
                    "union_by_value_small" if is_union else "struct_by_value_small"
                )
            elif sz > 16:
                param_group["modifier_tags"].append(
                    "union_by_value_large" if is_union else "struct_by_value_large"
                )
        params.append(param_group)

    attrs = []
    attr_tags: set[str] = set()
    src_lower = source.lower()
    for child in cursor.get_children():
        cov.count_cursor(child.kind.name)
        if child.kind == ci.CursorKind.PARM_DECL:
            continue
        if child.kind.is_attribute():
            cov.count_attr(child.kind.name)
            sp = child.spelling or child.displayname or ""
            if not sp or sp == child.kind.name:
                sp = fc.source_text(child, limit=300)
            attrs.append(sp)
            attr_tags.add(_attribute_tag(child.kind.name, sp, src_lower))
        else:
            _log_unhandled_child(child, fc, cov)

    # Some attributes produce UNEXPOSED_ATTR in C++ mode; detect via source
    for attr_name, patterns in (
        ("attr_noreturn", ("noreturn))", "[[noreturn]]", "_Noreturn")),
        ("attr_constructor", ("constructor))",)),
        ("attr_deprecated", ("deprecated",)),
    ):
        if attr_name not in attr_tags:
            if any(p in src_lower for p in patterns):
                attr_tags.add(attr_name)

    semantic_attr_tags = sorted(t for t in attr_tags if t not in METADATA_ATTR_TAGS)

    ri["modifier_tags"].update(ret_modifiers)

    function_group_key = _function_group_key(function_flags)
    return_group = {
        "base_tag": ri["base_tag"],
        "modifier_tags": sorted(ri["modifier_tags"]),
        "sizeof": ret_sizeof,
        "key": _shape_key(ri),
    }
    param_groups = []
    for idx, param in enumerate(params):
        param_groups.append({
            "index": idx,
            "name": param["name"],
            "base_tag": param["base_tag"],
            "modifier_tags": param["modifier_tags"],
            "key": _shape_key({
                "base_tag": param["base_tag"],
                "modifier_tags": set(param["modifier_tags"]),
            }),
        })
    full_signature_parts = [function_group_key, f"ret={return_group['key']}"]
    for pg in param_groups:
        full_signature_parts.append(f"param[{pg['index']}]={pg['key']}")
    full_signature_key = " ; ".join(full_signature_parts)

    return {
        **info,
        "kind": "function",
        "name": cursor.spelling,
        "source": source,
        "function_group_key": function_group_key,
        "function_flags": sorted(function_flags),
        "return": {
            "c_type": ret.spelling,
            "base_tag": ri["base_tag"],
            "modifier_tags": sorted(ri["modifier_tags"]),
            "sizeof": ret_sizeof,
            "key": return_group["key"],
        },
        "parameters": [
            {
                "index": pg["index"],
                "name": param["name"],
                "c_type": param["type"],
                "base_tag": pg["base_tag"],
                "modifier_tags": pg["modifier_tags"],
                "sizeof": param.get("sizeof", -1),
                "element_count": param.get("element_count", -1),
                "key": pg["key"],
            }
            for param, pg in zip(params, param_groups)
        ],
        "attributes": {
            "spellings": attrs,
            "tags": semantic_attr_tags,
        },
        "matrix_keys": {
            "function_x_return": f"{function_group_key} ; ret={return_group['key']}",
            "function_x_params": [
                f"{function_group_key} ; param={pg['key']}"
                for pg in param_groups
            ],
            "full_signature": full_signature_key,
        },
    }


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
            ftags = set()
            if fi["base_tag"]:
                ftags.add(fi["base_tag"])
            ftags.update(fi["modifier_tags"])
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
        else:
            _log_unhandled_child(child, fc, cov)

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
        else:
            _log_unhandled_child(child, fc, cov)
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
