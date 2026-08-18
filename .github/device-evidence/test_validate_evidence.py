import hashlib
import importlib.util
import json
import tempfile
import unittest
import subprocess
import zlib
from pathlib import Path

HERE = Path(__file__).parent
spec = importlib.util.spec_from_file_location("validate_evidence", HERE / "validate_evidence.py")
validator = importlib.util.module_from_spec(spec)
# Tests intentionally load the standalone script without package assumptions.
spec.loader.exec_module(validator)

def chunk(kind, payload=b""):
    return len(payload).to_bytes(4, "big") + kind + payload + zlib.crc32(kind + payload).to_bytes(4, "big")

PNG = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00") + chunk(b"IDAT", b"x") + chunk(b"IEND")
SHA = "a" * 40
RUN = 42

def png_for(label):
    payload = label.encode()
    chunk = len(payload).to_bytes(4, "big") + b"tEXt" + payload + zlib.crc32(b"tEXt" + payload).to_bytes(4, "big")
    return PNG[:33] + chunk + PNG[33:]


def write_fixture(root, *, contract=None, entries=None, provenance=None, files=None):
    root.mkdir(parents=True, exist_ok=True)
    contract = contract or {"schema_version": "1", "profile_version": "1", "privacy": {"redaction_required": True, "contains_credentials": False, "contains_private_data": False}, "smoke_scenarios": ["launch-stopped-v1", "running-v1", "paused-v1", "reset-v1"], "feature_scenarios": ["wil-86-evidence-v1"], "scenarios": {s: {"id": s, "path": f"{s}.png", "set": "smoke" if s != "wil-86-evidence-v1" else "feature", "profile_version": "1", "expected_state": "expected", "privacy": {"redaction_required": True}} for s in ["launch-stopped-v1", "running-v1", "paused-v1", "reset-v1", "wil-86-evidence-v1"]}}
    (root / "contract.json").write_text(json.dumps(contract))
    files = files or {f"{s}.png": png_for(s) for s in contract["smoke_scenarios"] + contract["feature_scenarios"]}
    for name, data in files.items():
        p = root / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(data)
    entries = entries if entries is not None else [{"scenario": s, "path": f"{s}.png", "set": "smoke" if s in contract["smoke_scenarios"] else "feature", "profile_version": "1", "sha256": hashlib.sha256(files[f"{s}.png"]).hexdigest(), "size": len(files[f"{s}.png"])} for s in contract["smoke_scenarios"] + contract["feature_scenarios"]]
    provenance = provenance or {"baseline": {"workflow": "android-baseline", "run_id": 7, "run_attempt": 1, "commit": SHA}, "device_gate": {"repository": "owner/repo", "workflow": "device-evidence", "commit": SHA, "run_id": RUN, "run_attempt": 1, "profile_version": "1", "schema_version": "1"}}
    if "baseline" not in provenance or "device_gate" not in provenance:
        provenance = {"baseline": {"workflow": "android-baseline", "run_id": 7, "run_attempt": 1, "commit": SHA}, "device_gate": provenance}
    (root / "index.json").write_text(json.dumps({"provenance": provenance, "entries": entries}))
    apk = b"fixture-apk"
    (root / "app-debug.apk").write_bytes(apk)
    provenance["device_gate"]["apk_size_bytes"] = len(apk)
    provenance["device_gate"]["apk_sha256"] = hashlib.sha256(apk).hexdigest()
    (root / "index.json").write_text(json.dumps({"provenance": provenance, "entries": entries}))
    (root / "manifest.json").write_text(json.dumps({"provenance": provenance, "apk": {"path": "app-debug.apk", "size": len(apk), "sha256": hashlib.sha256(apk).hexdigest()}, "paths": [e["path"] for e in entries]}))


