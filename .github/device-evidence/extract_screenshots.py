#!/usr/bin/env python3
"""Extract the complete contract-declared screenshot set from app-private storage."""

import argparse
import json
import os
import pathlib
import subprocess


def pull(application_id: str, source: str, destination: pathlib.Path) -> None:
    with destination.open("wb") as handle:
        subprocess.run(
            ["adb", "exec-out", "run-as", application_id, "cat", source],
            stdout=handle,
            check=True,
        )
    if not destination.stat().st_size:
        raise SystemExit(f"empty extracted evidence: {destination.name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("contract", type=pathlib.Path)
    parser.add_argument("screenshot_dir")
    parser.add_argument("evidence_dir", type=pathlib.Path)
    args = parser.parse_args()

    if pathlib.PurePosixPath(args.screenshot_dir).is_absolute() or ".." in pathlib.PurePosixPath(args.screenshot_dir).parts:
        raise SystemExit("screenshot directory must be a relative app-private path")
    application_id = os.environ["APPLICATION_ID"]
    contract = json.loads(args.contract.read_text(encoding="utf-8"))
    args.evidence_dir.mkdir(parents=True, exist_ok=True)

    for name in contract["smoke_scenarios"] + contract["feature_scenarios"]:
        path = pathlib.PurePosixPath(contract["scenarios"][name]["path"])
        if path.is_absolute() or ".." in path.parts or path.suffix.lower() != ".png":
            raise SystemExit(f"unsafe contract screenshot path: {path}")
        destination = args.evidence_dir / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        pull(application_id, f"files/{args.screenshot_dir}/{path}", destination)

    pull(application_id, f"files/{args.screenshot_dir}/app-window.xml", args.evidence_dir / "app-window.xml")


if __name__ == "__main__":
    main()
