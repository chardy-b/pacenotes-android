#!/usr/bin/env python3
"""Check that release evidence has exactly the contract screenshots."""
import json
import sys
from pathlib import Path

contract = json.loads(Path(sys.argv[1]).read_text())
root = Path(sys.argv[2])
names = contract["smoke_scenarios"] + contract["feature_scenarios"]
expected = sorted(contract["scenarios"][name]["path"] for name in names)
actual = sorted(str(path.relative_to(root)) for path in root.rglob("*.png"))
if actual != expected:
    raise SystemExit(f"contract screenshots differ: expected={expected}, actual={actual}")
