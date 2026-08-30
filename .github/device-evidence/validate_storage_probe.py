#!/usr/bin/env python3
"""Validate one post-instrumentation, test-owned evidence export."""

import json
import pathlib
import struct
import sys
import xml.etree.ElementTree as element_tree

TARGET_APPLICATION_ID = "com.rich.rallypacenotes"
IDENTITY_KEYS = {"test_package", "target_package", "output_dir"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def validate_png(path: pathlib.Path) -> None:
    data = path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", f"invalid PNG signature: {path.name}")
    require(len(data) >= 24, f"truncated PNG header: {path.name}")
    width, height = struct.unpack(">II", data[16:24])
    require(width > 0 and height > 0, f"invalid PNG dimensions: {path.name}")


def parse_identity(path: pathlib.Path) -> dict[str, str]:
    fields: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        key, separator, value = line.partition("=")
        require(separator == "=" and key in IDENTITY_KEYS and bool(value), "invalid probe identity record")
        require(key not in fields, f"duplicate probe identity field: {key}")
        fields[key] = value
    require(set(fields) == IDENTITY_KEYS, "probe identity record has missing or unknown fields")
    return fields


def validate(root: pathlib.Path, test_application_id: str, expected_device_dir: str) -> dict[str, object]:
    screenshot = root / "probe.png"
    hierarchy = root / "app-window.xml"
    identity = root / "identity.txt"
    for path in (screenshot, hierarchy, identity):
        require(path.is_file() and path.stat().st_size > 0, f"missing or empty probe file: {path.name}")
    validate_png(screenshot)
    element_tree.parse(hierarchy)
    require(TARGET_APPLICATION_ID in hierarchy.read_text(encoding="utf-8"), "probe UI XML does not identify the target app")
    fields = parse_identity(identity)
    require(fields["test_package"] == test_application_id, "probe identity did not record the test package")
    require(fields["target_package"] == TARGET_APPLICATION_ID, "probe identity did not record the target package")
    require(fields["output_dir"] == expected_device_dir, "probe identity did not record the expected test-owned output path")
    return {
        "test_package": fields["test_package"],
        "target_package": fields["target_package"],
        "output_dir": fields["output_dir"],
        "screenshot_bytes": screenshot.stat().st_size,
        "ui_xml_bytes": hierarchy.stat().st_size,
    }


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit("usage: validate_storage_probe.py PROBE_DIR TEST_APPLICATION_ID EXPECTED_DEVICE_DIR")
    print(json.dumps(validate(pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3]), sort_keys=True))


if __name__ == "__main__":
    main()
