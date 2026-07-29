# Open-Stack GPX Pacenotes MVP Implementation Plan

> **For Hermes:** Use `subagent-driven-development` only after this plan is accepted; execute each task with strict `test-driven-development` RED → GREEN → REFACTOR cycles.

**Goal:** Deliver an installable Android field-test **GPX route-following pacenote companion** that imports a tester-supplied GPX route, tracks or simulates progress along it, and speaks conservative geometry-based pacenotes—without Mapbox, Google Maps, API credentials, or network routing/map-tile dependencies.

**Architecture:** The app does **not** perform ordinary navigation, destination search, road-network map matching, or rerouting. It parses a local GPX track into provider-neutral route geometry, qualifies its geometry, computes deterministic curve events in a pure Kotlin `pacenotes` module, and statefully matches Android framework `LocationManager` fixes to the loaded route. Ambiguous, low-confidence, wrong-way, or off-route conditions pause speech rather than inventing a reroute. A Compose look-ahead route canvas renders app-owned geometry only. A service-owned Android `location` foreground service owns active GPS, matching, scheduling, TTS, and audio focus; the UI binds to session state. No V1 route/location data is sent to a server.

**Tech Stack:** Kotlin 2.0, Gradle Kotlin DSL, Android SDK 35, Jetpack Compose/Material 3, Android framework `LocationManager`, Android `TextToSpeech`, Android foreground services/audio focus, a named hardened GPX pull parser, JUnit/Kotlin test, and Compose UI tests. V1 must declare no `INTERNET` permission and has no Google Play Services dependency.

## Sol-reviewed authoritative amendments — 2026-07-26

These amendments override any conflicting wording or ordering later in this plan.

1. **Product boundary:** Call V1 a *GPX route-following pacenote companion*. The tester independently knows and validates the route; GPX geometry is not proof of legal access, current road conditions, road direction, lane position, or hazard information.
2. **Input qualification:** GPX import must reject or clearly mark unsupported/multi-segment input; it may not silently use only the first track segment. The chosen parser must have documented namespace support, DTD/external-entity protections, licenses, and bounds for bytes, XML depth, points, segments, distance, gaps, and geometry density. Live GPS guidance requires an accepted route-quality result; replay can expose rejected-route diagnostics.
3. **Route matching:** Do not globally snap each fix to the nearest segment. Score candidates near prior progress using elapsed realtime, lateral distance, reported accuracy, bearing, speed, plausible forward/backward movement, and stabilization history. Model `ACQUIRING`, `MATCHED`, `UNCERTAIN`, `OFF_ROUTE`, `WRONG_WAY`, `AMBIGUOUS`, and `COMPLETED`; use silence/rejection around crossings, parallel roads, hairpins, loops, and retraced geometry that cannot be resolved. Add a bounded segment search/spatial index.
4. **Classifier:** Define severity as a route-geometry category, never a safe-speed or reconnaissance rating. Events expose entry, apex/reference, exit, length, heading change, radius statistic, confidence reasons, and suppression reasons. Start physical testing with spoken left/right/severity only; generate `tightens` as diagnostic data until fixtures and field evidence validate it.
5. **Execution order:** Build deterministic GPX qualification → classifier → replay trace with fake speaker **before** live matching, foreground service, or rich active-navigation UI.
6. **Android lifecycle:** The service owns active location, matcher, scheduler, TTS, and audio focus. It starts only from a visible user action after precise-location permission. It declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, and `android:foregroundServiceType="location"`; it provides a persistent notification with Stop/Pause. `OFF_ROUTE` pauses speech but retains location for reacquisition; only explicit stop, completion, fatal error, or documented abandonment timeout ends the service. Do not request background location for V1 unless a verified restart flow requires it.
7. **Data/licensing:** Disable backup or exclude imported routes, derived events, session state, and location traces. Fixtures must be original or carry documented source, license, attribution, and redistribution permission. Add no generic OSM attribution unless OSM-derived data is actually displayed or bundled.
8. **Field evidence:** Record wrong direction/severity, false/missed/nuisance calls, timing error, pause duration, expected notes, and comprehension/usefulness ratings. Three to five GPX routes are an integration gate, not a classifier-validation corpus.

