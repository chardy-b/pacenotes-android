#!/bin/sh
# Run one WIL-70 readiness test, then collect fresh ADB evidence.
set -eu

mkdir -p "$EVIDENCE_DIR/probe"
collect() {
  status=$?
  adb logcat -d -t 1500 > "$EVIDENCE_DIR/device-logcat.txt" || true
  adb shell pm path "$APPLICATION_ID" > "$EVIDENCE_DIR/target-package-after.txt" || true
  adb shell pm path "$TEST_APPLICATION_ID" > "$EVIDENCE_DIR/test-package-after.txt" || true
  adb shell pidof "$APPLICATION_ID" > "$EVIDENCE_DIR/target-pid-after.txt" || true
  printf 'exit_status=%s\n' "$status" > "$EVIDENCE_DIR/status.txt"
  exit "$status"
}
trap collect 0

./gradlew --no-daemon --stacktrace :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rich.rallypacenotes.ReplayAlphaAppInstrumentedTest#wil70PostInstrumentationEvidenceProbeHasReadyMap \
  > "$EVIDENCE_DIR/instrumentation-output.txt" 2>&1

apk=app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk" > "$EVIDENCE_DIR/target-reinstall.txt"
adb shell am force-stop "$APPLICATION_ID"
adb shell am start -W -n "$APPLICATION_ID/.MainActivity" > "$EVIDENCE_DIR/target-launch.txt"

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
printf 'capture_source=adb-post-instrumentation\ncaptured_after_instrumentation=true\ntarget_package=%s\ntest_package=%s\n' "$APPLICATION_ID" "$TEST_APPLICATION_ID" > "$EVIDENCE_DIR/probe/identity.txt"
python3 .github/device-evidence/validate_storage_probe.py "$EVIDENCE_DIR/probe" "$TEST_APPLICATION_ID" > "$EVIDENCE_DIR/validation.json"
cp -R app/build/reports/androidTests "$EVIDENCE_DIR/androidTest-reports"
