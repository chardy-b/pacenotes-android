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

## WIP and priority control

- Work on at most one P0 implementation issue at a time. The sole current active P0 is `WIL-10`.
- Before switching from an active issue, post a Linear checkpoint that identifies completed acceptance criteria, remaining work, reason for the switch, and the new priority decision.
- A feasibility spike does not authorize product delivery or adjacent work. If blocked or deferred, record the exact blocker/next action and return the issue to the appropriate non-active state.

## Required slice contract

Before any material code change, record in the active Linear issue:

1. one outcome and explicit non-goals;
2. one focused acceptance test, including the expected RED failure;
3. the required evidence tier (unit, integration, device, visual, or release);
4. a maximum of three logical commits for the slice.

Implement exactly one acceptance item per slice: RED test, minimal implementation, focused verification, relevant suite, then a focused commit. Count only predeclared acceptance criteria with matching evidence as progress.

## Stop and reassess

Stop implementation and post a Linear checkpoint after either two failed repair attempts, three commits without closing the stated acceptance item, or any material scope/architecture change. The checkpoint must state completed evidence, unverified conditions, blockers/scope change, and a clear recommendation to continue, split, defer, or reprioritize.

## Evidence rules

- A unit test proves logic; an integration test proves a component boundary; a device test proves installed-device behavior; visual/release claims require visual/release evidence.
- A Compose semantic assertion proves a UI node exists, and an application PID proves the process survived; neither proves rendered visual content.
- Issue-specific done criteria must name the required evidence. For offline maps this includes validated atomic import, corrupt/interrupted-package handling, verified no-network tile rendering, visually inspected evidence, provenance, and explicit test-versus-production packaging behavior.
- Keep CI infrastructure changes narrowly scoped to the product acceptance they unlock; do not mix CI refactoring with unrelated feature delivery.

## Current priority

- `WIL-10` — sole active P0: finish conservative curve detection/classification acceptance coverage one fixture category at a time.
- `WIL-17` — deferred feasibility follow-up; resume only with a new bounded slice contract and the required evidence tier.
