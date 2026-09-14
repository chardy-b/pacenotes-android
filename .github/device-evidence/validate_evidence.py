#!/usr/bin/env python3
"""Validate deterministic screenshot evidence against a scenario contract.

This validates internal consistency only. An evidence-local digest cannot defend
against an actor replacing both the screenshot and its index; the Task 2
workflow must bind the downloaded artifact to the GitHub run provenance.
"""
import argparse
import hashlib
import json
import re
import sys
from collections.abc import Mapping
from pathlib import Path


def _load(path):
    try:
        with path.open(encoding="utf-8") as f:
            return json.load(f)
    except Exception as exc:
        raise ValueError(f"cannot read JSON {path}: {exc}") from exc


def _valid_png(data):
    """Strictly validate PNG structure; malformed input never escapes."""
    try:
        if not isinstance(data, bytes) or not data.startswith(b"\x89PNG\r\n\x1a\n"):
            return False
        pos, seen, saw_idat = 8, set(), False
        saw_ihdr = False
        idat_ended = False
        while pos < len(data):
            if pos + 12 > len(data): return False
            length = int.from_bytes(data[pos:pos + 4], "big")
            kind = data[pos + 4:pos + 8]
            end = pos + 12 + length
            if end > len(data) or not re.fullmatch(rb"[A-Za-z]{4}", kind): return False
            payload = data[pos + 8:pos + 8 + length]
            crc = int.from_bytes(data[pos + 8 + length:end], "big")
            import zlib
            if zlib.crc32(kind + payload) & 0xffffffff != crc: return False
            if kind == b"IHDR":
                if seen or length != 13: return False
                saw_ihdr = True
                width = int.from_bytes(payload[0:4], "big")
                height = int.from_bytes(payload[4:8], "big")
                depth, color = payload[8], payload[9]
                if not width or not height or (depth, color) not in {(1,0),(2,0),(4,0),(8,0),(16,0),(1,3),(2,3),(4,3),(8,3),(8,4),(16,4),(8,6),(16,6)}: return False
            elif kind == b"IDAT":
                if not saw_ihdr or idat_ended: return False
                saw_idat = True
            elif kind == b"IEND":
                if length or end != len(data) or not saw_ihdr or not saw_idat: return False
                return True
            elif kind in {b"PLTE", b"tRNS"} and (kind in seen or (kind == b"PLTE" and saw_idat)): return False
            elif kind[0] & 32 == 0:
                # Unknown critical chunks cannot be safely ignored.
                return False
            elif kind in seen and (kind[0] & 32) == 0: return False
            if kind[0] & 32 == 0 and kind not in {b"IHDR", b"IDAT", b"IEND", b"PLTE", b"tRNS"} and kind in seen: return False
            if kind != b"IDAT" and saw_idat: idat_ended = True
            seen.add(kind); pos = end
        return False
    except Exception:
        return False