---

## Why this is the correct open-stack MVP

### Chosen V1: GPX-guided navigation, no hosted routing/map dependency

- **Input:** a GPX route created or downloaded by the tester and imported from Android’s Storage Access Framework.
- **Guidance:** the app plays pacenotes based on the GPX geometry and approximate GPS progress along that route.
- **Visualization:** an app-owned `RouteCanvas`, not a third-party online basemap. It must show north-up route geometry, position/progress, next curve, and off-route status clearly.
- **Testing:** a built-in replay source follows the same route-progress pipeline as live location, making classifier/speech tuning safe and deterministic.
- **Safety:** if matching confidence is low or the device is meaningfully off the GPX track, mute pacenotes; do not invent reroutes or speed guidance.

This reduces the MVP from “build a general road-navigation service” to the actual product-risk question: **are high-confidence rally-style curve callouts useful, understandable, and conservative?** It is field-testable without API provisioning or an external routing SLA.

### Deferred adapter boundary

Create `NavigationSource` in app-owned code. The first implementation is `GpxNavigationSource`; later candidates can be:

1. **Self-hosted Valhalla or GraphHopper** for route calculation/map matching/rerouting; this needs an operated backend and OSM data lifecycle.
2. **MapLibre Native + a licensed/self-hosted or bundled tile source** for a full basemap.
3. A deliberately selected commercial provider if product economics later justify it.

Do **not** ship an app depending on the public `tile.openstreetmap.org` service or OSRM demo endpoint: they are community/demo services, not a production navigation backend. Do not put MapLibre into the first build merely to fetch those public services. A later offline map phase must package or license a regional MBTiles/PMTiles source and show OSM attribution.

---

## Explicit V1 acceptance criteria

1. A tester can select a local `.gpx` file, see its name/distance/point count, and start a replay or GPS-guided session.
2. The same GPX + classifier version + driver profile always produces the same ordered pacenote IDs and phrases.
3. The app identifies significant high-confidence left/right curves and may label a high-confidence tightening curve; it suppresses minor bends, malformed geometry, low-confidence matches, and off-route positions.
4. Replay at a selectable simulated speed visibly advances through the route and emits the same scheduled phrases tested on the JVM.
5. Live GPS mode shows a persistent foreground notification, uses Android audio focus/TTS, and stops/mutes route guidance when off route.
6. The active screen is readable at 200% font scale and does not require touch interaction while moving.
7. `./gradlew :app:assembleDebug`, JVM tests, and Compose UI tests pass; an APK is produced without credentials or secret-bearing files in Git.

### Explicitly not in V1

- Destination search, address geocoding, route calculation, traffic, automatic rerouting, ordinary turn-by-turn instructions, and offline road-map tiles.
- Speed recommendations, crest/hazard/surface/visibility claims, Android Auto, accounts, telemetry backends, or ML classification.
- Public-OSM-tile or OSRM-demo-service dependence.

---

## Repository context and constraints

- Root: `/home/hermes/rally-pacenotes-android`
- A partial Android/Compose scaffold exists: `app/`, `core-model/`, root Gradle files, wrapper, and ignored `local.properties`.
- The previous Gradle wrapper/build was interrupted, so its result is **unknown**. First execution must inspect state; do not assume it built.
- Local development toolchain exists under `/home/hermes/.local/opt/temurin-21` and `/home/hermes/.android-sdk`; the environment has roughly 1.9 GiB RAM. Use `--no-daemon` and `-Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8'` for verification if the default daemon is unstable.
- Existing project documents still describe Mapbox. The execution must supersede that architecture in an ADR and update the charter/status/backlog before any product claim says the open stack is live.
- Every production behavior is test-first. Generated Gradle/Android configuration is the only non-behavioral exception.
- Keep package namespace `com.rich.rallypacenotes` unless the user directs otherwise.

---

## Target repository layout

