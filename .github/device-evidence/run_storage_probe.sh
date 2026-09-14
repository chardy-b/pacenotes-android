#!/bin/sh
# Run one WIL-70 readiness test, then collect fresh ADB evidence.
set -eu

mkdir -p "$EVIDENCE_DIR/probe"
INSTRUMENTATION_STARTED=0
POST_INSTRUMENTATION_HEALTH_CAPTURED=0
collect() {
  status=$?
  adb logcat -d -t 1500 > "$EVIDENCE_DIR/device-logcat.txt" || true
  adb shell dumpsys activity activities > "$EVIDENCE_DIR/activity-state-at-exit.txt" || true
  adb shell dumpsys window windows > "$EVIDENCE_DIR/window-state-at-exit.txt" || true
  adb shell pm path "$APPLICATION_ID" > "$EVIDENCE_DIR/target-package-after.txt" || true
  adb shell pm path "$TEST_APPLICATION_ID" > "$EVIDENCE_DIR/test-package-after.txt" || true
  adb shell pidof "$APPLICATION_ID" > "$EVIDENCE_DIR/target-pid-after.txt" || true
  if test "$INSTRUMENTATION_STARTED" -eq 1 && test "$POST_INSTRUMENTATION_HEALTH_CAPTURED" -eq 0; then
    capture_emulator_health post-instrumentation || true
  elif test "$INSTRUMENTATION_STARTED" -eq 0; then
    mkdir -p "$EVIDENCE_DIR/emulator-health"
    printf 'unavailable: instrumentation did not start\n' > "$EVIDENCE_DIR/emulator-health/post-instrumentation-unavailable.txt"
  fi
  printf 'exit_status=%s\n' "$status" > "$EVIDENCE_DIR/status.txt"
  exit "$status"
}
trap collect 0

capture_emulator_health() {
  phase=$1
  remote_xml="/sdcard/wil-70-health-$phase.xml"
  mkdir -p "$EVIDENCE_DIR/emulator-health"
  adb shell dumpsys activity activities > "$EVIDENCE_DIR/emulator-health/$phase-activity.txt" || true
  adb shell dumpsys window windows > "$EVIDENCE_DIR/emulator-health/$phase-window.txt" || true
  adb shell rm -f "$remote_xml" || true
  adb shell uiautomator dump "$remote_xml" >/dev/null || true
  adb exec-out cat "$remote_xml" > "$EVIDENCE_DIR/emulator-health/$phase-ui.xml" || true
  adb shell rm -f "$remote_xml" || true
  adb exec-out screencap -p > "$EVIDENCE_DIR/emulator-health/$phase.png" || true
  if python3 - "$EVIDENCE_DIR/emulator-health/$phase-ui.xml" <<'PY' > "$EVIDENCE_DIR/emulator-health/$phase-anr-dialog.txt"
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except (OSError, ET.ParseError):
    raise SystemExit(1)
for node in root.iter():
    if node.attrib.get("package") != "android":
        continue
    value = " ".join(filter(None, (node.attrib.get("text"), node.attrib.get("content-desc"))))
    if re.search(r"(?:isn.?t responding|keeps stopping)", value, re.IGNORECASE):
        print(value)
        raise SystemExit(0)
raise SystemExit(1)
PY
  then
    echo "emulator health check failed: Android system ANR dialog during $phase: $(tr '\n' ' ' < "$EVIDENCE_DIR/emulator-health/$phase-anr-dialog.txt")" >&2
    return 1
  fi
  rm -f "$EVIDENCE_DIR/emulator-health/$phase-anr-dialog.txt"
  test -s "$EVIDENCE_DIR/emulator-health/$phase-ui.xml"
  test -s "$EVIDENCE_DIR/emulator-health/$phase.png"
}

capture_emulator_health pre-instrumentation

INSTRUMENTATION_STARTED=1
./gradlew --no-daemon --stacktrace :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rich.rallypacenotes.ReplayAlphaAppInstrumentedTest#wil70PostInstrumentationEvidenceProbeHasReadyMap \
  > "$EVIDENCE_DIR/instrumentation-output.txt" 2>&1

