# Product Backlog

## Operating rules

- Work top to bottom unless a blocker prevents it.
- Every code item uses strict TDD.
- Estimates are **approximate model-token budgets**, not calendar commitments.
- A task is done only when its stated verification has actually run and its documentation is updated.

## Now — critical path

| ID | Task | Dependency | Token budget | Done when |
|---|---|---|---:|---|
| P0-01 | Scaffold Kotlin/Compose Android project; add Git-safe ignore/config files | None | 8k–15k | `./gradlew :app:assembleDebug` succeeds with no secrets tracked. |
| P0-02 | Record the credential-free GPX runtime decision; update charter/status/safety wording | P0-01 | 4k–8k | ADR, scope, and README describe a GPX route-following companion rather than ordinary navigation. |
| P0-03 | ✅ Create provider-neutral route/progress/pacenote domain models | P0-01 | 10k–18k | Completed 2026-08-01: unit tests cover model invariants and deterministic IDs. |
| P0-04 | ✅ Implement geometry normalization and distance/heading primitives | P0-03 | 16k–28k | Completed 2026-08-01: pure geometry module, stable meter projection, heading primitives, fixed-distance resampling, and discontinuity suppression are unit-tested. |
| P0-05 | ✅ Build conservative curve detection/classification | P0-04 | 28k–45k | Completed 2026-08-14 classifier implementation and synthetic evidence; `:pacenotes:test` passed. The required debug APK build is pending because this environment has no configured Android SDK, so WIL-10 documentation is not yet accepted as fully verified. |
| P0-06 | Add stateful GPX location matcher and route-revision handling | P0-03 + P0-04 + P0-05 | 20k–35k | Simulated route-following session produces neutral progress; ambiguity/off-route pauses speech without invented rerouting. Blocked until WIL-10 final verification is accepted; not yet unblocked. |
| P0-07 | Implement original Compose theme and static navigation screen | P0-01 + P0-03 | 20k–35k | UI/semantics tests cover pacenote card, state bar, 200% font scale, and direction not-color-only. |
| P0-08 | Implement phrase builder, speech scheduler, and TTS runtime | P0-03 + P0-06 | 18k–30k | Scheduler tests and device simulation demonstrate timely, non-duplicate calls. |
| P0-09 | Add foreground navigation service and audio focus | P0-06 + P0-08 | 16k–28k | Physical-device screen-off/Bluetooth/audio-focus checklist passes. |
| P0-10 | Build replay/debug tooling and route fixture corpus | P0-04 + P0-05 + P0-07 | 20k–35k | Same fixture/version always produces same event IDs and phrases. |
| P0-11 | Internal field test and threshold calibration | P0-05 through P0-10 | 20k–40k | Field-test report, tuned fixtures, and accepted known limitations exist. |

## Next — after MVP gates pass

| ID | Task | Gate | Token budget |
|---|---|---|---:|
| P1-01 | GPX import/export and saved drives | MVP classifier + UX gate | 18k–32k |
| P1-02 | Offline regional-download UX | MVP navigation/runtime gate | 20k–40k |
| P1-03 | Android Auto presentation | MVP active-navigation UX gate | 28k–50k |
| P1-04 | User-facing calibration profiles | Field-test evidence supports different preferences | 16k–30k |
| P1-05 | Curvy-road discovery/routing spike | Product demand + data-control decision | 35k–70k |

## Not planned until explicitly re-evaluated

- Social route sharing/accounts.
- Machine-learning classifier.
- Global self-hosted OSM/Valhalla infrastructure.
- Road-hazard claims or speed recommendation features.

## Definition of ready

A backlog item may start only when:

1. Its dependencies are complete or explicitly waived.
2. Its source inputs are available (for example Mapbox token or field-test region).
3. Test strategy and exact files/modules are written before production code.
4. The task does not silently expand MVP scope.

## Definition of done

1. Focused tests failed first and then pass; full relevant test suite passes. For WIL-10, serial `:pacenotes:test` passed on 2026-08-14, but local `:app:assembleDebug` failed because the Android SDK location was unavailable. No GitHub Actions workflow or fallback was found in this checkout, so final verification remains blocked.
2. Build/physical-device verification was actually run when applicable.
3. No secret or local credential is tracked.
4. Documentation, status, and decision records reflect a meaningful change.
5. A focused local commit exists once Git author identity is configured.