```text
rally-pacenotes-android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/rich/rallypacenotes/
│       │       ├── MainActivity.kt
│       │       ├── navigation/GpxNavigationSource.kt
│       │       ├── navigation/LocationRouteMatcher.kt
│       │       ├── navigation/NavigationForegroundService.kt
│       │       ├── speech/AndroidPacenoteSpeaker.kt
│       │       ├── speech/PacenoteScheduler.kt
│       │       └── ui/
│       │           ├── RallyApp.kt
│       │           ├── RouteCanvas.kt
│       │           ├── NavigationScreen.kt
│       │           └── theme/
│       ├── test/kotlin/.../
│       └── androidTest/kotlin/.../
├── core-model/
│   └── src/
│       ├── main/kotlin/com/rich/rallypacenotes/model/
│       └── test/kotlin/com/rich/rallypacenotes/model/
├── pacenotes/
│   └── src/
│       ├── main/kotlin/com/rich/rallypacenotes/pacenotes/
│       └── test/kotlin/com/rich/rallypacenotes/pacenotes/
├── test-fixtures/
│   └── src/main/resources/routes/
├── docs/
│   ├── classifier.md
│   ├── gpx-input.md
│   ├── field-test-protocol.md
│   └── open-stack-architecture.md
└── .hermes/plans/
```

`core-model` and `pacenotes` must remain pure Kotlin/JVM modules with no Android, Maps, MapLibre, or provider types. `app` owns Android framework integration and renders only app-owned models.

---

## Execution plan

### Task 1: Recover and verify the existing scaffold

**Objective:** Establish a known-good, low-memory Android build before adding product behavior.

**Files:**
- Inspect: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `core-model/build.gradle.kts`, `.gitignore`, `local.properties`
- Modify only if needed: Gradle configuration and `.gitignore`

**Step 1 — inspect interrupted state**

Run:
```bash
export JAVA_HOME=/home/hermes/.local/opt/temurin-21
export ANDROID_HOME=/home/hermes/.android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8' :app:assembleDebug
```

Expected: either a successful debug APK or a concrete Gradle/SDK error. Do not overwrite or delete project files before recording the failure.

**Step 2 — repair only the observed configuration error**

Keep `local.properties` ignored; track only non-secret `gradle.properties`. Ensure `.gitignore` ignores `gradle.local.properties`, signing stores, `.env*`, and build directories, but does **not** ignore the tracked base `gradle.properties`.

**Step 3 — verify build and Git safety**

Run:
```bash
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8' :app:assembleDebug
git check-ignore -v local.properties gradle.local.properties
git status --short
```

Expected: build succeeds; `local.properties` and `gradle.local.properties` are ignored; no token configuration exists.

**Step 4 — documentation state**

Update `docs/PROJECT-STATUS.md` only after the build result is verified.

**Commit:** `chore: establish reproducible Android scaffold`

---

### Task 2: Record the open-stack architectural supersession

**Objective:** Replace the blocked Mapbox runtime decision with a credential-free, GPX-guided MVP without pretending the app has general routing.

**Files:**
- Modify: `docs/DECISIONS.md`, `docs/PROJECT-CHARTER.md`, `docs/BACKLOG.md`, `docs/PROJECT-STATUS.md`, `README.md`
- Create: `docs/open-stack-architecture.md`

**Step 1 — add an ADR**

Append `ADR-008 — GPX-guided, credential-free open-stack MVP` to `docs/DECISIONS.md`:

```markdown
- **Status:** Accepted
- **Decision:** Replace Mapbox runtime integration in the first field-test build with local GPX import, app-owned route matching/replay, Compose route rendering, Android location, and TTS.
- **Why:** Mapbox credentials are unavailable; the product hypothesis can be tested without hosted routing or map tiles.
- **Consequence:** V1 has no destination search, automatic rerouting, or basemap. A future routing provider remains behind `NavigationSource`.
- **Supersedes:** ADR-001 for V1 runtime only; ADR-002 and ADR-003 remain accepted.
```

**Step 2 — explicitly preserve safety constraints**

