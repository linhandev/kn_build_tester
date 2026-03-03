from pathlib import Path
from .config import INCLUDE_PREFIXES, SYSROOT_PREFIXES


class FileCache:
    def __init__(self):
        self._cache: dict[str, str] = {}

    def read(self, filepath: str) -> str:
        if filepath not in self._cache:
            try:
                self._cache[filepath] = Path(filepath).read_text(errors="replace")
            except Exception:
                self._cache[filepath] = ""
        return self._cache[filepath]

    def source_text(self, cursor, limit=2000) -> str:
        ext = cursor.extent
        if ext.start.file is None:
            return ""
        fp = str(ext.start.file)
        content = self.read(fp)
        if not content:
            return ""
        text = content[ext.start.offset : ext.end.offset]
        return text[:limit] + ("\u2026" if len(text) > limit else "")


def relative_path(filepath: str) -> str:
    for prefix in INCLUDE_PREFIXES:
        if filepath.startswith(prefix):
            return filepath[len(prefix):]
    return filepath


def sysroot_of(filepath: str) -> str:
    if "/hms/" in filepath:
        return "hms"
    if "/openharmony/" in filepath:
        return "openharmony"
    return "other"


def is_in_sysroot(filepath: str) -> bool:
    return any(filepath.startswith(p) for p in SYSROOT_PREFIXES)
