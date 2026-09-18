# Pacenotes Agent Context

## Sources of truth

- Linear Pacenotes project (`WIL`) owns scope, dependencies, status, and blockers.
- Repository docs own architecture, test strategy, and durable technical decisions.
- Read the active Linear issue before material work. Do not invent a ticket. If no real issue covers the work, stop and define one with actual scope and acceptance criteria.

Historical plans, handoffs, and status records may preserve commands or results from earlier local Gradle runs. They are provenance, not current procedure: do not execute those commands locally. The GitHub Actions-only Gradle rule below supersedes them.

## Build and delivery boundaries

- Never run Gradle on the local host. GitHub Actions is the only Gradle execution environment.
- Use a feature branch and pull request for material work; never push directly to `main` and never enable auto-merge.
- Classify each change as baseline-only, device-gate-required, or release-requested.
- Exact-head baseline CI must produce `app-debug.apk` and run the documented unit-test suites.
- UI, permissions, location/GPS replay, foreground-service, audio/TTS, MapLibre, lifecycle, and other Android-runtime changes require API-35 device evidence on the same commit.
- Device evidence must include the exact APK, required screenshots, UI XML, reports, logs, relevant device state, and commit/run/checksum metadata.
- Phone-test prereleases publish the unchanged verified APK and required screenshots as GitHub prerelease assets; they never rebuild.
- Record exact runs and artifacts in the PR and Linear. Mark Done only after every required gate passes.

Teo may merge its own Pacenotes PR after applicable exact-head checks and self-review pass. Escalate to independent review for security, credential, public-exposure, destructive-migration, or concurrency-critical changes. Human merge remains allowed but is not a universal prerequisite.

An issue that explicitly opts into `docs/factory/` uses the stricter factory contract: separate verifier/integrator, recorded independence attestation, and Richard's Linear decision before merge or release. That opt-in factory policy overrides the general self-merge rule only for that factory run.

Load `.agents/skills/pacenotes-delivery/SKILL.md` for the procedural workflow and `docs/PACENOTES-GITHUB-DELIVERY.md` for the complete evidence contract. The unchanged official Google Android skills are available through the scoped `androiddev` Hermes profile, not general-purpose profiles.

## Product safety

- Pacenotes follows local GPX routes; it is not ordinary navigation, rerouting, speed advice, or a hazard system.
- Keep provider SDK types out of `core-model` and `pacenotes`.
- Suppress or pause guidance when matching is uncertain, off-route, wrong-way, or ambiguous.
- Never infer speed, hazards, visibility, surface, crests, or jumps from route geometry.
- Never expose credentials or private data in source, logs, fixtures, Linear, or chat.
