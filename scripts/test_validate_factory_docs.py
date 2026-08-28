import importlib.util
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_factory_docs", HERE / "scripts" / "validate_factory_docs.py"
)
validator = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(validator)

SHA = "0123456789abcdef0123456789abcdef01234567"


def make_tree(index: str, records: dict[str, str]) -> Path:
    root = Path(tempfile.mkdtemp())
    directory = root / "docs/factory/runs/WIL-1"
    directory.mkdir(parents=True)
    (directory / "INDEX.md").write_text(index, encoding="utf-8")
    for name, body in records.items():
        (directory / name).write_text(body, encoding="utf-8")
    return root


def row(run_id: str, sha: str = SHA, state: str = "allocated") -> str:
    return (
        f"| {run_id} | {sha} | {run_id[:16]} | "
        f"docs/factory/runs/WIL-1/{run_id}.md | {state} |"
    )


def record(run_id: str, sha: str = SHA, state: str = "allocated") -> str:
    return (
        f"- `run_id`: `{run_id}`\n"
        f"- `started_utc`: `{run_id[:16]}`\n"
        f"- `allocation_state`: `{state}`\n"
        f"- `exact_head_sha`: `{sha}`\n"
    )


def index(rows: list[str]) -> str:
    return (
        "| run_id | exact_head_sha | started_utc | record_path | "
        "allocation_state |\n"
        "|---|---|---|---|---|\n"
        + "\n".join(rows)
        + "\n"
    )


class ValidatorAdversarialTests(unittest.TestCase):
    def test_duplicate_and_unknown_index_rows_are_rejected(self) -> None:
        present = "20260828T120000Z-0123456-01"
        missing = "20260828T120000Z-0123456-02"
        root = make_tree(
            index([row(present), row(present), row(missing)]),
            {present + ".md": record(present)},
        )
        errors = validator.validate(root)
        self.assertTrue(any("duplicate" in error for error in errors))
        self.assertTrue(any("unknown index row" in error for error in errors))

    def test_gap_bad_state_and_short_sha_are_rejected(self) -> None:
        first = "20260828T120000Z-0123456-02"
        second = "20260828T120000Z-0123456-03"
        bad_sha = "f" + SHA[1:]
        root = make_tree(
            index(
                [
                    row(first, state="bogus"),
                    row(second, sha=bad_sha),
                ]
            ),
            {
                first + ".md": record(first),
                second + ".md": record(second, sha=bad_sha),
            },
        )
        errors = validator.validate(root)
        self.assertTrue(any("invalid allocation_state" in error for error in errors))
        self.assertTrue(any("sequences" in error for error in errors))
        self.assertTrue(any("short SHA" in error for error in errors))

    def test_malformed_table_and_orphan_record_are_rejected(self) -> None:
        run_id = "20260828T120000Z-0123456-01"
        root = make_tree(
            "| run_id | wrong |\n|---|---|\n",
            {run_id + ".md": record(run_id)},
        )
        errors = validator.validate(root)
        self.assertTrue(any("canonical table" in error for error in errors))

    def test_bidirectional_field_mismatch_is_rejected(self) -> None:
        run_id = "20260828T120000Z-0123456-01"
        root = make_tree(
            index([row(run_id, state="allocated")]),
            {run_id + ".md": record(run_id, state="superseded")},
        )
        errors = validator.validate(root)
        self.assertTrue(any("INDEX.md fields disagree" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
