#!/usr/bin/env python3
"""Unit tests for the CI PNG validator."""

from __future__ import annotations

import importlib.util
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("validate-png.py")
SPEC = importlib.util.spec_from_file_location("validate_png", MODULE_PATH)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


def chunk(kind: bytes, payload: bytes) -> bytes:
    crc = zlib.crc32(kind)
    crc = zlib.crc32(payload, crc) & 0xFFFFFFFF
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", crc)


def minimal_png(width: int = 100, height: int = 100) -> bytes:
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    # One RGBA scanline with filter byte, repeated for every row.
    image = (b"\x00" + b"\x00\x00\x00\xff" * width) * height
    return (
        MODULE.PNG_SIGNATURE
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(image))
        + chunk(b"IEND", b"")
    )


class ValidatePngTest(unittest.TestCase):
    def validate(self, data: bytes) -> None:
        with tempfile.NamedTemporaryFile() as file:
            file.write(data)
            file.flush()
            MODULE.validate_png(Path(file.name))

    def test_accepts_complete_png(self) -> None:
        self.validate(minimal_png())

    def test_rejects_signature_and_fabricated_dimensions_only(self) -> None:
        fake = MODULE.PNG_SIGNATURE + b"\x00" * 8 + struct.pack(">II", 1440, 3120) + b"\x00" * 40
        with self.assertRaises(ValueError):
            self.validate(fake)

    def test_rejects_bad_crc(self) -> None:
        data = bytearray(minimal_png())
        data[-5] ^= 1
        with self.assertRaises(ValueError):
            self.validate(bytes(data))

    def test_rejects_missing_iend(self) -> None:
        with self.assertRaises(ValueError):
            self.validate(minimal_png()[:-12])

    def test_rejects_small_image(self) -> None:
        with self.assertRaises(ValueError):
            self.validate(minimal_png(10, 10))


if __name__ == "__main__":
    unittest.main()
