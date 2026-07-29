# Initial QA Report — 2026-07-26

## Scope

Initial validation of the credential-free Android scaffold and provider-neutral core model. This report does **not** claim user-interface, GPX import, replay, GPS, TTS, foreground-service, Bluetooth, or field-driving validation: those features do not exist yet.

## Automated results

| Check | Result | Evidence |
|---|---|---|
| Core model tests | Pass | `:core-model:test`; 10 tests across `GeoPointTest` and `RouteGeometryTest` passed. |
| Defensive input-list regression | Pass | A `MutableList` used to construct `RouteGeometry` can be cleared without changing the route snapshot. |
| Debug APK build | Pass | `:app:assembleDebug`; APK at `app/build/outputs/apk/debug/app-debug.apk`. |
| Diff whitespace check | Pass | `git diff --check`. |
| Package metadata | Pass | Package `com.rich.rallypacenotes`, `minSdk=26`, `targetSdk=35`; no `INTERNET` permission declared. |

## Exploratory/device QA result

**Blocked by intentional scaffold state.** There is no `Activity`, launcher intent filter, Kotlin app source, connected device, or installed Android emulator. The assembled APK therefore has no runnable screen to install, navigate, or capture. No UI screenshot can truthfully be produced yet.

## QA finding

| ID | Severity | Category | Finding |
|---|---|---|---|
| QA-001 | High | Functional / testability | The current APK has no launcher activity or user interface, so it cannot be opened for on-device interaction testing or screenshots. This is expected before the planned replay/UI slice, not a regression in the domain model. |

## Screenshot readiness gate

Before screenshots can be produced, implement and test a minimal launcher `MainActivity` plus a Compose replay/route-state screen, then install it on an emulator or physical Android device. The UI must expose a deterministic fixture/replay state so visual screenshots are meaningful and repeatable.

## Next QA scope after the first screen exists

1. Install debug APK on an emulator/device.
2. Capture screenshots for empty/import, route-loaded, replay-active, and paused/off-route states.
3. Run accessibility semantics checks and a 200% font-scale visual pass.
4. Capture Android logcat and lifecycle/TTS evidence for replay.