Update scope and backlog acceptance text to say “off-route suppression” rather than “reroute.” State that GPX files must be tested by a passenger and are a guidance reference, not authoritative navigation.

**Step 3 — document the hosting boundary**

`docs/open-stack-architecture.md` must state:
- GPX/route geometry remains local to the device;
- no route geometry is sent to a server in V1;
- public OSM raster tiles and OSRM demo service are prohibited dependencies;
- later full-map work needs a licensed/self-hosted/bundled source with required OSM attribution;
- all provider adapters yield app-owned models.

**Verification:** markdown link check or manual inspection; `git diff --check`.

**Commit:** `docs: define credential-free open-stack MVP`

---

### Task 3: Add the pure route and pacenote domain model (TDD)

**Objective:** Specify the stable data boundary with no Android/provider imports.

**Files:**
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/GeoPoint.kt`
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/RouteGeometry.kt`
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/NavigationProgress.kt`
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/Pacenote.kt`
- Create: matching unit tests under `core-model/src/test/kotlin/com/rich/rallypacenotes/model/`

**Step 1 — RED: write one model-invariant test**

```kotlin
class RouteGeometryTest {
    @Test
    fun `route requires at least two distinct ordered points`() {
        assertFailsWith<IllegalArgumentException> {
            RouteGeometry(id = "sample", points = listOf(GeoPoint(45.0, -122.0)))
        }
    }
}
```

Run:
```bash
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8' :core-model:test --tests '*RouteGeometryTest*'
```
Expected: FAIL because the model does not exist.

**Step 2 — GREEN: implement the smallest immutable types**

Use value classes/data classes and validate latitude/longitude ranges, nonblank IDs, at least two distinct points, and non-decreasing `routeDistanceMeters`. Do not add serialization or a generic provider abstraction yet.

**Step 3 — repeat TDD slices**

Add one test then one minimal implementation for:
- `MatchedRoutePosition` with a nonnegative distance and `matchConfidence` constrained to `0.0..1.0`;
- `RouteRevision` ID;
- `PacenoteDirection` (`LEFT`, `RIGHT`), severity `1..6`, confidence `0.0..1.0`;
- `Pacenote` stable ID derived from route ID, revision, route distance, direction, severity, and classifier version.

**Step 4 — full module check**

Run `:core-model:test`; output must be warning-free.

**Commit:** `feat: add provider-neutral route domain`

---

### Task 4: Parse and validate GPX routes locally (TDD)

