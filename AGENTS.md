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

## Non-negotiable product boundaries

- Pacenotes is a local-GPX route-following companion, not ordinary navigation, rerouting, speed advice, or a hazard system.
- Do not put provider SDK types in `core-model` or `pacenotes`.
- Suppress or pause guidance when matching is uncertain, off-route, wrong-way, or ambiguous.
- Do not claim speed, hazards, visibility, surface, crests, or jumps from route geometry.
- Do not expose credentials or private data in source, logs, test fixtures, Linear, or chat.

## Current priority

- `WIL-10` — finish P0-05 conservative curve detection/classification acceptance coverage.
- `WIL-17` — prove the local offline Northern California MapLibre basemap without runtime network access.
