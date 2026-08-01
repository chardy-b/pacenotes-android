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
6. Plan and execute the first unblocked P0 item: P0-04 geometry normalization and distance/heading primitives.
```

## Current state

- Project root: `/home/hermes/rally-pacenotes-android/`
- Git repository: `main`, remote `https://github.com/chardy-b/pacenotes-android.git`; initial project commit exists.
- Android scaffold is verified locally; `:app:assembleDebug` succeeds.
- `core-model` is pure Kotlin/JVM and contains validated `GeoPoint`, immutable `RouteGeometry`, `RouteRevision`, `MatchedRoutePosition`, `NavigationProgress`/`NavigationStatus`, and deterministic `Pacenote` event IDs.
- Last full local verification: `./gradlew --no-daemon :core-model:test test :app:assembleDebug` succeeded on 2026-08-01.
- The next coding slice is P0-04, not GPX import, UI, GPS matching, or speech.

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