**Objective:** Turn a user-selected GPX file into `RouteGeometry` without network access.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/navigation/GpxRouteParser.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/navigation/GpxRouteParserTest.kt`
- Create: `test-fixtures/src/main/resources/routes/gentle-left.gpx`
- Create: `test-fixtures/src/main/resources/routes/malformed.gpx`
- Create: `docs/gpx-input.md`

**Step 1 — RED: supported `<trkpt>` extraction**

```kotlin
@Test
fun `parses track points in document order`() {
    val route = parser.parse(fixture("gentle-left.gpx"), routeId = "gentle-left")
    assertEquals(5, route.points.size)
    assertEquals(45.000000, route.points.first().latitude, 0.000001)
}
```

Run the focused JVM test and confirm it fails because `GpxRouteParser` is absent.

**Step 2 — GREEN: secure minimal streaming parser**

Use an XML pull parser with external entity processing disabled. Support GPX `<trk>/<trkseg>/<trkpt lat="…" lon="…">` only. Reject unavailable/invalid coordinates, fewer than two usable distinct points, and route discontinuities larger than an explicit initial threshold (e.g., 1 km).

**Step 3 — repeat TDD slices**

- Prefer the first non-empty track segment; report a warning/status when a file includes unsupported waypoints/routes.
- Normalize duplicate adjacent points before constructing `RouteGeometry`.
- Give parse errors an app-safe message without echoing full file contents.
- Test malformed XML, invalid coordinate, one-point route, duplicate adjacent point, and discontinuity behavior.

**Step 4 — Android document import wiring**

Add `ActivityResultContracts.OpenDocument` handling in `MainActivity.kt` only after the parser is covered. Accept `application/gpx+xml`, `application/xml`, and `text/xml`; treat MIME types as hints and validate content. Persist URI permission when available.

**Verification:** `:app:test`; manually select a fixture on an emulator/device.

**Commit:** `feat: import GPX routes locally`

---

### Task 5: Implement deterministic geometry normalization and curve detection (TDD)

**Objective:** Convert route geometry into conservative, explainable curve events.

**Files:**
- Create module: `pacenotes/build.gradle.kts`
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/GeometryMath.kt`
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/RouteNormalizer.kt`
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetector.kt`
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/PacenoteGenerator.kt`
- Create: focused test files and GPX/Kotlin fixtures under `pacenotes/src/test/`
- Create: `docs/classifier.md`

**Step 1 — RED/GREEN: meter primitive**

Write a test proving distance between two nearby lat/lon points has the expected approximate meter range. Implement a stable haversine (or documented local tangent-plane) function. Do not compute headings in raw degrees without projection/wrapping tests.

**Step 2 — RED/GREEN: route normalization**

Test and implement:
- cumulative route distance;
- resampling at a fixed 5 m interval;
- removal of duplicate points;
- no event from fewer than 3 valid samples;
- discontinuity failure/suppression.

**Step 3 — RED/GREEN: curve candidates**

Create separate fixtures/tests for:
- straight route → no event;
- sustained gentle left → one `LEFT` event;
- sustained sharp right → one `RIGHT` event;
- a short digitization zig-zag → no event;
- opposing S-bend → distinct ordered events;
- curve with clear declining radius → eligible `TIGHTENS` modifier;
- sparse/noisy geometry → suppression, never invented confidence.

Implement heading-change windows, same-direction segmentation, min total-heading/min-length gates, and a conservative provisional severity map. Record all numeric thresholds in `docs/classifier.md` as field-test hypotheses, not safe-speed values.

**Step 4 — deterministic event IDs**

Test that repeated generation with the same input/profile/classifier version has byte-for-byte equivalent IDs and phrase inputs.

**Verification:** `:pacenotes:test`, then all JVM tests.

**Commit:** `feat: generate conservative GPX pacenotes`

---

### Task 6: Map GPS fixes to the loaded route and suppress unsafe guidance (TDD)

**Objective:** Provide a conservative live-progress signal without pretending to perform road-network map matching.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/navigation/LocationRouteMatcher.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/navigation/GpxNavigationSource.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/navigation/LocationRouteMatcherTest.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/navigation/GpxNavigationSourceTest.kt`

**Step 1 — RED: nearest-segment projection**

Test a point beside a simple two-segment route. Expected result: a projected along-route distance within a tolerance, lateral error in meters, and an ordered progress value.

**Step 2 — GREEN: minimal matcher**

Project each incoming location to the closest local route segment; return `MatchedRoutePosition` with lateral error, monotonic route distance only when confidence is adequate, and a score based on lateral error, accuracy radius, and plausibility of progress jump.

**Step 3 — RED/GREEN: confidence state machine**

Test:
- accurate on-route location → `MATCHED`;
- inaccurate fix (accuracy too large) → `UNCERTAIN`, no speech;
- persistent lateral error beyond a documented threshold → `OFF_ROUTE`, no speech;
- a later reliable near-route fix → resume after an explicit stabilization window.

The app must show `Off route — guidance paused` rather than “rerouting.”

**Step 4 — source interface**

Define a small app-owned interface:

```kotlin
interface NavigationSource {
    val route: StateFlow<RouteGeometry?>
    val progress: StateFlow<NavigationProgress>
    suspend fun begin(route: RouteGeometry)
    suspend fun stop()
}
```

`GpxNavigationSource` receives `Location` fixes from a replaceable `LocationFixSource`, enabling JVM fakes. Android’s Fused Location Provider stays behind that interface.

**Verification:** focused matcher/source tests, then `:app:test`.

**Commit:** `feat: match device location to loaded GPX route`

---

### Task 7: Build the speech phrase builder and route-revision scheduler (TDD)

