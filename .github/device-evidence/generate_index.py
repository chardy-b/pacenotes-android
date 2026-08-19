#!/usr/bin/env python3
"""Generate provenance-bound screenshot index and manifest."""
import argparse, hashlib, json
from pathlib import Path


def main():
    p = argparse.ArgumentParser()
    p.add_argument("contract", type=Path)
    p.add_argument("root", type=Path)
    p.add_argument("--provenance", required=True, type=Path)
    p.add_argument("--apk", required=True, type=Path,
                   help="APK file relative to the evidence root")
    a = p.parse_args()
    contract = json.loads(a.contract.read_text(encoding="utf-8"))
    provenance = json.loads(a.provenance.read_text(encoding="utf-8"))
    if not isinstance(provenance, dict) or not isinstance(provenance.get("device_gate"), dict):
        raise SystemExit("provenance must contain a device_gate mapping")
    apk_path = a.apk
    if apk_path.is_absolute() or ".." in apk_path.parts:
        raise SystemExit("APK path must be relative to the evidence root")
    apk = a.root / apk_path
    try:
        apk_data = apk.read_bytes()
    except OSError as exc:
        raise SystemExit(f"cannot read APK {apk_path}: {exc}") from exc
    if not apk_data:
        raise SystemExit("APK must be non-empty")
    apk_size = len(apk_data)
    apk_sha256 = hashlib.sha256(apk_data).hexdigest()
    device_gate = provenance["device_gate"]
    if device_gate.get("apk_size_bytes") != apk_size:
        raise SystemExit("APK size does not match device-gate provenance")
    if device_gate.get("apk_sha256") != apk_sha256:
        raise SystemExit("APK SHA256 does not match device-gate provenance")
    entries = []
    for name in contract["smoke_scenarios"] + contract["feature_scenarios"]:
        definition = contract["scenarios"][name]
        path = definition["path"]
        data = (a.root / path).read_bytes()
        entries.append({"scenario": name, "set": definition["set"],
                        "path": path, "profile_version": contract["profile_version"],
                        "sha256": hashlib.sha256(data).hexdigest(), "size": len(data)})
    payload = {"provenance": provenance, "entries": entries}
    (a.root / "index.json").write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n")
    (a.root / "manifest.json").write_text(json.dumps({
        "provenance": provenance,
        "apk": {"path": str(apk_path), "size": apk_size, "sha256": apk_sha256},
        "paths": [entry["path"] for entry in entries],
    }, indent=2, sort_keys=True) + "\n")


if __name__ == "__main__":
    main()
