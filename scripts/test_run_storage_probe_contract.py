import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
SOURCE_SCRIPT = HERE / ".github/device-evidence/run_storage_probe.sh"


class ProbeFailureHealthArtifactTests(unittest.TestCase):
    def test_instrumentation_failure_collects_post_instrumentation_health(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "run_storage_probe.sh").write_text(SOURCE_SCRIPT.read_text(), encoding="utf-8")
            (root / "gradlew").write_text("#!/bin/sh\nexit 1\n", encoding="utf-8")
            bin_dir = root / "bin"
            bin_dir.mkdir()
            (bin_dir / "adb").write_text(
                "#!/bin/sh\n"
                "if test \"$1\" = exec-out && test \"$2\" = cat; then\n"
                "  printf '%s' '<hierarchy />'\n"
                "elif test \"$1\" = exec-out && test \"$2\" = screencap; then\n"
                "  printf x\n"
                "fi\n",
                encoding="utf-8",
            )
            for executable in (root / "gradlew", bin_dir / "adb", root / "run_storage_probe.sh"):
                executable.chmod(0o755)
            evidence = root / "evidence"
            environment = {
                **os.environ,
                "PATH": f"{bin_dir}:{os.environ['PATH']}",
                "EVIDENCE_DIR": str(evidence),
                "APPLICATION_ID": "com.rich.rallypacenotes",
                "TEST_APPLICATION_ID": "com.rich.rallypacenotes.test",
            }
            result = subprocess.run(["sh", "./run_storage_probe.sh"], cwd=root, env=environment, capture_output=True, text=True)
            self.assertEqual(result.returncode, 1, result.stderr)
            health = evidence / "emulator-health"
            for path in (
                health / "pre-instrumentation-ui.xml",
                health / "pre-instrumentation.png",
                health / "post-instrumentation-ui.xml",
                health / "post-instrumentation.png",
            ):
                self.assertTrue(path.is_file() and path.stat().st_size > 0, path)
            self.assertEqual((evidence / "status.txt").read_text(encoding="utf-8"), "exit_status=1\n")


if __name__ == "__main__":
    unittest.main()
