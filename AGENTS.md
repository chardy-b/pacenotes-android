# Pacenotes Development Workflow

## Linear is the project source of truth

- **Linear project:** [Pacenotes](https://linear.app/wildhearts/project/pacenotes-87634b2bd822)
- **Team:** `Wildhearts` (`WIL`)
- Use Linear for every meaningful development task, lifecycle transition, blocker, and project update.
- Repository documentation remains the technical source of record for design/ADRs, test strategy, and context-free handoff. Keep it consistent with Linear.

## Required task loop

1. Read the active Linear issue and relevant project document before editing code.
2. If no issue represents the work, create one in the Pacenotes project with outcome, acceptance criteria, dependencies, risk/safety constraints, and a token-budget estimate.
3. Mark the issue `In Progress` before material implementation.
4. Follow strict TDD and all repository safety boundaries.
5. Record real test/build/device evidence and any deviation or blocker in a Linear comment.
6. Move an issue to `Done` only when its acceptance criteria and verification have actually passed.
7. Post a Linear project status update at milestones, material decisions, delivery risks, scope changes, and blockers.

## GitHub delivery gates

- Use a feature branch and pull request for all material work.
- Do not push directly to `main`.
- Do not enable auto-merge or merge a pull request without explicit user instruction.
- The authoritative `Android baseline / build and unit tests` check must pass on the current PR head.
- Baseline CI must build `app-debug.apk`, run unit-test suites for `app`, `core-model`, and `pacenotes`, and retain the APK as an Actions artifact.
- Classify every change as **baseline-only**, **device gate required**, or **release requested** before delivery.
- UI, permissions, location, GPS replay, foreground-service, audio/TTS, MapLibre, lifecycle, and other Android-runtime changes require a successful manual API-35 device gate on the same commit.
- Device-gate evidence must include the exact APK, ticket-required screenshots, UI XML, test reports, logs, device state when applicable, and commit/run/checksum metadata.
- Link exact Actions runs and artifacts in the PR and Linear issue.
- Publish a phone-test prerelease only after deliberate human review of a successful device-gate artifact. The release workflow must publish that unchanged artifact and must not rebuild it.
- Do not move a Linear issue to Done while a required gate is missing, stale, failed, or unreviewed.
- Follow [`docs/PACENOTES-GITHUB-DELIVERY.md`](docs/PACENOTES-GITHUB-DELIVERY.md) for the exact delivery sequence.

## Non-negotiable product boundaries

- Pacenotes is a local-GPX route-following companion, not ordinary navigation, rerouting, speed advice, or a hazard system.
- Do not put provider SDK types in `core-model` or `pacenotes`.
- Suppress or pause guidance when matching is uncertain, off-route, wrong-way, or ambiguous.
- Do not claim speed, hazards, visibility, surface, crests, or jumps from route geometry.
- Do not expose credentials or private data in source, logs, test fixtures, Linear, or chat.

## Current priority

- `WIL-10` — finish P0-05 conservative curve detection/classification acceptance coverage.
- `WIL-17` — prove the local offline Northern California MapLibre basemap without runtime network access.
