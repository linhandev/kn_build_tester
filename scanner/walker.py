import clang.cindex as ci

from .config import KNOWN_SKIP_KINDS, CHILD_HANDLED_KINDS
from .coverage import Coverage
from .helpers import FileCache, is_in_sysroot
from .scanners import (
    scan_function, scan_record, scan_enum,
    scan_typedef, scan_var, scan_macro,
)


def walk(cursor, entries: list, fc: FileCache, atg: set, cov: Coverage,
         depth=0, allow_paths=frozenset()):
    kn = cursor.kind.name

    if cursor.kind == ci.CursorKind.TRANSLATION_UNIT:
        cov.count_cursor(kn)
        cov.mark_handled("TRANSLATION_UNIT")
        for child in cursor.get_children():
            walk(child, entries, fc, atg, cov, depth + 1,
                 allow_paths=allow_paths)
        return

    loc = cursor.location
    filepath = str(loc.file) if loc.file else ""
    if filepath and not is_in_sysroot(filepath):
        if not any(filepath.startswith(p) for p in allow_paths):
            return

    cov.count_cursor(kn)

    if cursor.kind in (ci.CursorKind.LINKAGE_SPEC, ci.CursorKind.NAMESPACE):
        cov.mark_handled(kn)
        for child in cursor.get_children():
            walk(child, entries, fc, atg, cov, depth + 1,
                 allow_paths=allow_paths)
        return

    if cursor.kind == ci.CursorKind.FUNCTION_DECL:
        cov.mark_handled("FUNCTION_DECL")
        entries.append(scan_function(cursor, fc, atg, cov))
    elif cursor.kind in (ci.CursorKind.STRUCT_DECL, ci.CursorKind.UNION_DECL):
        cov.mark_handled(kn)
        entries.append(scan_record(cursor, fc, atg, cov))
    elif cursor.kind == ci.CursorKind.ENUM_DECL:
        cov.mark_handled("ENUM_DECL")
        entries.append(scan_enum(cursor, fc, atg, cov))
    elif cursor.kind in (ci.CursorKind.TYPEDEF_DECL, ci.CursorKind.TYPE_ALIAS_DECL):
        cov.mark_handled(kn)
        entries.append(scan_typedef(cursor, fc, atg, cov))
    elif cursor.kind == ci.CursorKind.VAR_DECL:
        cov.mark_handled("VAR_DECL")
        entries.append(scan_var(cursor, fc, atg, cov))
    elif cursor.kind == ci.CursorKind.MACRO_DEFINITION:
        cov.mark_handled("MACRO_DEFINITION")
        entries.append(scan_macro(cursor, fc, atg, cov))
    elif kn in KNOWN_SKIP_KINDS or kn in CHILD_HANDLED_KINDS:
        pass
    elif cursor.kind.is_attribute():
        cov.count_attr(kn)
    else:
        source = fc.source_text(cursor, limit=200)
        cov.log_unhandled(kn, filepath, loc.line if loc.file else 0, source)
