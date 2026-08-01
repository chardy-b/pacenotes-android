# P0-03 Route Progress and Pacenote Domain Implementation Plan

> **For Hermes:** Use `subagent-driven-development` task-by-task and preserve strict `test-driven-development` RED → GREEN → REFACTOR evidence.

**Goal:** Complete the provider-neutral domain boundary for route revisions, matched route positions, navigation progress states, and deterministic pacenote events.

**Architecture:** All new types remain in the pure Kotlin `core-model` module. Android, location-provider, map-provider, network, and serialization dependencies are prohibited. The types represent geometry-derived state only: no speed advice, hazards, or routing decisions.

**Tech Stack:** Kotlin/JVM 21, `kotlin.test`, Gradle.

---

### Task 1: Add validated route revision and matched-position value types

**Objective:** Make the minimum identity and progress coordinates explicit and safe at the app-owned boundary.

**Files:**
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/RouteRevision.kt`
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/MatchedRoutePosition.kt`
- Create: `core-model/src/test/kotlin/com/rich/rallypacenotes/model/NavigationProgressTest.kt`

**Step 1: Write failing tests**

Test that a blank revision is rejected and that a matched position accepts finite, non-negative route distance plus confidence in the inclusive `0.0..1.0` range, while rejecting NaN, infinity, negative distance, and confidence outside the range.

**Step 2: Verify RED**

Run:

```bash
./gradlew --no-daemon :core-model:test --tests '*NavigationProgressTest*'
```

Expected: compilation/test failure because the new types do not exist.

**Step 3: Implement the minimum types**

Use immutable Kotlin value/data types. Do not introduce Android `Location`, timestamps, headings, or matcher algorithms.

**Step 4: Verify GREEN**

Run the focused test again; expected: PASS.

---

### Task 2: Model navigation status and progress invariants

**Objective:** Represent the matcher-facing states required by the accepted GPX architecture without embedding matching policy.

**Files:**
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/NavigationProgress.kt`
- Modify: `core-model/src/test/kotlin/com/rich/rallypacenotes/model/NavigationProgressTest.kt`

**Step 1: Write failing tests**

Test that `MATCHED` requires a `MatchedRoutePosition`, and all non-matched statuses reject a position. Cover `ACQUIRING`, `UNCERTAIN`, `OFF_ROUTE`, `WRONG_WAY`, `AMBIGUOUS`, and `COMPLETED` as explicit enum values.

**Step 2: Verify RED**

Run the focused test; expected: failure because `NavigationProgress`/`NavigationStatus` are absent.

**Step 3: Implement the minimum immutable model**

`NavigationProgress` carries a `RouteRevision`, `NavigationStatus`, and optional matched position. It must not emit speech, calculate confidence, or implement a state machine.

**Step 4: Verify GREEN**

Run the focused test; expected: PASS.

---

### Task 3: Add deterministic, geometry-only pacenote events

**Objective:** Make generated pacenotes reproducibly identifiable across replay runs.

**Files:**
- Create: `core-model/src/main/kotlin/com/rich/rallypacenotes/model/Pacenote.kt`
- Modify: `core-model/src/test/kotlin/com/rich/rallypacenotes/model/NavigationProgressTest.kt`

**Step 1: Write failing tests**

Test that a valid event exposes only immutable route/event inputs and receives the same stable ID for identical inputs. Test that changing any identity input—route ID, route revision, route distance, direction, severity, or classifier version—changes the ID. Test invalid blank IDs/versions, non-finite or negative distance, severity outside `1..6`, and confidence outside `0.0..1.0`.

**Step 2: Verify RED**

Run the focused test; expected: failure because `Pacenote`/`PacenoteDirection` are absent.

**Step 3: Implement the minimum event factory**

Use a private constructor and a `create` factory so identity is derived rather than caller supplied. Canonically encode finite route distance with `Double.toString()` and length-prefix string components; produce a non-secret deterministic ID from that canonical material. Do not use device state, wall-clock time, random IDs, or speech text.

**Step 4: Verify GREEN and relevant full tests**

Run:

```bash
./gradlew --no-daemon :core-model:test
./gradlew --no-daemon test :app:assembleDebug
```

Expected: all tasks succeed.

---

### Task 4: Update portable project records and commit

**Objective:** Leave the work reproducible and accurately documented.

**Files:**
- Modify: `docs/PROJECT-STATUS.md`
- Modify: `docs/BACKLOG.md`
- Modify: `docs/SESSION-HANDOFF.md`
- Create: this plan

**Verification:** `git diff --check`; inspect status/diff; ensure no secrets or generated build files are staged; run the full relevant Gradle verification.

**Commit:** `feat: add route progress and deterministic pacenotes`
