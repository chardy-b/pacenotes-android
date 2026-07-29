# Rally Pacenotes Android

A credential-free, open-stack Android **GPX route-following pacenote companion**. It imports a local GPX track and provides conservative, geometry-based rally-style curve callouts; it is not address-to-address navigation, automatic rerouting, speed advice, or a road-hazard system.

## Current artifact

Open `design/rally-technical-dossier-inspo.html` directly in a browser for the visual direction. Read `design/compose-technical-dossier-translation.md` for the original Jetpack Compose component architecture, token mapping, accessibility behavior, and guidance for using Evangelion-inspired web libraries as reference material only. External references and reuse boundaries are recorded in [`docs/design-inspiration.md`](docs/design-inspiration.md).

Image provenance is linked inside the guide. The project takes inspiration from broad technical-drawing and retro street-racing principles only; it does not use or reproduce franchise artwork, logos, screen layouts, or characters.

## Project management

- [Project charter](docs/PROJECT-CHARTER.md) — product scope, safety position, architecture, and working agreements.
- [Current status](docs/PROJECT-STATUS.md) — completed work, blockers, and the exact next action.
- [Backlog](docs/BACKLOG.md) — dependency-aware P0/P1 work, definitions of ready/done, and token-budget estimates.
- [Decision record](docs/DECISIONS.md) — accepted architecture, safety, vendor, and design choices.
- [Context-free handoff](docs/SESSION-HANDOFF.md) — required reading and a ready-to-use resumption prompt.
- [Design inspiration](docs/design-inspiration.md) — external sources and reuse boundaries.

## Status

The V1 architecture is credential-free: local GPX import, app-owned geometry/replay, Android framework location, TTS, and a route-only Compose canvas. It intentionally has no Mapbox integration, network routing, public OSM-service dependency, or `INTERNET` permission. The Android scaffold and first tested provider-neutral domain slice are in progress; see [Project Status](docs/PROJECT-STATUS.md).
