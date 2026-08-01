# Replay Alpha Installable MVP Plan

> **For Hermes:** Execute with strict TDD for production behavior; use the Android QA skill for package/device evidence.

**Goal:** Deliver the first installable, launchable, deterministic Replay Alpha: a tester launches the app, chooses a bundled synthetic route fixture, runs/pause/resets replay, sees geometry-derived curve candidates, and can evaluate the callouts without GPS or network access.

**Architecture:** Keep route detection in `pacenotes`. The Android `app` module owns a `MainActivity`, Compose screen, and an app-owned replay state holder. Use bundled synthetic fixture data only for this alpha—no GPX parser, location permission, foreground service, routing, or TTS claim until their dedicated work items. A route canvas may initially be a text/debug trace; it must label the state as REPLAY / NOT FOR DRIVING.

**Tech stack:** Kotlin/Android, Compose Material 3, existing pure modules. No `INTERNET` permission, provider SDK, or credential.

---

### Task 1: Make a launchable Compose app

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/MainActivity.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ReplayAlphaApp.kt`

Add an exported `MAIN`/`LAUNCHER` activity. Render an explicit “Replay Alpha — Not for Driving” screen with deterministic fixture content. Add a Compose instrumentation test for the required safety label and initial replay state. Build the debug APK and inspect manifest badging.

### Task 2: Add deterministic in-app replay state

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/replay/ReplayController.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/replay/ReplayControllerTest.kt`

TDD controller state: selected fixture, stopped/running/paused, deterministic current distance, start/pause/reset. No wall-clock loop yet. Tests must prove state transitions and reset behavior.

### Task 3: Connect one bundled fixture to detected candidates

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/replay/ReplayFixtures.kt`
- Modify: `ReplayAlphaApp.kt`

Use a synthetic normalized route fixture and `CurveDetector` output. Show candidate direction/severity/geometry distance or explicit “No conservative curve candidate.” Test the fixture bridge as a JVM test.

### Task 4: Add user controls and UI checks

Add Start, Pause, Reset controls and stable semantics labels. Compose instrumentation tests must show state changes. Include no TTS unless a dedicated scheduler/runtime task is completed.

### Task 5: Device gate

Run all relevant Gradle tests and `:app:assembleDebug`; inspect launcher activity with `aapt`. If an ADB device is available, install, launch, capture a real screenshot, and inspect logcat. Without a connected device, report the physical install/launch gate as blocked—not passed.
