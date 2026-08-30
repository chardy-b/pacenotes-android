#!/usr/bin/env python3
"""Reject blank hosted-map PNGs using the unobscured upper map area."""
import json
import struct
import sys
import zlib
from collections import Counter
from pathlib import Path


def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c


def png_rows(path: Path) -> tuple[int, int, int, list[bytes]]:
    raw = path.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"not a PNG: {path}")
    pos, width, height, bit_depth, color_type, interlace = 8, None, None, None, None, None
    chunks = []
    while pos < len(raw):
        size = struct.unpack(">I", raw[pos:pos + 4])[0]
        kind, data = raw[pos + 4:pos + 8], raw[pos + 8:pos + 8 + size]
        pos += 12 + size
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", data)
        elif kind == b"IDAT":
            chunks.append(data)
        elif kind == b"IEND":
            break
    if None in (width, height, bit_depth, color_type, interlace):
        raise ValueError(f"incomplete PNG header: {path}")
    if bit_depth != 8 or color_type not in (0, 2, 6) or interlace != 0:
        raise ValueError(f"unsupported PNG encoding: depth={bit_depth}, type={color_type}, interlace={interlace}")
    bpp = {0: 1, 2: 3, 6: 4}[color_type]
    stream, stride, prior, rows = zlib.decompress(b"".join(chunks)), width * bpp, bytearray(width * bpp), []
    offset = 0
    for _ in range(height):
        filter_type, encoded = stream[offset], bytearray(stream[offset + 1:offset + 1 + stride])
        offset += stride + 1
        if filter_type not in (0, 1, 2, 3, 4):
            raise ValueError(f"unsupported PNG filter: {filter_type}")
        decoded = bytearray(stride)
        for index, value in enumerate(encoded):
            left = decoded[index - bpp] if index >= bpp else 0
            up = prior[index]
            up_left = prior[index - bpp] if index >= bpp else 0
            decoded[index] = (value + (0 if filter_type == 0 else left if filter_type == 1 else up if filter_type == 2 else (left + up) // 2 if filter_type == 3 else paeth(left, up, up_left))) & 0xFF
        rows.append(bytes(decoded))
        prior = decoded
    return width, height, bpp, rows


def metrics(path: Path) -> dict:
    width, height, bpp, rows = png_rows(path)
    start, end = height // 12, (height * 3) // 5
    colors = Counter()
    for row in rows[start:end]:
        for index in range(0, len(row), bpp):
            colors[row[index:index + min(3, bpp)]] += 1
    pixels = width * (end - start)
    return {"path": path.name, "size": [width, height], "crop_pixels": pixels, "unique_colors": len(colors), "dominant_color_fraction": round(max(colors.values()) / pixels, 6)}


reports = [metrics(Path(arg)) for arg in sys.argv[1:]]
if not reports:
    raise SystemExit("provide at least one screenshot")
for report in reports:
    if report["unique_colors"] < 64 or report["dominant_color_fraction"] > 0.90:
        raise SystemExit(json.dumps({"rendered_basemap": False, "report": report}, sort_keys=True))
print(json.dumps({"rendered_basemap": True, "reports": reports}, sort_keys=True))