def validate(contract_path, evidence_root, expected_sha, expected_run):
    errors = []
    root = Path(evidence_root).resolve()
    try:
        contract = _load(Path(contract_path))
        index = _load(root / "index.json")
        manifest = _load(root / "manifest.json")
    except ValueError as exc:
        return [str(exc)]
    if not isinstance(contract, Mapping):
        return ["contract root must be a mapping"]
    if not isinstance(index, Mapping):
        errors.append("index root must be a mapping")
        index = {}
    if not isinstance(manifest, Mapping):
        errors.append("manifest root must be a mapping")
        manifest = {}
    smoke = contract.get("smoke_scenarios", [])
    feature = contract.get("feature_scenarios", [])
    for label, scenarios in (("smoke", smoke), ("feature", feature)):
        if not isinstance(scenarios, list) or any(not isinstance(s, str) or not s for s in scenarios):
            errors.append(f"{label} scenarios must be a list of non-empty strings")
    smoke = smoke if isinstance(smoke, list) and all(isinstance(s, str) and s for s in smoke) else []
    feature = feature if isinstance(feature, list) and all(isinstance(s, str) and s for s in feature) else []
    if not isinstance(contract.get("schema_version"), str) or not contract.get("schema_version"): errors.append("schema version must be a string")
    if not isinstance(contract.get("profile_version"), str) or not contract.get("profile_version"): errors.append("profile version must be a string")
    privacy = contract.get("privacy")
    if not isinstance(privacy, Mapping) or any(not isinstance(privacy.get(key), bool) for key in ("redaction_required", "contains_credentials", "contains_private_data")):
        errors.append("invalid top-level privacy metadata")
    if not smoke: errors.append("smoke scenario set is empty")
    if not feature: errors.append("feature scenario set is empty")
    if len(smoke) != len(set(smoke)): errors.append("duplicate smoke scenarios")
    if len(feature) != len(set(feature)): errors.append("duplicate feature scenarios")
    scenario_defs = contract.get("scenarios")
    if not isinstance(scenario_defs, dict):
        errors.append("missing scenario definitions")
        scenario_defs = {}
    for set_name, names in (("smoke", smoke), ("feature", feature)):
        for name in names if isinstance(names, list) else []:
            definition = scenario_defs.get(name)
            if not isinstance(definition, dict):
                errors.append(f"missing {set_name} scenario definition: {name}"); continue
            if definition.get("id") != name: errors.append(f"invalid scenario id: {name}")
            path = definition.get("path")
            if not isinstance(path, str) or not path or Path(path).is_absolute() or ".." in Path(path).parts or Path(path).suffix.lower() != ".png": errors.append(f"invalid scenario path: {name}")
            if definition.get("set") != set_name: errors.append(f"invalid scenario set: {name}")
            if definition.get("profile_version") != contract.get("profile_version"): errors.append(f"scenario profile version mismatch: {name}")
            if not isinstance(definition.get("expected_state"), str) or not definition.get("expected_state"): errors.append(f"invalid expected state: {name}")
            privacy = definition.get("privacy")
            if not isinstance(privacy, dict) or not isinstance(privacy.get("redaction_required"), bool): errors.append(f"missing privacy/redaction metadata: {name}")
    referenced = smoke + feature if isinstance(smoke, list) and isinstance(feature, list) else []
    if set(scenario_defs) != set(referenced): errors.append("scenario definitions do not match declared sets")
    provenance = index.get("provenance", {})
    mp = manifest.get("provenance", {})
    if not isinstance(provenance, Mapping):
        errors.append("index provenance must be a mapping"); provenance = {}
    if not isinstance(mp, Mapping):
        errors.append("manifest provenance must be a mapping"); mp = {}
    for source, values in (("index", provenance), ("manifest", mp)):
        if not isinstance(values.get("baseline"), Mapping) or not isinstance(values.get("device_gate"), Mapping):
            errors.append(f"{source} provenance must preserve baseline and device_gate sections")
    provenance = provenance.get("device_gate", {}) if isinstance(provenance, Mapping) else {}
    mp = mp.get("device_gate", {}) if isinstance(mp, Mapping) else {}
    for key, expected in (("commit", expected_sha), ("run_id", expected_run), ("profile_version", contract.get("profile_version")), ("schema_version", contract.get("schema_version"))):
        if provenance.get(key) != expected: errors.append(f"stale/mismatched index {key}")
        if mp.get(key) != expected: errors.append(f"stale/mismatched manifest {key}")
    for source, values in (("index", provenance), ("manifest", mp)):
        for key in ("repository", "workflow", "commit", "run_id", "run_attempt"):
            value = values.get(key)
            valid = isinstance(value, str) and bool(value) if key in ("repository", "workflow", "commit") else isinstance(value, int) and not isinstance(value, bool) and value > 0
            if not valid: errors.append(f"invalid {source} provenance {key}")
    entries = index.get("entries", [])
    if not isinstance(entries, list) or any(not isinstance(entry, Mapping) for entry in entries):
        errors.append("index entries must be a list of mappings")
        entries = [entry for entry in entries if isinstance(entry, Mapping)] if isinstance(entries, list) else []
    paths, hashes = [], []
    for entry in entries:
        scenario = entry.get("scenario")
        path = entry.get("path")
        entry_set = entry.get("set")
        entry_sha = entry.get("sha256")
        entry_size = entry.get("size")
        if entry.get("profile_version") != contract.get("profile_version"): errors.append(f"entry profile version mismatch: {scenario}")
        valid_scenario = isinstance(scenario, str) and bool(scenario)
        valid_path = isinstance(path, str) and bool(path)
        valid_set = isinstance(entry_set, str) and bool(entry_set)
        valid_sha = isinstance(entry_sha, str) and re.fullmatch(r"[0-9a-f]{64}", entry_sha) is not None
        valid_size = isinstance(entry_size, int) and not isinstance(entry_size, bool) and entry_size >= 0
        if not valid_scenario:
            errors.append(f"invalid scenario: {scenario}")
        if not valid_set:
            errors.append(f"invalid entry set: {entry_set}")
        if not valid_sha:
            errors.append(f"invalid SHA256: {entry_sha}")
        if not valid_size:
            errors.append(f"invalid size: {entry_size}")
        if not isinstance(path, str) or not path:
            errors.append(f"unsafe relative path: {path}"); continue
        p = Path(path)
        if p.is_absolute() or ".." in p.parts or p.suffix.lower() != ".png":
            errors.append(f"unsafe relative path: {path}"); continue
        if path in paths: errors.append(f"duplicate path: {path}")
        paths.append(path)
        try:
            target = (root / p).resolve(); target.relative_to(root)
            data = target.read_bytes()
        except (OSError, ValueError):
            errors.append(f"missing screenshot: {path}"); continue
        digest = hashlib.sha256(data).hexdigest()
        if digest in hashes: errors.append(f"duplicate hash: {path}")
        hashes.append(digest)
        if not data or not _valid_png(data): errors.append(f"invalid PNG: {path}")
        if valid_sha and entry_sha != digest: errors.append(f"SHA mismatch: {path}")
        if valid_size and entry_size != len(data): errors.append(f"size mismatch: {path}")
    declared = set(paths)
    actual = {str(p.relative_to(root)) for p in root.rglob("*.png") if p.is_file()}
    for unknown in sorted(actual - declared): errors.append(f"unknown PNG: {unknown}")
    manifest_paths = manifest.get("paths", [])
    if not isinstance(manifest_paths, list) or any(not isinstance(path, str) for path in manifest_paths):
        errors.append("manifest paths must be a list of strings")
        manifest_paths = []
    if manifest_paths != paths: errors.append("manifest path inventory mismatch")
    entry_ids = [e.get("scenario") for e in entries if isinstance(e.get("scenario"), str)]
    if len(entry_ids) != len(set(entry_ids)): errors.append("duplicate scenario IDs")
    scenarios = set(entry_ids)
    for required in smoke:
        if required not in scenarios: errors.append(f"missing smoke scenario evidence: {required}")
    for required in feature:
        if required not in scenarios: errors.append(f"missing feature scenario evidence: {required}")
    if any(not isinstance(e.get("scenario"), str) or e.get("scenario") not in set(smoke + feature) for e in entries): errors.append("undeclared scenario")
    for entry in entries:
        definition = scenario_defs.get(entry.get("scenario"), {}) if isinstance(entry.get("scenario"), str) else {}
        if entry.get("path") != definition.get("path"): errors.append(f"contract path mismatch: {entry.get('scenario')}")
        expected_set = definition.get("set")
        if entry.get("set") != expected_set: errors.append(f"contract set mismatch: {entry.get('scenario')}")
    apk = manifest.get("apk")
    if not isinstance(apk, Mapping):
        errors.append("manifest APK metadata must be a mapping")
    else:
        apk_path, apk_size, apk_sha = apk.get("path"), apk.get("size"), apk.get("sha256")
        valid_apk_path = isinstance(apk_path, str) and bool(apk_path) and not Path(apk_path).is_absolute() and ".." not in Path(apk_path).parts
        if not valid_apk_path: errors.append("invalid manifest APK path")
        if not isinstance(apk_size, int) or isinstance(apk_size, bool) or apk_size <= 0: errors.append("manifest APK size must be positive")
        if not isinstance(apk_sha, str) or re.fullmatch(r"[0-9a-f]{64}", apk_sha) is None: errors.append("invalid manifest APK SHA256")
        if valid_apk_path:
            try: apk_data = (root / apk_path).resolve(); apk_data.relative_to(root); data = apk_data.read_bytes()
            except (OSError, ValueError): data = None
            if data is None: errors.append("missing manifest APK")
            else:
                if apk_size != len(data): errors.append("manifest APK size mismatch")
                if apk_sha != hashlib.sha256(data).hexdigest(): errors.append("manifest APK SHA256 mismatch")
                device_apk = provenance.get("apk_size_bytes"), provenance.get("apk_sha256")
                if device_apk != (apk_size, apk_sha): errors.append("manifest APK does not match device-gate provenance")
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("contract")
    parser.add_argument("evidence_root")
    parser.add_argument("expected_sha")
    parser.add_argument("expected_run", type=int)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.expected_sha): parser.error("expected SHA must be 40 lowercase hexadecimal characters")
    if args.expected_run <= 0: parser.error("expected run ID must be positive")
    errors = validate(args.contract, args.evidence_root, args.expected_sha, args.expected_run)
    if errors:
        for error in errors: print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("evidence valid")
    return 0

if __name__ == "__main__": sys.exit(main())
