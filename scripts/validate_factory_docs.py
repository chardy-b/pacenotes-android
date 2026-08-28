#!/usr/bin/env python3
"""Validate checked-in factory documentation invariants."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FACTORY = ROOT / "docs" / "factory"
RUN_ID = re.compile(r"^(\d{8}T\d{6}Z)-([0-9a-f]{7})-(\d{2})$")
SHA = re.compile(r"^[0-9a-f]{40}$")
LINK = re.compile(r"\]\(([^)#]+)(?:#[^)]+)?\)")
STATES = {"allocated", "superseded"}


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    factory = root / "docs" / "factory"

    def fail(message: str) -> None:
        errors.append(message)

    for doc in factory.rglob("*.md"):
        text = doc.read_text(encoding="utf-8")
        for target in LINK.findall(text):
            if "://" in target or target.startswith("mailto:"):
                continue
            resolved = (doc.parent / target).resolve()
            if not resolved.exists() or (
                root not in resolved.parents and resolved != root
            ):
                fail(f"{doc.relative_to(root)}: broken relative link {target!r}")

    runs_root = factory / "runs"
    if not runs_root.exists():
        return errors

    for ticket_dir in sorted(runs_root.iterdir()):
        if not ticket_dir.is_dir():
            fail(
                f"unexpected non-directory run entry: "
                f"{ticket_dir.relative_to(root)}"
            )
            continue

        index = ticket_dir / "INDEX.md"
        records = sorted(p for p in ticket_dir.glob("*.md") if p.name != "INDEX.md")
        if records and not index.exists():
            fail(f"{ticket_dir.relative_to(root)}: records require INDEX.md")
        if not index.exists():
            continue

        rows = parse_index(index, fail, root)
        by_id: dict[str, tuple[str, str, str, str, str]] = {}
        for row in rows:
            run_id, sha, started, record_path, state = row
            if run_id in by_id:
                fail(f"{index.relative_to(root)}: duplicate run_id {run_id}")
            else:
                by_id[run_id] = row

            match = RUN_ID.fullmatch(run_id)
            if not match:
                fail(f"{index.relative_to(root)}: invalid run ID {run_id!r}")
                continue
            if not SHA.fullmatch(sha):
                fail(f"{index.relative_to(root)}: invalid exact_head_sha for {run_id}")
            if started != match.group(1):
                fail(f"{index.relative_to(root)}: started_utc disagrees with {run_id}")
            if state not in STATES:
                fail(f"{index.relative_to(root)}: invalid allocation_state {state!r}")

            expected = ticket_dir / f"{run_id}.md"
            if record_path != str(expected.relative_to(root)):
                fail(f"{index.relative_to(root)}: record_path mismatch for {run_id}")
            if not expected.is_file():
                fail(f"{index.relative_to(root)}: unknown index row {run_id}")

        groups: dict[tuple[str, str], list[int]] = {}
        for run_id, sha, _started, _record_path, _state in rows:
            match = RUN_ID.fullmatch(run_id)
            if not match:
                continue
            if SHA.fullmatch(sha) and sha.startswith(match.group(2)):
                groups.setdefault((match.group(1), sha), []).append(
                    int(match.group(3))
                )
            elif SHA.fullmatch(sha):
                fail(
                    f"{index.relative_to(root)}: short SHA does not match "
                    f"exact SHA for {run_id}"
                )

        for timestamp_sha, sequences in groups.items():
            if sorted(sequences) != list(range(1, len(sequences) + 1)):
                fail(
                    f"{index.relative_to(root)}: sequences for "
                    f"{timestamp_sha[0]} / {timestamp_sha[1]} must start at "
                    "01 and be contiguous"
                )

        actual = {p.stem: p for p in records}
        for run_id, record in sorted(actual.items()):
            if run_id not in by_id:
                fail(f"{record.relative_to(root)}: missing matching INDEX.md row")
                continue

            text = record.read_text(encoding="utf-8")
            match = RUN_ID.fullmatch(run_id)
            if not match:
                fail(f"{record.relative_to(root)}: invalid run ID")
                continue

            fields = dict(
                re.findall(r"^- `([^`]+)`: `([^`]+)`$", text, re.MULTILINE)
            )
            if fields.get("run_id") != run_id:
                fail(f"{record.relative_to(root)}: run_id field does not match filename")
            if fields.get("started_utc") != match.group(1):
                fail(f"{record.relative_to(root)}: started_utc does not match filename")

            exact_sha = fields.get("exact_head_sha", "")
            if not SHA.fullmatch(exact_sha):
                fail(
                    f"{record.relative_to(root)}: missing exact 40-character "
                    "head SHA"
                )
            elif not exact_sha.startswith(match.group(2)):
                fail(f"{record.relative_to(root)}: short head does not match exact SHA")

            state = fields.get("allocation_state")
            if state not in STATES:
                fail(f"{record.relative_to(root)}: invalid allocation_state")

            row = by_id[run_id]
            if (row[1], row[2], row[4]) != (
                exact_sha,
                fields.get("started_utc"),
                state,
            ):
                fail(f"{record.relative_to(root)}: INDEX.md fields disagree")

            if "<" in text or ">" in text:
                fail(
                    f"{record.relative_to(root)}: placeholder text is forbidden "
                    "in actual runs"
                )

    return errors


def parse_index(path: Path, fail, root: Path) -> list[tuple[str, str, str, str, str]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    header = (
        "| run_id | exact_head_sha | started_utc | record_path | "
        "allocation_state |"
    )
    positions = [i for i, line in enumerate(lines) if line.strip() == header]
    if len(positions) != 1:
        fail(
            f"{path.relative_to(root)}: INDEX.md must contain exactly one "
            "canonical table"
        )
        return []

    header_line = positions[0]
    separator = "|---|---|---|---|---|"
    if (
        header_line + 1 >= len(lines)
        or lines[header_line + 1].strip() != separator
    ):
        fail(f"{path.relative_to(root)}: malformed INDEX.md separator")
        return []

    rows: list[tuple[str, str, str, str, str]] = []
    for line in lines[header_line + 2 :]:
        if not line.strip():
            break
        parts = [part.strip() for part in line.strip().strip("|").split("|")]
        if len(parts) != 5:
            fail(f"{path.relative_to(root)}: malformed INDEX.md row")
            continue
        rows.append(tuple(parts))
    return rows


if __name__ == "__main__":
    errors = validate(ROOT)
    if errors:
        print("Factory documentation validation failed:", file=sys.stderr)
        print("\n".join(f"- {e}" for e in errors), file=sys.stderr)
        raise SystemExit(1)
    print("Factory documentation validation passed")
