import sys
from collections import Counter, defaultdict

import clang.cindex as ci

from .config import KNOWN_SKIP_KINDS, CHILD_HANDLED_KINDS
from .helpers import relative_path


class Coverage:
    def __init__(self):
        self.cursor_kinds: Counter = Counter()
        self.type_kinds: Counter = Counter()
        self.attr_kinds: Counter = Counter()
        self.handled_cursor_kinds: set[str] = set()
        self.skipped_cursor_kinds: set[str] = set(KNOWN_SKIP_KINDS)
        self.child_cursor_kinds: set[str] = set(CHILD_HANDLED_KINDS)
        self.unhandled_examples: dict[str, list] = defaultdict(list)

    def count_cursor(self, kind_name: str):
        self.cursor_kinds[kind_name] += 1

    def count_type(self, t):
        if t is not None and t.kind != ci.TypeKind.INVALID:
            self.type_kinds[t.kind.name] += 1

    def count_attr(self, kind_name: str):
        self.attr_kinds[kind_name] += 1

    def mark_handled(self, kind_name: str):
        self.handled_cursor_kinds.add(kind_name)

    def log_unhandled(self, kind_name: str, filepath: str, line: int, source: str):
        if len(self.unhandled_examples[kind_name]) < 3:
            self.unhandled_examples[kind_name].append(
                {"file": relative_path(filepath), "line": line, "source": source[:200]}
            )

    def report(self) -> dict:
        cursor_report = {}
        for kind_name, count in self.cursor_kinds.most_common():
            if kind_name in self.handled_cursor_kinds:
                status = "HANDLED"
            elif kind_name in self.child_cursor_kinds:
                status = "HANDLED_AS_CHILD"
            elif kind_name in self.skipped_cursor_kinds:
                status = "SKIPPED"
            elif kind_name.endswith("_ATTR"):
                status = "HANDLED_AS_ATTR"
            else:
                status = "UNHANDLED"
            entry = {"count": count, "status": status}
            if status == "UNHANDLED" and kind_name in self.unhandled_examples:
                entry["examples"] = self.unhandled_examples[kind_name]
            cursor_report[kind_name] = entry

        type_report = {}
        for kind_name, count in self.type_kinds.most_common():
            type_report[kind_name] = {"count": count}

        attr_report = {}
        for kind_name, count in self.attr_kinds.most_common():
            attr_report[kind_name] = {"count": count}

        handled = sum(1 for v in cursor_report.values() if v["status"] in ("HANDLED", "HANDLED_AS_ATTR"))
        child = sum(1 for v in cursor_report.values() if v["status"] == "HANDLED_AS_CHILD")
        skipped = sum(1 for v in cursor_report.values() if v["status"] == "SKIPPED")
        unhandled = sum(1 for v in cursor_report.values() if v["status"] == "UNHANDLED")

        return {
            "cursor_kinds": cursor_report,
            "type_kinds": type_report,
            "attr_kinds": attr_report,
            "summary": {
                "total_cursor_kinds": len(cursor_report),
                "handled": handled,
                "handled_as_child": child,
                "skipped": skipped,
                "unhandled": unhandled,
            },
        }

    def print_stderr(self):
        r = self.report()
        print("\n=== AST COVERAGE REPORT ===\n", file=sys.stderr)
        print(f"CursorKinds encountered: {r['summary']['total_cursor_kinds']}", file=sys.stderr)
        for kind_name, info in sorted(r["cursor_kinds"].items(), key=lambda x: -x[1]["count"]):
            status = info["status"]
            label = f"  {kind_name:30s} {info['count']:>6d}  [{status}]"
            print(label, file=sys.stderr)
            if status == "UNHANDLED" and "examples" in info:
                for ex in info["examples"]:
                    print(f"    -> {ex['file']}:{ex['line']}: {ex['source'][:80]}", file=sys.stderr)

        print(f"\nTypeKinds encountered: {len(r['type_kinds'])}", file=sys.stderr)
        for kind_name, info in sorted(r["type_kinds"].items(), key=lambda x: -x[1]["count"]):
            print(f"  {kind_name:30s} {info['count']:>6d}", file=sys.stderr)

        if r["attr_kinds"]:
            print(f"\nAttribute kinds encountered: {len(r['attr_kinds'])}", file=sys.stderr)
            for kind_name, info in sorted(r["attr_kinds"].items(), key=lambda x: -x[1]["count"]):
                print(f"  {kind_name:30s} {info['count']:>6d}", file=sys.stderr)

        s = r["summary"]
        print(f"\nSUMMARY: {s['handled']} handled, {s['handled_as_child']} child, "
              f"{s['skipped']} skipped, {s['unhandled']} UNHANDLED", file=sys.stderr)