class ValidatorTests(unittest.TestCase):
    def test_rejects_invalid_critical_chunk_order_and_missing_idat(self):
        ihdr = chunk(b"IHDR", b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00")
        idat = chunk(b"IDAT", b"x")
        iend = chunk(b"IEND")
        signature = b"\x89PNG\r\n\x1a\n"
        cases = [signature + iend, signature + ihdr + iend,
                 signature + idat + ihdr + iend,
                 signature + ihdr + idat + chunk(b"tEXt", b"ok") + idat + iend]
        for data in cases:
            with self.subTest(data=data):
                self.assertFalse(validator._valid_png(data))
        self.assertTrue(validator._valid_png(signature + ihdr + idat + iend))

    def test_allows_ancillary_chunk_after_idat_but_rejects_later_idat(self):
        signature = b"\x89PNG\r\n\x1a\n"
        ihdr = chunk(b"IHDR", b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00")
        idat = chunk(b"IDAT", b"x")
        text = chunk(b"tEXt", b"note\x00legal post-IDAT metadata")
        iend = chunk(b"IEND")
        self.assertTrue(validator._valid_png(signature + ihdr + idat + text + iend))
        self.assertFalse(validator._valid_png(signature + ihdr + idat + text + idat + iend))

    def test_valid_fixture(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            self.assertEqual(validator.validate(root / "contract.json", root, SHA, RUN), [])

    def test_rejects_missing_empty_and_unknown_inventory(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root, entries=[])
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("smoke" in e for e in errors))
            (root / "extra.png").write_bytes(PNG)
            self.assertTrue(any("unknown" in e for e in validator.validate(root / "contract.json", root, SHA, RUN)))

    def test_rejects_provenance_and_unsafe_paths(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            entry = {"scenario":"launch-stopped-v1", "path":"../secret.png", "sha256":"bad", "size":1}
            write_fixture(root, entries=[entry], provenance={"commit":"b"*40,"run_id":0,"profile_version":"old","schema_version":"old"})
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertGreaterEqual(len(errors), 4)
            self.assertTrue(any("relative" in e for e in errors))

    def test_rejects_duplicates_invalid_png_and_feature_absence(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            files = {"a.png": b"not-png", "b.png": b"not-png"}
            contract = {"schema_version":"1", "profile_version":"1", "smoke_scenarios":["launch-stopped-v1","running-v1","paused-v1","reset-v1"], "feature_scenarios":["feature"]}
            entries = [{"scenario":"launch-stopped-v1","path":"a.png","sha256":hashlib.sha256(b"not-png").hexdigest(),"size":7}, {"scenario":"launch-stopped-v1","path":"b.png","sha256":hashlib.sha256(b"not-png").hexdigest(),"size":7}]
            write_fixture(root, contract=contract, entries=entries, files=files)
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("duplicate" in e for e in errors))
            self.assertTrue(any("PNG" in e for e in errors))
            self.assertTrue(any("feature" in e for e in errors))

    def test_rejects_malformed_contract_and_duplicate_feature(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            contract = {"schema_version": 1, "profile_version": "1", "smoke_scenarios": ["launch-stopped-v1"], "feature_scenarios": ["feature", "feature"], "scenarios": {"launch-stopped-v1": {"expected_state": "stopped"}, "feature": {"id": "wrong", "path": "feature.jpg", "set": "smoke", "expected_state": "x"}}}
            write_fixture(root, contract=contract, entries=[])
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("schema" in e for e in errors))
            self.assertTrue(any("duplicate feature" in e for e in errors))
            self.assertTrue(any("metadata" in e or "path" in e for e in errors))

    def test_rejects_duplicate_ids_and_contract_entry_mismatch(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            contract = {"schema_version": "1", "profile_version": "1", "smoke_scenarios": ["launch-stopped-v1"], "feature_scenarios": ["feature"], "scenarios": {"launch-stopped-v1": {"id": "launch-stopped-v1", "path": "expected.png", "set": "smoke", "expected_state": "stopped", "privacy": {"redaction_required": True}}, "feature": {"id": "feature", "path": "feature.png", "set": "feature", "expected_state": "verified", "privacy": {"redaction_required": True}}}}
            files = {"actual.png": PNG, "feature.png": PNG[:-12] + b"\x00\x00\x00\x01tEXtX\x00\x00\x00\x00" + PNG[-12:]}
            entries = [{"scenario": "launch-stopped-v1", "path": "actual.png", "sha256": hashlib.sha256(files["actual.png"]).hexdigest(), "size": len(files["actual.png"])}, {"scenario": "launch-stopped-v1", "path": "feature.png", "sha256": hashlib.sha256(files["feature.png"]).hexdigest(), "size": len(files["feature.png"])}]
            write_fixture(root, contract=contract, entries=entries, files=files)
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("duplicate scenario" in e for e in errors))
            self.assertTrue(any("contract" in e for e in errors))

    def test_rejects_malformed_entry_without_traceback(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            index = json.loads((root / "index.json").read_text()); index["entries"] = [1]
            (root / "index.json").write_text(json.dumps(index))
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("entries" in e for e in errors))

    def test_rejects_malformed_entry_fields_without_traceback(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            valid = json.loads((root / "index.json").read_text())["entries"][0]
            cases = {
                "path": 7,
                "scenario": [],
                "set": {},
                "sha256": "A" * 64,
                "size": True,
            }
            for field, value in cases.items():
                with self.subTest(field=field):
                    entry = dict(valid); entry[field] = value
                    index = json.loads((root / "index.json").read_text())
                    index["entries"] = [entry]
                    (root / "index.json").write_text(json.dumps(index))
                    errors = validator.validate(root / "contract.json", root, SHA, RUN)
                    self.assertTrue(errors)
                    self.assertTrue(all(isinstance(error, str) for error in errors))

    def test_cli_rejects_non_hex_expected_sha(self):
        script = HERE / "validate_evidence.py"
        result = subprocess.run(["python3", str(script), "missing.json", ".", "z" * 40, "1"], capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("lowercase hexadecimal", result.stderr)

    def test_rejects_non_mapping_provenance_without_traceback(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            (root / "index.json").write_text(json.dumps({"provenance": [], "entries": []}))
            (root / "manifest.json").write_text(json.dumps({"provenance": [], "paths": []}))
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("provenance" in e for e in errors))

    def test_rejects_malformed_scenario_sets_without_traceback(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            contract = json.loads((root / "contract.json").read_text())
            contract["smoke_scenarios"] = ["ok", 1]
            contract["feature_scenarios"] = "feature"
            (root / "contract.json").write_text(json.dumps(contract))
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("scenario" in e for e in errors))

    def test_rejects_invalid_top_level_privacy(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); write_fixture(root)
            contract = json.loads((root / "contract.json").read_text())
            contract["privacy"] = {"redaction_required": True, "contains_credentials": "yes", "contains_private_data": False}
            (root / "contract.json").write_text(json.dumps(contract))
            errors = validator.validate(root / "contract.json", root, SHA, RUN)
            self.assertTrue(any("privacy" in e for e in errors))

    def test_non_mapping_roots_return_errors_not_tracebacks(self):
        for field in ("contract", "index", "manifest"):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as d:
                root = Path(d); write_fixture(root)
                if field == "contract": (root / "contract.json").write_text("[]")
                else: (root / f"{field}.json").write_text("[]")
                errors = validator.validate(root / "contract.json", root, SHA, RUN)
                self.assertTrue(errors)
                self.assertTrue(all(isinstance(error, str) for error in errors))

    def test_cli_rejects_non_positive_run_ids(self):
        script = HERE / "validate_evidence.py"
        for run_id in ("0", "-1"):
            result = subprocess.run(["python3", str(script), "missing.json", ".", SHA, run_id], capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("positive", result.stderr)

if __name__ == "__main__": unittest.main()
