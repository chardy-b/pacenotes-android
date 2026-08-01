# P0-05 Conservative Curve Classifier Implementation Plan

> **For Hermes:** Execute task-by-task using `subagent-driven-development`; every production behavior follows `test-driven-development` RED → GREEN → REFACTOR.

**Goal:** Convert a normalized local-GPX route into deterministic, conservative curve candidates, while suppressing noise, ambiguous geometry, and non-curve artifacts. This plan does **not** make speed, hazard, surface, visibility, crest, or safety claims.

**Architecture:** Keep all code in the pure Kotlin/JVM `pacenotes` module. `CurveDetector` accepts `NormalizedRoute`, calculates signed heading changes across samples, groups sustained same-direction turns, and produces a typed `CurveDetectionResult`. It is deliberately separate from the `core-model` `Pacenote` event factory: route revision, classifier version, and confidence gating are applied by a later bridge/replay layer. Uncertain observations become typed suppression results rather than permissive events.

**Tech Stack:** Kotlin/JVM 21, `kotlin.test`, existing `GeometryMath` and `NormalizedRoute`; no Android/provider/network/serialization dependencies.

## Provisional, fixture-bound rules

These are algorithm parameters—not driving advice—and must remain explicit in code and tests:

| Parameter | Initial value | Reason |
|---|---:|---|
| Per-step heading-noise floor | `3°` | Ignore GPS/digitization jitter. |
| Minimum accumulated turn | `20°` | Suppress minor bends. |
| Minimum curve length | `15m` | Avoid a one-sample spike. |
| Direction consistency | one sign only | An S-bend splits into independent candidates. |
| Maximum candidate span | `250m` | Suppress broad/ambiguous geometry rather than infer a callout. |
| Severity bands by accumulated turn | `1: ≥100°`, `2: ≥75°`, `3: ≥55°`, `4: ≥40°`, `5: ≥30°`, `6: ≥20°` | Larger numeral remains a *geometry category only*, never speed advice. |

Roundabout-like loops, self-intersection ambiguity, and junction artifacts require a later topology-aware gate. P0-05 must not label them as high-confidence curve events merely because they have heading change; add typed suppression when the simple detector identifies a closed/reversing candidate and keep candidate confidence conservative.

---

### Task 1: Define typed curve-detector output (TDD)

**Files:**
- Create: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetection.kt`
- Create: `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`

**RED:** Specify the wished-for `CurveDirection`, immutable `CurveCandidate` (start/end distance, signed total turn, severity), and `CurveDetectionResult` accepted/suppressed shape. Test invalid candidate values are rejected.

**Run:**
```bash
./gradlew --no-daemon :pacenotes:test --tests '*CurveDetectorTest*'
```
Expected: unresolved references.

**GREEN:** Implement only validated typed output, no detection behavior.

---

### Task 2: Detect a sustained directional curve (TDD)

**Files:** modify the two files above.

**RED fixtures:** Construct metre-indexed synthetic `NormalizedRoute` samples for a straight route and a sustained left turn. Assert straight geometry produces no candidates; the turn produces one left candidate with a geometric severity, and no output mentions speed.

**GREEN:** Add `CurveDetector.detect(route)` with locally computed initial headings and signed heading deltas. Ignore per-step deltas below the noise floor, group contiguous same-sign deltas, and require the minimum turn/length before emitting.

**Verify:** focused test, then `:pacenotes:test`.

---

### Task 3: Add conservative split/suppression behavior (TDD)

**RED fixtures:**
- S-bend yields separated left/right candidates, not one combined callout;
- one noisy heading spike is suppressed;
- a broad/overlong or reversal/loop-like candidate produces typed suppression or no candidate;
- severity band boundaries are deterministic.

**GREEN:** Add only enough grouping/boundary logic to satisfy those fixtures. Do not add a `tightens` modifier, speech phrasing, GPS matching, UI, or speed inference.

---

### Task 4: Review, fixtures, docs, and commit

Run specification review, then code-quality review. Update `PROJECT-STATUS.md`, `BACKLOG.md`, and `SESSION-HANDOFF.md` only after P0-05’s stated fixture suite passes.

**Final verification:**
```bash
./gradlew --no-daemon :pacenotes:test :core-model:test test :app:assembleDebug
git diff --check
```

**Commit:** `feat: detect conservative route curves`