**Objective:** Announce the right high-confidence note once, at an adaptive time, and mute all stale/off-route prompts.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/speech/PacenotePhraseBuilder.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/speech/PacenoteScheduler.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/speech/AndroidPacenoteSpeaker.kt`
- Create: corresponding tests under `app/src/test/`

**Step 1 — RED/GREEN: phrase grammar**

Test exact outputs such as:
- `In 250 meters, left four.`
- `Right four tightens to two.`
- low confidence / off-route → no phrase.

Use explicit words (“left”, “right”), never color-only semantics, and no speed/hazard claims.

**Step 2 — RED/GREEN: scheduling policy**

Inject a monotonic clock. Test an initial call target based on 8–15 seconds of ETA, bounded by a conservative minimum/maximum distance. Test de-duplication and no queue overlap.

**Step 3 — RED/GREEN: cancellation**

Test that changing route revision, entering `UNCERTAIN`, or entering `OFF_ROUTE` clears queued but unsaid notes. Test that a resumed navigation state does not replay already-spoken IDs.

**Step 4 — Android speaker**

Adapt `TextToSpeech` only after pure tests pass. Request audio focus immediately before speaking; expose initialization/failure state; never log route data or audio text in release logging.

**Verification:** `:app:test`; replay a fixture on device/emulator and capture only non-sensitive test output.

**Commit:** `feat: schedule GPX pacenote guidance`

---

### Task 8: Implement the active Compose UI and route canvas (TDD)

**Objective:** Make GPX-guided use glanceable without relying on an online map.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ui/RallyApp.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ui/NavigationScreen.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ui/RouteCanvas.kt`
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/ui/NavigationViewModel.kt`
- Create: theme files under `app/src/main/kotlin/com/rich/rallypacenotes/ui/theme/`
- Create: Compose tests under `app/src/androidTest/`

**Step 1 — RED: semantic active-note state**

Write a Compose semantics test that expects `NEXT CURVE`, explicit direction text, severity, and distance when a note is available. It must expect a text status (not only a color) for `OFF_ROUTE`.

**Step 2 — GREEN: minimal screen**

Implement four mutually exclusive states:
1. no route selected → `IMPORT GPX ROUTE`;
2. route loaded, not started → summary + `START REPLAY` / `START GPS`;
3. active/matched → route canvas, large next-pacenote card, state label, stop;
4. uncertain/off-route → prominent `GUIDANCE PAUSED` status with safe stop action.

**Step 3 — RED/GREEN: route canvas behavior**

Unit-test coordinate normalization separately from the composable. Render the route as a padded north-up polyline and current matched position. Route geometry and text must remain readable without a basemap.

**Step 4 — accessibility tests**

Test at a 2.0 font scale that direction/severity/distance nodes exist and are not clipped. Direction must be arrow + text. Keep decorative grid/texture out of the live route and instruction areas.

**Verification:** `:app:connectedDebugAndroidTest` on an emulator/device when available; otherwise run Compose unit/robolectric-compatible tests and record that physical rendering remains pending.

**Commit:** `feat: add offline GPX navigation interface`

---

### Task 9: Add deterministic replay before live tracking (TDD)

**Objective:** Prove the entire classifier → progress → scheduler path safely and repeatably.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/navigation/ReplayLocationFixSource.kt`
- Create: `app/src/test/kotlin/com/rich/rallypacenotes/navigation/ReplayLocationFixSourceTest.kt`
- Create: `test-fixtures/src/main/resources/routes/field-loop.gpx`
- Create: `docs/field-test-protocol.md`

**Step 1 — RED/GREEN: replay positions**

Given a route and a simulated speed, test that replay emits ordered location fixes with deterministic timestamps and completes at the route endpoint.

**Step 2 — RED/GREEN: end-to-end event trace**

With a fake speaker and monotonic fake clock, test that `field-loop.gpx` produces the expected ordered phrase IDs and no duplicates.

**Step 3 — UI controls**

Add development-only speed controls (e.g., 1×/5×/20×), a route reset action, and a clearly labeled `REPLAY — DO NOT DRIVE` state. Do not expose debug tuning controls during GPS-guided mode.

