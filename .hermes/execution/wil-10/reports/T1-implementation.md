# WIL-10 T1 implementation report

## Outcome

Reconciled `teo/wil-10-p0-05-curve-classification` onto local `origin/main` at
`3043546c6045f63f05f7ad58af5c11a306ad38a5`, removed the stale duplicate
`android-verify.yml`, and added direct regression coverage for the abrupt-step
boundary: an exact 60° heading step is accepted while a step greater than 60°
is suppressed.

## Git state

- Base: `3043546c6045f63f05f7ad58af5c11a306ad38a5`
- Final implementation/report HEAD: `d4dd2715fed4e56ab06032d4dde2e5a043e724b3`
- Branch: `teo/wil-10-p0-05-curve-classification`
- Branch is locally ahead of the remote branch and has not been pushed.
- Rebase command: `git rebase origin/main` — completed successfully.

## Changed paths in the implementation commit

- Deleted `.github/workflows/android-verify.yml`; current main's baseline,
  device-gate, and phone-test-release workflows remain authoritative.
- Modified `docs/PROJECT-STATUS.md` to record that the exact-60° boundary
  regression is no longer deferred.
- Modified `pacenotes/src/test/kotlin/com/rich/rallypacenotes/pacenotes/CurveDetectorTest.kt`
  with `acceptsExactSixtyDegreeHeadingStepButSuppressesGreaterStep` and pure
  synthetic geometry fixtures.

The rebased branch also retains the existing WIL-10 detector, test, classifier
contract, and documentation changes from its prior commits. No Android,
provider, network, GPS, speed, hazard, speech, or UI behavior was added.

## Verification

- `git rebase origin/main` — PASS.
- `git diff --check` — PASS before and after staging.
- Static boundary assertion script checking the new test names/fixtures — PASS.
- Staged-file inspection with `git diff --cached --name-status` and
  `git diff --cached --stat` — PASS; no generated artifacts staged.
- No Gradle command was run, per task contract and repository policy.
- No local Android SDK/device verification is claimed.

## Acceptance mapping

- Straight, gentle/sharp, S-bend, digitization-noise, junction-like, and
  roundabout-like synthetic cases remain covered by the existing corpus.
- Same-direction candidate span boundary remains covered at exactly 250 m and
  suppression beyond 250 m.
- Severity exact and just-below numeric boundaries remain covered.
- New abrupt-step regression directly covers accepted exactly 60° versus
  rejected greater-than-60°.
- Detector remains pure Kotlin/JVM and conservative; geometry labels do not
  imply speed, hazard, surface, visibility, crest, or jump information.

## Limitations and readiness

Gradle/JVM tests were intentionally not executed locally. Existing remote CI
results recorded on the pre-reconciliation branch are not re-asserted as
verification of this rebased head. The branch is ready for independent spec
review, with local static checks complete and the exact CI/build verification
left to the independent reviewer or remote CI.
