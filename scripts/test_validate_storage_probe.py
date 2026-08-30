import importlib.util
import struct
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_storage_probe", HERE / ".github" / "device-evidence" / "validate_storage_probe.py"
)
validator = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(validator)

TEST_APPLICATION_ID = "com.rich.rallypacenotes.test"
EXPECTED_DIR = "/storage/emulated/0/Android/data/com.rich.rallypacenotes.test/files/factory-evidence/wil-70-probe"


def make_probe(identity: str | None = None, xml: str = '<hierarchy package="com.rich.rallypacenotes"/>') -> Path:
    root = Path(tempfile.mkdtemp())
    (root / "probe.png").write_bytes(
        b"\x89PNG\r\n\x1a\n" + b"\x00" * 8 + struct.pack(">II", 1, 1)
    )
    (root / "app-window.xml").write_text(xml, encoding="utf-8")
    (root / "identity.txt").write_text(
        identity
        or f"test_package={TEST_APPLICATION_ID}\ntarget_package=com.rich.rallypacenotes\noutput_dir={EXPECTED_DIR}\n",
        encoding="utf-8",
    )
    return root


class StorageProbeValidatorTests(unittest.TestCase):
    def test_accepts_complete_test_owned_export(self) -> None:
        result = validator.validate(make_probe(), TEST_APPLICATION_ID, EXPECTED_DIR)
        self.assertEqual(result["output_dir"], EXPECTED_DIR)

    def test_rejects_other_external_storage_path(self) -> None:
        root = make_probe(
            f"test_package={TEST_APPLICATION_ID}\ntarget_package=com.rich.rallypacenotes\noutput_dir=/storage/emulated/0/Download/probe\n"
        )
        with self.assertRaisesRegex(SystemExit, "expected test-owned output path"):
            validator.validate(root, TEST_APPLICATION_ID, EXPECTED_DIR)

    def test_rejects_duplicate_or_missing_identity_fields(self) -> None:
        root = make_probe(f"test_package={TEST_APPLICATION_ID}\ntest_package={TEST_APPLICATION_ID}\n")
        with self.assertRaisesRegex(SystemExit, "duplicate probe identity field"):
            validator.validate(root, TEST_APPLICATION_ID, EXPECTED_DIR)

    def test_rejects_xml_without_target_app(self) -> None:
        with self.assertRaisesRegex(SystemExit, "does not identify the target app"):
            validator.validate(make_probe(xml='<hierarchy package="com.android.launcher"/>'), TEST_APPLICATION_ID, EXPECTED_DIR)

    def test_rejects_truncated_png(self) -> None:
        root = make_probe()
        (root / "probe.png").write_bytes(b"\x89PNG\r\n\x1a\n")
        with self.assertRaisesRegex(SystemExit, "truncated PNG header"):
            validator.validate(root, TEST_APPLICATION_ID, EXPECTED_DIR)


if __name__ == "__main__":
    unittest.main()
