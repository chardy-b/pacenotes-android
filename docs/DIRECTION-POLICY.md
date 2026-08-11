# Direction and Navigation-Camera Policy

**Status:** Implemented design for WIL-76; device verification remains required before the issue can close.

## Purpose

The hosted MapLibre map has two explicit camera modes:

- **Navigation view:** follows the delivered foreground GPS location at zoom `17`, pitch `50°`, and a truthful selected bearing.
- **North-up map view:** continues to follow the delivered foreground GPS location, but fixes bearing and pitch to `0°`. The UI action always names the next view.

The app's bottom-right Floating Action Button is padded for navigation/gesture insets. Attribution stays independently visible at the bottom-left. MapLibre's native location component remains responsible for the location puck; no Compose marker or fake camera fixture is used.

## Direction policy

A location update is passed to MapLibre and to the camera controller through the same app-owned `PlatformGpsLocationEngine` callback. The controller uses the following ordered policy:

1. **Course / travel bearing** — preferred only when all are true:
   - `Location.hasBearing()`;
   - speed is at least **1.5 m/s**;
   - the fix is no older than **5 seconds** according to elapsed realtime;
   - where Android supplies bearing accuracy (`hasBearingAccuracy()`), it is at most **35°**.
2. **Fused device heading** — only if a current **`TYPE_ROTATION_VECTOR`** reading is no older than **2 seconds** and reports Android sensor accuracy `MEDIUM` or `HIGH`.
3. **Retained course** — a most-recent reliable course may be held for at most **3 seconds** while stopped or transiently uncertain.
4. **North-up** — if none of the above are credible, the map bearing is `0°`. The status wording says that it is awaiting direction rather than implying a heading.

`Location` bearing is optional, so it is never treated as available solely because a location permission exists. Course is preferred while moving because it describes travel direction rather than the way the phone is held. Future timestamps are rejected rather than treated as fresh.

Freshness is actively reevaluated every 250 ms while the host lifecycle is `STARTED`, even if no GPS or sensor callback arrives. A last rendered source therefore expires at its documented threshold rather than leaving a stale directional claim on-screen.

## Device heading implementation and limits

The implementation uses Android's fused `TYPE_ROTATION_VECTOR` sensor only while the host lifecycle is at least `STARTED`: it registers at `ON_START` and unregisters at `ON_STOP` (and disposal). It remaps the rotation matrix for the current display rotation before calculating azimuth. It deliberately does **not** use:

- accelerometer-only heading (an accelerometer cannot determine yaw); or
- `TYPE_GAME_ROTATION_VECTOR` (Android documents that it excludes geomagnetism and is not north-referenced).

The considered fallback is the documented accelerometer + `TYPE_MAGNETIC_FIELD` rotation-matrix calculation. It can yield magnetic-north azimuth, but needs two raw hardware listeners, depends on magnetic calibration, is rate-limited on modern Android, and is susceptible to interference. Rather than present that lower-confidence fallback as a compass, WIL-76 holds retained-course/north-up whenever the fused rotation-vector sensor is missing or unreliable. This minimizes lifecycle and battery cost while keeping the UI truthful.

A rotation-vector heading is magnetic-north referenced and can still be disturbed by nearby ferrous material, electronics, or poor calibration. Android's own documentation suggests a figure-eight motion for magnetic-sensor calibration. Missing or low-confidence sensors fall through to retained-course/north-up rather than creating a guessed direction. The sensor is foreground-only and uses `SENSOR_DELAY_UI`; no background navigation service is added.

## Smoothing and update bounds

`CircularHeadingSmoother` normalizes every angle to `[0, 360)` and calculates the signed shortest-path delta in `[-180, 180)`. It uses:

- a **3° deadband** to discard minor jitter;
- a **250 ms** minimum update interval;
- exponential circular smoothing (`α = 0.35`) for accepted changes.

Camera animation duration is **500 ms**. The bounded location engine request is one update per second and the smoother avoids uncontrolled sensor-driven animation loops. Unit tests cover wrap-around near north, deadband behavior, update-rate limiting, trustworthy/missing/stale/inaccurate course data, stationary fallback, and device-heading fallback.

## MapLibre integration

MapLibre's native LocationComponent remains enabled with the app-owned direct-GPS `LocationEngine`. Camera positioning is driven with MapLibre `CameraPosition`/`animateCamera`, not a Compose overlay. The native location renderer uses `RenderMode.GPS` only when the selected source is a reliable course; otherwise it uses `NORMAL` so the puck does not claim an unverified direction. This preserves an actual native puck and provides a travel-direction arrow only when Android's course bearing passes the policy.

## Evidence still required

A GitHub-hosted API-35 emulator run must inject a moving sequence *after* the hosted map is fully rendered, then capture both mode screenshots and `dumpsys location` / app-log evidence. Review must confirm map render, attribution, native puck, navigation pitch/bearing/following, north-up bearing/pitch, and the inset-aware FAB. Permission denial/regrant, activity recreation, unavailable rotation-vector sensor, and no-fix behavior also require device coverage.

## Primary sources

- [Android `Location` API](https://developer.android.com/reference/android/location/Location) — locations always include latitude/longitude/time/accuracy; bearing, speed, and bearing accuracy are optional.
- [Android position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position) — fused rotation-vector options, game-rotation-vector north-reference limitation, orientation computation, coordinate remapping, calibration notes.
- [Android sensors overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) — screen-rotation coordinate remapping.
- [MapLibre LocationComponent API](https://maplibre.org/maplibre-native/android/api/-map-libre+-native+-android/org.maplibre.android.location/-location-component/index.html) and [render-mode API](https://maplibre.org/maplibre-native/android/api/-map-libre+-native+-android/org.maplibre.android.location/-location-component/set-render-mode.html) — native location rendering and camera/location modes.
