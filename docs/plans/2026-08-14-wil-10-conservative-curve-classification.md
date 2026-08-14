# WIL-10 Conservative Curve Classification Implementation Plan

> **For Hermes:** Use subagent-driven-development task-by-task. Run spec review before quality review. Use strict TDD for production behavior.

**Goal:** Make the pure route-geometry classifier conservatively distinguish usable curve candidates from straight lines, short noise, and ambiguous shapes without making driving-safety claims.

**Architecture:** `CurveDetector` remains a pure function from `NormalizedRoute` to `List<CurveCandidate>`. A synthetic, versioned test corpus uses app-owned coordinate samples only. The detector groups sustained same-direction heading deltas, ends a group on a material reversal or a neutral run, applies bounded geometry evidence, and maps accepted total turn to a deterministic 1–6 shape label. Severity is a geometry label, not speed advice.

**Tech stack:** Kotlin/JVM, existing `core-model` and `pacenotes` modules, Kotlin test, Gradle.

---

## Global constraints
- Do not add Android, MapLibre, provider SDK, GPS, map tile, GPX parsing, or network dependencies.
- Fixtures are synthetic and non-sensitive; fixture IDs are stable and versioned in source.
- Do not describe speed, hazards, visibility, surface, crests, or jumps.
- Prefer suppression when evidence is short, noisy, discontinuous, or ambiguous.
- True road-junction proximity requires navigation-adapter maneuver data and is explicitly deferred. This ticket only suppresses suspicious geometry patterns.
- Final evidence: focused JVM tests, full `:pacenotes:test`, `:app:assembleDebug`, source/diff hygiene, and Linear evidence comment.

## File and interface map
| Path / component | Responsibility | Consumes | Publishes |
|---|---|---|---|
| `pacenotes/.../CurveDetection.kt` | Stable candidate model | primitive geometry values | validated `CurveCandidate` |
| `pacenotes/.../CurveDetector.kt` | Deterministic segmentation and severity mapping | `NormalizedRoute` | ordered candidate list |
| `pacenotes/.../CurveDetectorTest.kt` | Synthetic acceptance corpus | fixture sample routes | expected candidate behavior |
| `docs/classifier.md` | Human-readable classifier contract | implemented constants/rules | non-driving-safety explanation |

### Published contract
`CurveDetector.detect(route: NormalizedRoute): List<CurveCandidate>` returns candidates in route order. Each accepted candidate has matching direction/sign, strictly increasing route distances, non-zero finite signed turn, and severity 1–6. The function produces no candidate for insufficient/neutral/noisy/ambiguous evidence.

### Task 1: Capture fixture helpers and detector contract
**Objective:** Define readable synthetic-route helpers and acceptance fixtures before changing detection behavior.

**Files:**
- Modify: `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`
- Create: `docs/classifier.md`

**TDD:** Add focused tests for straight, gentle, sharp, S-bend, short zig-zag noise, junction-like right-angle geometry, and roundabout-like cumulative-turn geometry. Run `:pacenotes:test --tests '*CurveDetectorTest*'`; expected RED is failed expectations for missing conservative behavior. No production code change in this task.

### Task 2: Make segmentation bounded and reversal-safe
**Objective:** Ensure contiguous same-direction groups end at neutral geometry or a reversal, and reject inadequate spans.

**Files:**
- Modify: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetector.kt`
- Modify: `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`

**TDD:** First add a failing test for separate left/right candidates in an S-bend and a failing test for short oscillating digitization noise. Implement the smallest grouping change. Verify focused tests green.

### Task 3: Apply conservative shape suppression and severity boundaries
**Objective:** Suppress geometry suspicious for abrupt maneuver/roundabout-like patterns and test deterministic severity boundaries.

**Files:**
- Modify: `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetector.kt`
- Modify: `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`
- Modify: `docs/classifier.md`

**TDD:** Add failing geometry-only cases for junction-like abrupt turn and roundabout-like large cumulative turn, plus exact boundary tests for severity mapping. Implement only deterministic rules based on heading deltas, span, total turn, and length. Verify focused tests green.

### Task 4: Integrate and document evidence
**Objective:** Prove the finished classifier and preserve its operating limits.

**Files:**
- Modify: `docs/PROJECT-STATUS.md`
- Modify: `docs/BACKLOG.md`
- Modify: `docs/SESSION-HANDOFF.md`

**Checks:** Run serially:
```bash
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx512m -Dfile.encoding=UTF-8' -Dorg.gradle.workers.max=1 :pacenotes:test
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx512m -Dfile.encoding=UTF-8' -Dorg.gradle.workers.max=1 :app:assembleDebug
git diff --check
```
Then inspect staged files for generated artifacts before each commit.

## Non-goals
- No live GPS matching, GPX import, map-line ingestion, replay UI, phrases, speech, or event IDs.
- No claim that geometry alone identifies real road junctions.
- No speed or hazard prediction.
