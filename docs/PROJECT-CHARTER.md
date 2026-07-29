# Rally Pacenotes Android — Project Charter

## Product statement

Rally Pacenotes is an Android **GPX route-following pacenote companion** that uses a tester-supplied local route to give **conservative, geometry-based, rally-style descriptions of upcoming curves**. It is not general road navigation, a rally-speed assistant, or a source of speed recommendations.

## User value

While navigating an ordinary driving route, a driver hears concise preview calls such as:

> “In 250 meters, left four. Tightens to left two.”

The value is anticipation of road shape, with normal navigation reliability underneath.

## MVP success criteria

1. A tester can import a local, directionally correct, route-qualified GPX file and begin replay or GPS-guided route following.
2. The app tracks approximate progress only when location matching is confident; it pauses spoken guidance when it is uncertain, ambiguous, wrong-way, or off route.
3. An app-owned pacenote engine turns qualified route geometry into deterministic, testable events.
4. Significant high-confidence curves can be announced with direction, severity, distance, and optional `tightens` only after modifier validation.
5. Junction artifacts, roundabouts, minor bends, self-intersection ambiguity, and low-confidence geometry are suppressed.
6. Replay cancels stale queued speech and regenerates deterministic route events for the selected GPX revision.
7. The app continues voice guidance with screen off through a foreground service and behaves correctly with audio focus/Bluetooth.

## Safety position

- Directions are descriptive and advisory only.
- Users must obey traffic laws, speed limits, weather, visibility, road surface, and vehicle limits.
- The MVP does not infer or announce a safe speed, crest, jump, surface, sightline, or hazard.
- Voice is the primary active-driving channel; the UI must be glanceable and must not require interaction while driving.
- Field testing requires a passenger/tester to record feedback; the driver must not operate the test interface.

## MVP boundaries

### In scope
- One Android app for local GPX route following and safe replay.
- Android framework location updates with stateful, conservative GPX matching; no network route matching.
- Local, deterministic route-geometry classifier.
- Android TextToSpeech, service-owned foreground location/audio focus, and simulation/replay mode.
- Original Jetpack Compose technical-dossier visual system with an app-owned route canvas.
- No `INTERNET` permission or route/location transmission in the base V1 app.

### Explicitly deferred
- Curvy-road route selection.
- Android Auto.
- Accounts, social route sharing, telemetry backend, and ML classifier training.
- Global offline-region UX.
- Claims of genuine rally reconnaissance fidelity.

## Technical direction

```text
Local GPX track + framework GPS fixes
          ↓
GPX qualification + app-owned stateful route matcher
          ↓
Pacenote engine: normalize → detect → classify → confidence-gate
          ↓
Pacenote scheduler → Android TextToSpeech + audio focus
          ↓
Compose route canvas + navigation-state UI
```

### Key architectural rule

No provider SDK type may enter the pacenote engine. The `core-model` and `pacenotes` modules own provider-neutral route/progress/event models. Android framework integration stays behind app-owned adapters.

## Design direction

**Rally Technical Dossier:** an original system combining technical drafting, sharp operational hierarchy, and restrained retro road-racing energy. It draws only on broad principles from external inspiration; it does not copy franchise identifiers, artwork, layouts, or terminology.

- Visual source record: [`design-inspiration.md`](design-inspiration.md)
- Compose translation: [`../design/compose-technical-dossier-translation.md`](../design/compose-technical-dossier-translation.md)
- Interactive board: [`../design/rally-technical-dossier-inspo.html`](../design/rally-technical-dossier-inspo.html)

## Working agreements

- **Estimate in token budgets, not weeks**, when an estimate is useful.
- New production behavior follows strict TDD: test fails first, minimal implementation, pass, refactor.
- Preserve app-owned models and algorithm fixtures for replayable tests.
- Keep vendor terms, asset licenses, and provenance documented.
- Prefer a small, working field-testable scope to speculative platform features.
