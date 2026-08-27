#!/usr/bin/env python3
"""Validate checked-in factory documentation invariants."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FACTORY = ROOT / "docs" / "factory"
RUN_ID = re.compile(r"^(\d{8}T\d{6}Z)-([0-9a-f]{7})-(\d{2})$")
LINK = re.compile(r"\]\(([^)#]+)(?:#[^)]+)?\)")

errors: list[str] = []

def fail(message: str) -> None:
    errors.append(message)

for doc in FACTORY.rglob("*.md"):
    text = doc.read_text(encoding="utf-8")
    for target in LINK.findall(text):
        if "://" in target or target.startswith("mailto:"):
            continue
        resolved = (doc.parent / target).resolve()
        if not resolved.exists() or ROOT not in resolved.parents and resolved != ROOT:
            fail(f"{doc.relative_to(ROOT)}: broken relative link {target!r}")

runs_root = FACTORY / "runs"
if runs_root.exists():
    for ticket_dir in runs_root.iterdir():
        if not ticket_dir.is_dir():
            fail(f"unexpected non-directory run entry: {ticket_dir.relative_to(ROOT)}")
            continue
        index = ticket_dir / "INDEX.md"
        records = sorted(p for p in ticket_dir.glob("*.md") if p.name != "INDEX.md")
        if records and not index.exists():
            fail(f"{ticket_dir.relative_to(ROOT)}: records require INDEX.md")
        index_text = index.read_text(encoding="utf-8") if index.exists() else ""
        seen: set[str] = set()
        for record in records:
            match = RUN_ID.fullmatch(record.stem)
            if not match:
                fail(f"{record.relative_to(ROOT)}: invalid run ID")
                continue
            run_id = record.stem
            if run_id in seen:
                fail(f"duplicate run ID {run_id}")
            seen.add(run_id)
            text = record.read_text(encoding="utf-8")
            if "<" in text or ">" in text:
                fail(f"{record.relative_to(ROOT)}: placeholder text is forbidden in actual runs")
            if f"`run_id`: `{run_id}`" not in text:
                fail(f"{record.relative_to(ROOT)}: run_id field does not match filename")
            short_sha = match.group(2)
            sha_match = re.search(r"`exact_head_sha`: `([0-9a-f]{40})`", text)
            if not sha_match:
                fail(f"{record.relative_to(ROOT)}: missing exact 40-character head SHA")
            elif not sha_match.group(1).startswith(short_sha):
                fail(f"{record.relative_to(ROOT)}: short head does not match exact SHA")
            if run_id not in index_text or record.name not in index_text:
                fail(f"{record.relative_to(ROOT)}: missing matching INDEX.md row")

if errors:
    print("Factory documentation validation failed:", file=sys.stderr)
    print("\n".join(f"- {e}" for e in errors), file=sys.stderr)
    raise SystemExit(1)
print("Factory documentation validation passed")
