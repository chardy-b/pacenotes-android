#!/usr/bin/env python3
"""Validate a complete PNG used as Android CI screenshot evidence."""

from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MIN_DIMENSION = 100


def validate_png(path: Path) -> None:
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("missing PNG signature")

    offset = len(PNG_SIGNATURE)
    chunk_index = 0
    width = height = None
    idat = bytearray()
    saw_iend = False

    while offset < len(data):
        if len(data) - offset < 12:
            raise ValueError("truncated PNG chunk")
        length = struct.unpack(">I", data[offset : offset + 4])[0]
        chunk_type = data[offset + 4 : offset + 8]
        chunk_end = offset + 12 + length
        if chunk_end > len(data):
            raise ValueError("truncated PNG chunk data")
        payload = data[offset + 8 : offset + 8 + length]
        expected_crc = struct.unpack(">I", data[offset + 8 + length : chunk_end])[0]
        actual_crc = zlib.crc32(chunk_type)
        actual_crc = zlib.crc32(payload, actual_crc) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            raise ValueError(f"invalid {chunk_type!r} CRC")

        if chunk_index == 0:
            if chunk_type != b"IHDR" or length != 13:
                raise ValueError("first PNG chunk is not a valid IHDR")
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", payload
            )
            if width < MIN_DIMENSION or height < MIN_DIMENSION:
                raise ValueError(f"screenshot dimensions are too small: {width}x{height}")
            if compression != 0 or filtering != 0:
                raise ValueError("unsupported PNG compression or filtering method")
            if interlace not in (0, 1):
                raise ValueError("invalid PNG interlace method")
            if bit_depth not in (1, 2, 4, 8, 16) or color_type not in (0, 2, 3, 4, 6):
                raise ValueError("invalid PNG color format")
        elif chunk_type == b"IHDR":
            raise ValueError("duplicate IHDR chunk")

        if chunk_type == b"IDAT":
            idat.extend(payload)
        elif chunk_type == b"IEND":
            if length != 0:
                raise ValueError("IEND chunk is not empty")
            if chunk_end != len(data):
                raise ValueError("data follows IEND chunk")
            saw_iend = True
            break

        offset = chunk_end
        chunk_index += 1

    if width is None or height is None:
        raise ValueError("missing IHDR chunk")
    if not idat:
        raise ValueError("missing IDAT data")
    if not saw_iend:
        raise ValueError("missing IEND chunk")
    try:
        decompressed = zlib.decompress(bytes(idat))
    except zlib.error as error:
        raise ValueError("invalid compressed PNG image data") from error
    if not decompressed:
        raise ValueError("empty decompressed PNG image data")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: validate-png.py SCREENSHOT.png")
    try:
        validate_png(Path(sys.argv[1]))
    except (OSError, ValueError) as error:
        raise SystemExit(f"invalid PNG {sys.argv[1]}: {error}") from error
