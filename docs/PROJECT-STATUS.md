# Project Status

> **Last updated:** 2026-08-14
> **Phase:** Replay Alpha is buildable and launchable; physical-device validation remains pending.
> **Overall state:** No Mapbox credential is required for V1. The active scope is a local-GPX route-following pacenote companion, not general navigation.

## Completed

- [x] Feasibility research: Mapbox versus OSM/self-hosted navigation options.
- [x] V1 architecture superseded: credential-free local GPX route-following companion; Mapbox runtime is not required.
- [x] Sol architecture review completed; scope, safety, lifecycle, GPX qualification, and data/licensing corrections accepted.
- [x] Safety and scope boundaries documented.
- [x] Full implementation plan authored in Hermes planning workspace.
- [x] Original Rally Technical Dossier visual direction created.
- [x] HTML inspiration board with locally stored source assets created.
- [x] Compose-specific visual/component translation documented.
- [x] External design-reference policy and source provenance documented.
- [x] Local Git repository initialized; initial credential-free MVP commit created.
- [x] Android/Gradle scaffold verified with the constrained local toolchain: `:app:assembleDebug` succeeds.
- [x] First provider-neutral TDD slice: `GeoPoint` and immutable `RouteGeometry` invariants, including defensive input-list copying; Luna RED→GREEN evidence and Sol re-review passed.
- [x] Provider-neutral route state slice: validated `RouteRevision` and `MatchedRoutePosition`; explicit navigation statuses/progress invariants; and deterministic, caller-independent pacenote event IDs. Focused RED→GREEN tests and full JVM/debug-APK verification passed.
- [x] Pure `pacenotes` geometry foundation: stable spherical distance, initial heading, wrap-safe signed heading deltas, and typed metre-spaced route normalization with conservative discontinuity suppression. Focused tests and full relevant Gradle verification passed.
- [x] WIL-10 conservative curve classifier implementation and remote verification: pure app-owned normalized geometry candidate detection with synthetic fixtures for straight, gentle/sharp, S-bend, noise, junction-like, and roundabout-like geometry; span boundaries and exact severity mapping are covered. At exact head `7e75840844cb7223355db313f30fc6f8c7c3cc6c`, GitHub Actions run `31766037397` completed successfully: `Build and unit tests` completed/success, including the debug APK build and app/core-model/pacenotes unit tests. `API 35 instrumentation tests` completed/skipped because this classifier ticket is not device-specific and manual dispatch was not requested. Local Android SDK remains unavailable; this is remote CI evidence, not physical-device instrumentation evidence.

## Not started

- [ ] GPX parser/qualification, deterministic replay, stateful location matcher, UI, speech, foreground service, and field test.

## Current blockers / required inputs

| Item | Owner | Why it matters | Resolution |
|---|---|---|---|
| Local Git author identity | User / environment owner | First local commit currently fails | Set `git config user.name` and `git config user.email` for this repo or globally. |
| Initial field-test region and lawful GPX sources | Product owner | Needed for representative, directionally correct route fixtures | Choose a region and 10–20 routes; preserve source/license/provenance for non-original GPX data. |
| Physical Android test device | Product owner / developer | Needed to validate location, foreground service, TTS, Bluetooth, and screen-off behavior | Select at least one API-26+ physical device before field gate. |

## Immediate next action

**P0-06: Add the stateful GPX location matcher and route-revision handling.**

P0-06 is now unblocked by the successful remote WIL-10 verification recorded above. Local Android SDK access remains unavailable, and no physical-device or instrumentation validation is claimed.

Carry forward the classifier contract: geometry-only candidate detection, suppression of candidates over 250 m or containing any step over 60°, and no claims of topology/junction recognition, GPS, map/provider data, speed, hazards, phrases, or event IDs. Add the deferred direct exact-60° versus greater-than-60° boundary regression when the next test slice is appropriate.

Do not add Mapbox, public OSM tiles, OSRM demo endpoints, Google Play Services, `INTERNET` permission, credentials, or secret configuration to V1.

## Files a future contributor should read first

1. [`README.md`](../README.md)
2. [`PROJECT-CHARTER.md`](PROJECT-CHARTER.md)
3. [`BACKLOG.md`](BACKLOG.md)
4. [`DECISIONS.md`](DECISIONS.md)
5. [`SESSION-HANDOFF.md`](SESSION-HANDOFF.md)
6. [`../design/compose-technical-dossier-translation.md`](../design/compose-technical-dossier-translation.md)

## Source-of-truth locations

| Topic | Location |
|---|---|
| Repository | `/home/hermes/rally-pacenotes-android/` |
| Current project documents | `docs/` in repository |
| Full implementation plan | `/home/hermes/.hermes/plans/2026-07-25_031255-rally-pacenote-android-mvp.md` |
| Visual inspiration board | `design/rally-technical-dossier-inspo.html` |
| Compose visual blueprint | `design/compose-technical-dossier-translation.md` |

The full plan exists outside the repository; use `BACKLOG.md` and this status file as the portable execution summary.
