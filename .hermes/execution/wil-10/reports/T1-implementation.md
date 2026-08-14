# WIL-10 T1 implementation report

## Outcome

Repaired dense-sampling curve grouping with conservative pending evidence and a bounded neutral run. With no active group, consecutive same-sign non-zero sub-floor deltas accumulate pending turn/span evidence and start a group only after reaching 3 degrees. Isolated or oscillating sub-floor jitter resets pending evidence. Once active, same-sign sub-floor deltas extend the group; opposite-sign or zero sub-floor samples are neutral, with a maximum of two consecutive neutral samples before the group ends. Material reversals end and restart the group without merging.

## Git state

- Required starting HEAD verified: `a49bc17d6788d8b0c70c099b23314cd3e4939659`.
- Branch: `teo/wil-10-p0-05-curve-classification`.
- Final HEAD: `9102742dc2c06f126301a82b604be1d65dd2dc78` (local commit; no push).
- No push, GitHub, Linear, or Gradle action performed.

## Changed paths

- `pacenotes/src/main/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetector.kt`
- `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`
- `docs/classifier.md`

## Verification

- `git diff --check` — PASS.
- Python static geometry calculation — PASS: dense fixture has 10 route segments, therefore 9 computed deltas at 2.5 degrees each, cumulative turn 22.5 degrees, and 180 m candidate span. Negative fixture has two separated 15-degree sub-noise runs and 3 neutral samples.
- Gradle/JVM tests — intentionally not run per task instruction.

## Acceptance mapping

- Positive dense smooth same-direction fixture requires one right candidate with exact cumulative turn 22.5 degrees and 180 m span despite every computed delta being below 3 degrees.
- Negative genuinely neutral fixture requires no candidate, proving three neutral samples terminate evidence and prevent two separated sub-threshold turns from merging into a false 30-degree candidate.
- Existing reversal, span, abrupt-step, noise, junction-like, roundabout-like, and severity tests remain unchanged.
- Pure Kotlin/JVM geometry-only behavior and no-speed/no-safety/no-provider boundaries remain intact.

## Limitations

The local Gradle test suite was not executed by contract; independent spec/quality review and CI remain required. Report is ignored execution state.