**Verification:** all JVM tests; manually run replay and compare on-screen events to the fixture expectation.

**Commit:** `feat: add deterministic route replay`

---

### Task 10: Add foreground location/audio execution and perform a field-test gate

**Objective:** Make the installed build usable with its display off and document its exact limitations.

**Files:**
- Create: `app/src/main/kotlin/com/rich/rallypacenotes/navigation/NavigationForegroundService.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `GpxNavigationSource.kt`, `AndroidPacenoteSpeaker.kt`, `NavigationViewModel.kt`
- Modify: `docs/field-test-protocol.md`, `docs/PROJECT-STATUS.md`, `README.md`

**Step 1 — Android requirements discovery**

Before implementation, check the current Android documentation for target SDK 35 foreground-service types and permission requirements. Record only the required permissions and user-facing rationale.

**Step 2 — TDD lifecycle coordinator**

Keep service lifecycle policy in a testable coordinator first: start only for active GPS navigation, stop on explicit end/off-route terminal session/error, and maintain a visible notification. Then write the framework `Service` adapter.

**Step 3 — audio focus**

Test focus request/release through an injected interface. The Android adapter requests a navigation-appropriate transient/ducking policy only when speaking and releases it immediately after utterance completion or failure.

**Step 4 — physical field gate**

Use 3–5 GPX routes with a passenger/tester. Validate:
- route import and replay while stationary;
- screen-off operation and foreground notification;
- TTS through phone and Bluetooth;
- correct silence when off route or location accuracy degrades;
- every wrong/nuisance/missed callout recorded by route distance.

Do not operate the test interface while driving. Tune thresholds via regression fixtures first, replay second, then repeat field validation.

**Verification:** full `./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8' test :app:assembleDebug`, plus physical-device checklist results in `docs/field-test-protocol.md`.

**Commit:** `feat: support foreground GPX guidance`

---

## Final validation and release checklist

```bash
export JAVA_HOME=/home/hermes/.local/opt/temurin-21
export ANDROID_HOME=/home/hermes/.android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8' clean test :app:assembleDebug
git diff --check
git status --short
git ls-files | grep -Ei '(local\.properties|gradle\.local\.properties|\.env|token|\.jks|\.keystore)' && exit 1 || true
```

Required before calling the MVP complete:

- all new production behavior observed failing then passing under a focused test;
- all modules pass together;
- debug APK exists and installs on one physical Android device;
- route replay trace equals fixture expectations;
- safety and open-stack limitations appear in the app/README/field-test protocol;
- no secret, API key, signing file, or local SDK path is tracked;
- the project docs state clearly that V1 is GPX-guided navigation, **not** address-to-address routing.

---

## Risks and decisions after field validation

| Risk | Mitigation in this plan |
|---|---|
| GPX differs from real drivable road route | Start with known/reviewed GPX tracks; show off-route pause; never claim a reroute. |
| GPS drift triggers false prompts | Accuracy/lateral-error confidence gates, stabilization window, silence by default. |
| No basemap is less familiar | Make the route canvas explicit, directional, and limited to the next-guidance task; add offline tiles only after classifier value is validated. |
| GPX geometry has poor density | Reject/suppress poor input; record resolution/quality; resample; preserve provenance of test fixtures. |
| User reads severity as speed | No speed language, safety notice, passenger-only test protocol, conservative labels. |
| Open routing later becomes an infrastructure project | Keep `NavigationSource`/core models independent; evaluate self-hosted Valhalla/GraphHopper only after MVP evidence justifies it. |

## Token-budget estimate

- Scaffold recovery + documentation supersession: **8k–14k tokens**
- Domain / GPX import / geometry classifier: **45k–75k tokens**
- GPS matching / speech / UI / replay: **55k–90k tokens**
- Foreground integration + physical field-test iteration: **35k–70k tokens**
- **Total to a first credential-free field-test MVP: 143k–249k tokens**, excluding human-driven road testing and any future offline basemap package work.
