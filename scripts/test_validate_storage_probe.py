import importlib.util
import struct
import tempfile
import unittest
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("validate_storage_probe", HERE / ".github/device-evidence/validate_storage_probe.py")
assert SPEC is not None and SPEC.loader is not None
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)

TEST_PACKAGE = "com.rich.rallypacenotes.test"
TARGET_PACKAGE = "com.rich.rallypacenotes"


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def valid_png() -> bytes:
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)) + png_chunk(b"IDAT", zlib.compress(b"\x00\x00\x00\x00")) + png_chunk(b"IEND", b"")


def make_probe(identity: str | None = None, xml: str | None = None, foreground: str | None = None) -> Path:
    root = Path(tempfile.mkdtemp())
    (root / "probe.png").write_bytes(valid_png())
    (root / "app-window.xml").write_text(xml or f'<hierarchy><node package="{TARGET_PACKAGE}" /></hierarchy>', encoding="utf-8")
    (root / "foreground-window.txt").write_text(foreground or f'mCurrentFocus=Window{{x u0 {TARGET_PACKAGE}/.MainActivity}}', encoding="utf-8")
    (root / "target-pid.txt").write_text("1234\n", encoding="utf-8")
    (root / "identity.txt").write_text(identity or "capture_source=adb-post-instrumentation\ncaptured_after_instrumentation=true\ntarget_package=com.rich.rallypacenotes\ntest_package=com.rich.rallypacenotes.test\n", encoding="utf-8")
    return root


class ValidatorAdversarialTests(unittest.TestCase):
    def test_valid_probe_is_accepted(self) -> None:
        self.assertEqual(validator.validate(make_probe(), TEST_PACKAGE)["capture_source"], "adb-post-instrumentation")

    def test_non_adb_capture_source_is_rejected(self) -> None:
        root = make_probe(identity="capture_source=test-owned-storage\ncaptured_after_instrumentation=true\ntarget_package=com.rich.rallypacenotes\ntest_package=com.rich.rallypacenotes.test\n")
        with self.assertRaisesRegex(SystemExit, "ADB post-instrumentation"):
            validator.validate(root, TEST_PACKAGE)

    def test_wrong_xml_package_is_rejected(self) -> None:
        with self.assertRaisesRegex(SystemExit, "no target-package"):
            validator.validate(make_probe(xml='<hierarchy><node package="other.app" /></hierarchy>'), TEST_PACKAGE)

    def test_api_35_resumed_activity_is_accepted(self) -> None:
        root = make_probe(foreground="topResumedActivity=ActivityRecord{u0 com.rich.rallypacenotes/.MainActivity t12}")
        self.assertEqual(validator.validate(root, TEST_PACKAGE)["target_package"], TARGET_PACKAGE)

    def test_api_35_resumed_activity_label_is_accepted(self) -> None:
        root = make_probe(foreground="ResumedActivity: ActivityRecord{u0 com.rich.rallypacenotes/.MainActivity t12}")
        self.assertEqual(validator.validate(root, TEST_PACKAGE)["target_package"], TARGET_PACKAGE)

    def test_foreground_target_text_outside_component_is_rejected(self) -> None:
        foreground = "mResumedActivity=ActivityRecord{u0 other.app/.Home t12 com.rich.rallypacenotes/.MainActivity}"
        with self.assertRaisesRegex(SystemExit, "foreground window"):
            validator.validate(make_probe(foreground=foreground), TEST_PACKAGE)

    def test_foreground_wrong_component_before_target_tuple_is_rejected(self) -> None:
        foreground = "mResumedActivity=ActivityRecord{other.app/.Home u0 com.rich.rallypacenotes/.MainActivity t12}"
        with self.assertRaisesRegex(SystemExit, "foreground window"):
            validator.validate(make_probe(foreground=foreground), TEST_PACKAGE)

    def test_foreground_component_suffix_is_rejected(self) -> None:
        foreground = "mResumedActivity=ActivityRecord{u0 com.rich.rallypacenotes/.MainActivityEvil t12}"
        with self.assertRaisesRegex(SystemExit, "foreground window"):
            validator.validate(make_probe(foreground=foreground), TEST_PACKAGE)

    def test_wrong_foreground_activity_is_rejected(self) -> None:
        with self.assertRaisesRegex(SystemExit, "foreground window"):
            validator.validate(make_probe(foreground='mCurrentFocus=Window{u0 other.app/.Home}'), TEST_PACKAGE)

    def test_corrupt_png_is_rejected(self) -> None:
        root = make_probe()
        (root / "probe.png").write_bytes(valid_png()[:-1] + b"x")
        with self.assertRaisesRegex(SystemExit, "CRC|end"):
            validator.validate(root, TEST_PACKAGE)


if __name__ == "__main__":
    unittest.main()
