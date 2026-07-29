# Context-Free Session Handoff

Use this document to resume the project without prior chat context.

## One-paragraph briefing

You are continuing **Rally Pacenotes Android**, a planned Android MVP that combines Mapbox turn-by-turn navigation with an app-owned, conservative geometry-based pacenote engine. The app should announce upcoming curve shape (for example “left four, tightens to two”), but never advise speed or claim hazards/visibility. Mapbox Navigation SDK v3 is selected for MVP navigation runtime; all pacenote logic must operate on provider-neutral models. UI uses an original Compose-native **Rally Technical Dossier** system: technical/operational hierarchy and restrained road-racing energy, with external Evangelion-inspired web libraries used only as non-branded references.

## Start here

```text
1. Read README.md.
2. Read docs/PROJECT-STATUS.md and docs/BACKLOG.md.
3. Read docs/DECISIONS.md before changing scope or architecture.
4. For UI work, read design/compose-technical-dossier-translation.md.
5. Check git status before touching files.
6. Pick the first unblocked P0 item from docs/BACKLOG.md.
```

## Current state

- Project root: `/home/hermes/rally-pacenotes-android/`
- Git repository exists; current files are staged but not committed.
- No Android Gradle project or production application code exists yet.
- Git commit currently fails because no local Git author name/email is configured.
- Mapbox account and tokens have not been supplied.
- The most useful next work is P0-01: Android/Compose project scaffold.

## Security rules

- Do not request Mapbox secrets in chat.
- Use Bitwarden CLI for the secret `DOWNLOADS:READ` token.
- Keep all tokens out of tracked files, screenshot artifacts, logs, and test fixtures.
- Public runtime tokens still require package/certificate restrictions before release.

## Non-negotiable product rules

- Do not turn curve severity into speed advice.
- Suppress uncertain/inferior geometry calls rather than speaking them.
- Do not require UI interaction while driving.
- Do not import/copy NERV/EVANGELION or Initial D assets, terminology, layouts, or identity.
- Do not allow Mapbox types into the classifier domain.
- Use test-first development for every production behavior.

## Suggested resumption prompt

> Continue Rally Pacenotes Android from `docs/SESSION-HANDOFF.md`. First inspect the repository and current Git status. Then execute only the next unblocked P0 backlog item using TDD, preserve provider-neutral pacenote models, and report real verification output. Do not request or expose secrets in chat.

## Handoff update rule

At the end of any meaningful work session:

1. Update `PROJECT-STATUS.md` with completed work, blockers, and the exact next action.
2. Add an ADR if architecture, safety, vendor, or scope changed.
3. Update `BACKLOG.md` if task order/dependencies changed.
4. Keep this document short; only update it when the resumption procedure or non-negotiable rules change.
