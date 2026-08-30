#!/usr/bin/env python3
"""Validate ADB-captured evidence after a single instrumentation probe."""

import json
import pathlib
import re
import struct
import sys
import xml.etree.ElementTree as element_tree
import zlib

TARGET_APPLICATION_ID = "com.rich.rallypacenotes"
IDENTITY_KEYS = {
    "capture_source",
    "captured_after_instrumentation",
    "target_package",
    "test_package",
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def validate_png(path: pathlib.Path) -> None:
    data = path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", f"invalid PNG signature: {path.name}")
    cursor = 8
    chunks: list[bytes] = []
    width = height = 0
    while cursor < len(data):
        require(cursor + 12 <= len(data), f"truncated PNG chunk: {path.name}")
        length = struct.unpack(">I", data[cursor:cursor + 4])[0]
        chunk_type = data[cursor + 4:cursor + 8]
        end = cursor + 12 + length
        require(end <= len(data), f"truncated PNG chunk data: {path.name}")
        payload = data[cursor + 8:cursor + 8 + length]
        actual_crc = struct.unpack(">I", data[cursor + 8 + length:end])[0]
        require(zlib.crc32(chunk_type + payload) & 0xFFFFFFFF == actual_crc, f"invalid PNG CRC: {path.name}")
        chunks.append(chunk_type)
        if chunk_type == b"IHDR":
            require(chunks == [b"IHDR"] and length == 13, f"invalid PNG IHDR: {path.name}")
            width, height = struct.unpack(">II", payload[:8])
            require(width > 0 and height > 0, f"invalid PNG dimensions: {path.name}")
        if chunk_type == b"IEND":
            require(length == 0 and end == len(data), f"invalid PNG end: {path.name}")
            break
        cursor = end
    require(bool(chunks) and chunks[0] == b"IHDR" and b"IDAT" in chunks and chunks[-1] == b"IEND", f"incomplete PNG: {path.name}")


def parse_identity(path: pathlib.Path) -> dict[str, str]:
    fields: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        key, separator, value = line.partition("=")
        require(separator == "=" and key in IDENTITY_KEYS and bool(value), "invalid probe identity record")
        require(key not in fields, f"duplicate probe identity field: {key}")
        fields[key] = value
    require(set(fields) == IDENTITY_KEYS, "probe identity record has missing or unknown fields")
    return fields


def validate(root: pathlib.Path, test_application_id: str) -> dict[str, object]:
    screenshot = root / "probe.png"
    hierarchy = root / "app-window.xml"
    identity = root / "identity.txt"
    foreground = root / "foreground-window.txt"
    pid = root / "target-pid.txt"
    for path in (screenshot, hierarchy, identity, foreground, pid):
        require(path.is_file() and path.stat().st_size > 0, f"missing or empty probe file: {path.name}")
    validate_png(screenshot)
    tree = element_tree.parse(hierarchy)
    packages = {node.attrib.get("package") for node in tree.iter()}
    require(TARGET_APPLICATION_ID in packages, "probe UI XML has no target-package window node")
    foreground_text = foreground.read_text(encoding="utf-8")
    foreground_pattern = re.compile(
        r"^(?:mCurrentFocus|mResumedActivity|topResumedActivity|ResumedActivity)\s*[:=]\s*"
        r"(?:ActivityRecord\{(?:[0-9a-fA-F]+\s+)?|Window\{[^\s}]+\s+)u\d+\s+"
        r"(?:com\.rich\.rallypacenotes/\.MainActivity|com\.rich\.rallypacenotes/com\.rich\.rallypacenotes\.MainActivity)(?=\s|})"
    )
    require(
        any(foreground_pattern.search(line) is not None for line in foreground_text.splitlines()),
        "probe foreground window did not identify target MainActivity",
    )
    require(pid.read_text(encoding="utf-8").strip().isdigit(), "target process PID was not observed")
    fields = parse_identity(identity)
    require(fields["test_package"] == test_application_id, "probe identity did not record the test package")
    require(fields["target_package"] == TARGET_APPLICATION_ID, "probe identity did not record the target package")
    require(fields["capture_source"] == "adb-post-instrumentation", "probe did not use ADB post-instrumentation capture")
    require(fields["captured_after_instrumentation"] == "true", "probe was not marked post-instrumentation")
    return {**fields, "screenshot_bytes": screenshot.stat().st_size, "ui_xml_bytes": hierarchy.stat().st_size}


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: validate_storage_probe.py PROBE_DIR TEST_APPLICATION_ID")
    print(json.dumps(validate(pathlib.Path(sys.argv[1]), sys.argv[2]), sort_keys=True))


if __name__ == "__main__":
    main()
