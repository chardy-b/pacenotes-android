# Replay Alpha Route Canvas Plan

> **For Hermes:** Execute each behavior with strict TDD and retain the credential-free/offline MVP boundary.

**Goal:** Add a useful map-like route view to Replay Alpha without introducing map tiles, network access, provider SDKs, credentials, or navigation claims.

**Decision:** Implement an app-owned Compose **route canvas**, not a third-party map. It projects the selected normalized-route fixture into the available canvas bounds, draws the route, highlights the replay position, and marks conservative geometry candidates. It is a local geometry visualization, not a road map or navigation display.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ui/RouteCanvas.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/ui/RouteCanvasProjectionTest.kt`
- Modify: `app/src/main/kotlin/com/rich/rallypacenotes/ReplayAlphaApp.kt`
- Modify: `app/src/androidTest/kotlin/com/rich/rallypacenotes/ReplayAlphaAppInstrumentedTest.kt`

**TDD tasks:**
1. RED: test a pure projection helper maps route bounds into a nonzero padded viewport and preserves point ordering.
2. GREEN: implement the dependency-free projection helper.
3. RED: specify the Canvas semantics label (`Replay route canvas`) and candidate marker label.
4. GREEN: draw route polyline, deterministic replay marker, and geometry-only candidate markers with Compose Canvas.
5. Verify unit tests, compile instrumentation tests, rebuild debug APK, and only run device screenshots when an ADB device/emulator is available.

**Out of scope:** OSM/Google/Mapbox tiles, pan/zoom, destination search, rerouting, road matching, `INTERNET`, provider SDKs, speed/hazard overlays, or any claim that the canvas represents road truth.
