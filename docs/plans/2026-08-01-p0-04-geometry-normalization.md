# P0-04 Geometry Normalization and Primitives Implementation Plan

> **For Hermes:** Use `subagent-driven-development` task-by-task and preserve strict `test-driven-development` RED → GREEN → REFACTOR evidence.

**Goal:** Establish a pure, deterministic geometry foundation for conservative GPX pacenote classification without making any driving, routing, or safety claims.

**Architecture:** Add a Kotlin/JVM-only `pacenotes` module that depends only on `core-model`. First provide well-tested spherical distance and heading primitives. Then introduce a normalizer which generates a distance-indexed, evenly sampled route or a typed suppression result; curve detection stays explicitly out of this P0-04 work.

**Tech Stack:** Kotlin/JVM 21, `kotlin.test`, Gradle; no Android/provider/network/serialization dependencies.

---

### Task 1: Create the pure `pacenotes` module

**Objective:** Make the algorithm boundary explicit and depend only on provider-neutral core models.

**Files:**
- Modify: `settings.gradle.kts`
- Create: `pacenotes/build.gradle.kts`

**Steps:**
1. Include `:pacenotes` in Gradle settings.
2. Apply `org.jetbrains.kotlin.jvm`, configure JDK 21, add `implementation(project(":core-model"))` and `testImplementation(kotlin("test"))`.
3. Verify the module is visible with `./gradlew --no-daemon :pacenotes:tasks`.

This is generated build configuration, so it is the explicit non-behavioral exception to TDD.

---

### Task 2: Add meter-distance primitive (TDD)

**Objective:** Compute deterministic great-circle distance between two `GeoPoint` values.

**Files:**
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/GeometryMath.kt`
- Create: `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/GeometryMathTest.kt`

**Step 1 — RED:** Test identical points yield `0.0` metres and one degree of longitude on the equator is within `111_000.0..112_000.0` metres.

**Step 2 — Verify RED:**
```bash
./gradlew --no-daemon :pacenotes:test --tests '*GeometryMathTest*'
```
Expected: compilation failure because `GeometryMath` is absent.

**Step 3 — GREEN:** Implement `distanceMeters(from, to)` using the haversine formula with documented mean Earth radius `6_371_008.8` metres.

**Step 4 — Verify GREEN:** Run the focused test; expected PASS.

---

### Task 3: Add normalized initial heading and signed turn primitives (TDD)

**Objective:** Provide wrap-safe angular inputs for future curve analysis without interpreting them as route advice.

**Files:**
- Modify: `GeometryMath.kt`
- Modify: `GeometryMathTest.kt`

**Step 1 — RED:** Test that an eastbound equatorial segment has heading near `90°`; a northbound segment near `0°`; and a signed delta from `350°` to `10°` is `+20°` while `10°` to `350°` is `-20°`.

**Step 2 — Verify RED:** Run the focused test and require an unresolved-reference failure.

**Step 3 — GREEN:** Add `initialHeadingDegrees(from, to)` normalized into `[0, 360)` and `signedHeadingDeltaDegrees(from, to)` normalized into `[-180, 180]`. Reject identical points for heading because it is undefined; keep this function free of Android or vehicle semantics.

**Step 4 — Verify GREEN:** Run focused tests and then `:pacenotes:test`.

---

### Task 4: Add distance-indexed route normalization (future P0-04 slice)

**Objective:** Convert a valid `RouteGeometry` into predictable metre-spaced samples for classification.

**Files:**
- Create: `NormalizedRoute.kt`, `RouteNormalizer.kt`, focused tests.

**Required behavior:** configurable finite positive sample interval (initial hypothesis: `5.0`m); retain start/end; attach monotonic cumulative distance; reject/suppress segments beyond an explicit discontinuity threshold; never create a curve event.

**Verification:** focused RED→GREEN tests for duplicate behavior, interpolation, endpoint preservation, density, and discontinuity suppression.

---

### Task 5: Finish P0-04 records and commit

**Files:**
- Modify: `docs/PROJECT-STATUS.md`, `docs/BACKLOG.md`, `docs/SESSION-HANDOFF.md`
- Create: this plan

**Verification:**
```bash
./gradlew --no-daemon :pacenotes:test :core-model:test test :app:assembleDebug
git diff --check
```

**Commit:** `feat: add pacenote geometry primitives`
