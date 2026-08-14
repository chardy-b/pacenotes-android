# Architecture and Product Decisions

Use this as an append-only decision record. Do not rewrite earlier decisions; supersede them with a new entry that links to the old one.

## ADR-001 — Use Mapbox Navigation SDK v3 for the MVP

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Use Mapbox Navigation SDK v3 for route acquisition, map matching, route progress, rerouting, map display, and offline-runtime foundations.
- **Why:** It minimizes time-to-field-test and lets the MVP focus on curve classification and voice UX rather than global mapping operations.
- **Consequence:** Monitor MAU/trip economics and Mapbox terms. Do not make the pacenote engine depend on Mapbox SDK types.

## ADR-002 — Keep the pacenote engine provider-neutral

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Convert Mapbox routes/progress into app-owned neutral models before geometry analysis and speech scheduling.
- **Why:** Preserves a future Valhalla/OSM evaluation path and makes algorithm replay/testing deterministic.
- **Consequence:** Mapbox adapter code stays separate from `core-model` and `pacenotes` modules.

## ADR-003 — Safety-first, geometry-only MVP

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Generate conservative curve-shape descriptions only; do not give speed, hazard, visibility, surface, or crest claims.
- **Why:** Route-centerline geometry cannot safely establish those properties.
- **Consequence:** Favor silence over an uncertain or noisy pacenote; field testing requires a passenger/tester.

## ADR-004 — Original Rally Technical Dossier design system

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Use original technical-drafting and restrained retro road-racing principles for the visual system.
- **Why:** It supports the desired high-energy instrument feeling without making the app a novelty skin.
- **Consequence:** Active navigation remains quiet and glanceable; expressive texture/motion belongs in onboarding, replay, and post-drive views.

## ADR-005 — Evangelion-inspired repositories are references, not dependencies

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Study `mdrbx/nerv-ui` and `TheGreatGildo/nerv-ui` for general component and command-surface principles only.
- **Why:** Both are web-oriented and their MIT licenses do not grant the rights to the underlying franchise identity.
- **Consequence:** No React/Tailwind import, no NERV terminology/branding, no copied layouts. Implement original Compose components. See [`design-inspiration.md`](design-inspiration.md).

## ADR-006 — Compose + Material 3 foundation

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** Use Jetpack Compose and Material 3 accessibility behavior as the UI foundation, with app-owned technical primitives.
- **Why:** Maintains Android-native semantics, focus behavior, font scaling, and testability.
- **Consequence:** No aesthetic-only UI library dependency. See [`../design/compose-technical-dossier-translation.md`](../design/compose-technical-dossier-translation.md).

## ADR-007 — Test-first implementation and token estimates

- **Date:** 2026-07-25
- **Status:** Accepted
- **Decision:** All production behavior follows TDD; task sizing uses approximate token budgets rather than calendar-week estimates when useful.
- **Why:** The classifier and driving UX need deterministic tests and measurable progress.
- **Consequence:** Each backlog task must specify verification and a token budget.

## ADR-008 — Credential-free GPX route-following MVP

- **Date:** 2026-07-26
- **Status:** Accepted
- **Decision:** Replace Mapbox runtime integration in V1 with local GPX import, app-owned GPX qualification/replay, Android framework location, a route-only Compose canvas, and Android TTS. The base app declares no `INTERNET` permission and sends no route/location data to a server.
- **Why:** Mapbox credentials are unavailable, while the core product hypothesis—whether geometry-derived spoken curve previews are useful and conservative—can be tested without hosted routing or map tiles.
- **Consequence:** V1 is a GPX route-following pacenote companion, not address-to-address navigation: it provides no destination search, routing, road-network map matching, ordinary maneuvers, or automatic rerouting. It must pause speech rather than invent guidance when matching is uncertain, ambiguous, wrong-way, or off-route. Self-hosted routing and MapLibre with licensed/bundled tiles remain separate future decisions.
- **Supersedes:** ADR-001 only for the V1 navigation-runtime choice. ADR-002, ADR-003, ADR-004, ADR-006, and ADR-007 remain accepted.

## ADR-009 — Self-hosted online tiles for early real-map testing

- **Date:** 2026-08-01
- **Status:** Accepted
- **Decision:** The early real-map view may require `INTERNET` and use an app-owned/self-hosted tile endpoint rendered by MapLibre. The pacenote engine remains provider-neutral and continues to receive only app-owned geometry models.
- **Why:** The product owner accepts an internet requirement for the first real-map iteration and can self-host tile data, avoiding a dependency on public OSM tile infrastructure while enabling genuine road context.
- **Consequence:** Add a narrowly scoped `INTERNET` permission only with the MapLibre/tile integration. Before implementation, document the endpoint, authentication model, region/data source, tile format/style, OSM attribution/licensing, cache policy, privacy disclosure, and failure/offline behavior. Do not use `tile.openstreetmap.org` as a production default. This supersedes ADR-008 only for optional early-map display; GPX replay, route matching, and pacenote generation remain local and app-owned.

## ADR-010 — Start real-map proof with a local Northern California package

- **Date:** 2026-08-01
- **Status:** Accepted
- **Decision:** Prove the first real basemap using a locally imported Northern California vector-tile package, rendered by MapLibre without runtime network access. The self-hosted online-tile option remains deferred, not rejected.
- **Why:** A single known region proves the real-map overlay while keeping Replay Alpha reliable, private, and independent of server setup.
- **Consequence:** Generate and version map data off-device; require OSM/ODbL attribution and provenance; do not bundle the large generated package inside the APK. The package must be optional with an app-owned route-canvas fallback. This supersedes ADR-009 for the initial implementation sequence only.

## ADR-011 — Hosted MapLibre navigation camera uses verified course before fused device heading

- **Date:** 2026-08-11
- **Status:** Accepted
- **Decision:** For WIL-76, use the delivered Android `Location` course only when it is moving, fresh, and accurate; otherwise use a fresh, sufficiently accurate, display-remapped `TYPE_ROTATION_VECTOR` heading, retain a last reliable course only briefly, then hold north-up. Drive MapLibre's actual camera and native LocationComponent rather than a Compose marker overlay. Provide an explicit navigation/ north-up camera toggle.
- **Why:** Course represents travel direction while moving; phone orientation does not. A raw accelerometer cannot provide yaw. The game rotation vector is not north-referenced. The fallback order avoids displaying a direction with unwarranted confidence while retaining useful orientation at low speed.
- **Consequence:** The map uses bounded circular smoothing and animation; location, sensor, and visual device evidence must be verified in hosted emulator CI before WIL-76 closes. See [`DIRECTION-POLICY.md`](DIRECTION-POLICY.md) for thresholds, lifecycle/battery constraints, rendering behavior, and primary sources.