capture_emulator_health post-instrumentation
POST_INSTRUMENTATION_HEALTH_CAPTURED=1

apk=app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk" > "$EVIDENCE_DIR/target-reinstall.txt"
adb shell am force-stop "$APPLICATION_ID"
adb shell am start -W -n "$APPLICATION_ID/.MainActivity" > "$EVIDENCE_DIR/target-launch.txt"
capture_emulator_health post-launch

ready=0
for attempt in $(seq 1 10); do
  rm -f "$EVIDENCE_DIR/probe/foreground-window.txt" "$EVIDENCE_DIR/probe/target-pid.txt" "$EVIDENCE_DIR/probe/app-window.xml"
  adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|ResumedActivity' > "$EVIDENCE_DIR/probe/foreground-window.txt" || true;
  adb shell pidof "$APPLICATION_ID" > "$EVIDENCE_DIR/probe/target-pid.txt" || true
  remote_xml="/sdcard/wil-70-probe-window-$attempt.xml"
  adb shell rm -f "$remote_xml" || true
  adb shell uiautomator dump "$remote_xml" >/dev/null || true
  adb exec-out cat "$remote_xml" > "$EVIDENCE_DIR/probe/app-window.xml" || true
  adb shell rm -f "$remote_xml" || true
  if test -s "$EVIDENCE_DIR/probe/foreground-window.txt" \
    && test -s "$EVIDENCE_DIR/probe/target-pid.txt" \
    && test -s "$EVIDENCE_DIR/probe/app-window.xml" \
    && grep -q "$APPLICATION_ID/.MainActivity" "$EVIDENCE_DIR/probe/foreground-window.txt" \
    && grep -q 'Map status: ready' "$EVIDENCE_DIR/probe/app-window.xml"; then
    ready=1
    break
  fi
  sleep 3
done
test "$ready" -eq 1

adb shell pm path "$APPLICATION_ID" > "$EVIDENCE_DIR/target-package-before-capture.txt"
test -s "$EVIDENCE_DIR/target-package-before-capture.txt"
grep -q '^package:/data/app/' "$EVIDENCE_DIR/target-package-before-capture.txt"
adb exec-out screencap -p > "$EVIDENCE_DIR/probe/probe.png"
printf 'capture_source=adb-post-instrumentation\ncaptured_after_instrumentation=true\ntarget_package=%s\ntest_package=%s\nhead_sha=%s\n' "$APPLICATION_ID" "$TEST_APPLICATION_ID" "$EXPECTED_SHA" > "$EVIDENCE_DIR/probe/identity.txt"
python3 - "$EVIDENCE_DIR/probe/run-provenance.json" <<'PY'
import json
import os
import re
import subprocess
import sys

head_sha = os.environ["EXPECTED_SHA"]
if not re.fullmatch(r"[0-9a-f]{40}", head_sha):
    raise SystemExit("EXPECTED_SHA must be a full lowercase commit SHA")
checkout_sha = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
if checkout_sha != head_sha:
    raise SystemExit("checkout SHA did not match EXPECTED_SHA")
run_id = os.environ["GITHUB_RUN_ID"]
if not run_id.isdigit():
    raise SystemExit("GITHUB_RUN_ID must be numeric")
json.dump(
    {
        "schema_version": 1,
        "repository": os.environ["GITHUB_REPOSITORY"],
        "workflow_run_id": run_id,
        "workflow_run_attempt": os.environ["GITHUB_RUN_ATTEMPT"],
        "head_sha": head_sha,
        "checkout_sha": checkout_sha,
    },
    open(sys.argv[1], "w", encoding="utf-8"),
    sort_keys=True,
)
PY
python3 .github/device-evidence/validate_storage_probe.py "$EVIDENCE_DIR/probe" "$TEST_APPLICATION_ID" > "$EVIDENCE_DIR/validation.json"
cp -R app/build/reports/androidTests "$EVIDENCE_DIR/androidTest-reports"
