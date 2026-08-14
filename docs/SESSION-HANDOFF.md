# Context-Free Session Handoff

Use this document to resume the project without prior chat context.

## One-paragraph briefing

You are continuing **Rally Pacenotes Android**, a credential-free Android MVP that follows a tester-supplied local GPX route and generates conservative, geometry-based curve descriptions. It is not ordinary navigation, does not reroute, and must not give speed, hazard, surface, visibility, crest, or jump advice. All route, progress, and pacenote domain types remain provider-neutral pure Kotlin; Android integration comes later.

## Start here

```text
1. Read README.md.
2. Read docs/PROJECT-STATUS.md and docs/BACKLOG.md.
3. Read docs/DECISIONS.md before changing scope or architecture.
4. Read docs/plans/2026-08-01-p0-03-route-progress-pacenote-domain.md for the completed model boundary.
5. Check git status before touching files.
6. After WIL-10 remote CI verification is accepted, execute P0-06 stateful GPX location matcher and route-revision handling; it is now unblocked. Local Android SDK access remains unavailable, and no physical-device or instrumentation validation is claimed.
```

## Current state

- Project root: `/home/hermes/rally-pacenotes-android/`
- Git repository: `main`, remote `https://github.com/chardy-b/pacenotes-android.git`; initial project commit exists.
- Android scaffold is verified locally; `:app:assembleDebug` succeeds.
- `core-model` is pure Kotlin/JVM and contains validated `GeoPoint`, immutable `RouteGeometry`, `RouteRevision`, `MatchedRoutePosition`, `NavigationProgress`/`NavigationStatus`, and deterministic `Pacenote` event IDs.
- `pacenotes` is a pure Kotlin/JVM module depending only on `core-model`; it contains stable spherical distance/heading primitives and fixed-distance route normalization with typed discontinuity suppression.
- WIL-10 curve classification is complete: it consumes only app-owned normalized geometry and conservatively detects candidates using synthetic fixtures. Candidates longer than 250 m, or with any per-step heading change over 60°, are suppressed.
- WIL-10 does not recognize real topology or junctions and does not use GPS matching, map/provider data, speed, hazards, phrases, or event IDs. Junction-like and roundabout-like fixtures are geometry-shape evidence only.
- WIL-10 `:pacenotes:test` passed on 2026-08-14. At exact head `7e75840844cb7223355db313f30fc6f8c7c3cc6c`, GitHub Actions run `31766037397` completed successfully: `Build and unit tests` completed/success, including the debug APK build and app/core-model/pacenotes unit tests. `API 35 instrumentation tests` completed/skipped because this classifier ticket is not device-specific and manual dispatch was not requested. Local Android SDK remains unavailable; no physical-device instrumentation is claimed. P0-06 is now unblocked.
- Last P0-04 focused verification: `./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx512m -Dfile.encoding=UTF-8' -Dorg.gradle.workers.max=1 :pacenotes:test --tests '*RouteNormalizerTest*' --rerun-tasks` succeeded on 2026-08-01.
- P0-06 stateful GPX location matcher and route-revision handling is now unblocked by WIL-10 remote CI verification. A minor deferred coverage refinement remains: add direct regression coverage distinguishing an accepted exact 60° step from a rejected step greater than 60°.

## Security rules

- Do not request or expose secrets in chat.
- Keep tokens out of tracked files, screenshot artifacts, logs, and test fixtures.
- Keep the base V1 free of `INTERNET`, Mapbox, public OSM tiles, OSRM demo endpoints, Google Play Services, and secret configuration.

## Non-negotiable product rules

- Do not turn curve severity into speed advice.
- Suppress uncertain, ambiguous, off-route, wrong-way, and low-confidence calls rather than speaking them.
- Do not require UI interaction while driving.
- Do not import/copy NERV/EVANGELION or Initial D assets, terminology, layouts, or identity.
- Do not allow provider SDK types into `core-model` or the future `pacenotes` module.
- Use strict test-first development for every production behavior.

## Handoff update rule

At the end of meaningful work:

1. Update `PROJECT-STATUS.md` with completed work, blockers, and the exact next action.
2. Add an ADR if architecture, safety, vendor, or scope changed.
3. Update `BACKLOG.md` if task order/dependencies changed.
4. Keep this document short and current; only record durable resumption facts.
