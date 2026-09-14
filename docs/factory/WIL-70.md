# WIL-70 factory pilot — hosted OpenFreeMap MapLibre MVP

## Identity and lifecycle

- **Linear issue:** [WIL-70](https://linear.app/wildhearts/issue/WIL-70/p0-13-ship-hosted-openfreemap-maplibre-basemap-mvp)
- **Contract owner / implementer:** Teo / implementation controller
- **Verifier/integrator:** Independent Luna reviewer, then Terra review lane
- **PR:** Pending creation from `ryli721/wil-70-factory-pilot`
- **Lifecycle state:** In Progress in Linear

## Bounded contract

### Inputs

- Pacenotes `main` base: `df0ffc5a297ac737dc83115890ab0debf8062407`.
- Hosted style: `https://tiles.openfreemap.org/styles/liberty`.
- Device proof target: GitHub-hosted API-35 `pixel_7_pro` emulator.

### In scope

Implement a map-first Android MVP that renders the hosted OpenFreeMap Liberty style at a deterministic Northern California camera; exposes essential attribution and a user-facing location control; preserves stability through activity recreation; and generates exact-head baseline, device, and inspected visual evidence.

### Non-goals

- No WIL-17 local/offline MBTiles, raster packages, import/update/delete UX, generated map data, or airplane-mode claim.
- No GPX replay, routing, rerouting, speed guidance, hazard behavior, or driver-distraction features.
- No provider credentials, API keys, self-hosted tiles, production SLA claim, release, or autonomous merge.

### Acceptance evidence

| ID | criterion | required command or artifact | expected result | exact 40-character head SHA | verifier result |
|---|---|---|---|---|---|
| A-1 | Style endpoint is centralized and credential-free | unit test and source review | OpenFreeMap Liberty URL only; OSM/OpenMapTiles attribution | Pending final PR head | Pending |
| A-2 | Map-first hosted basemap renders at NorCal camera | API-35 device artifact and inspected screenshot | roads/land/water visible; no blank map or system/app crash dialog | Pending final PR head | Pending |
| A-3 | Attribution and location control are visible | API-35 UI XML and screenshot | accessible, readable essential controls present | Pending final PR head | Pending |
| A-4 | Startup and recreation are stable | exact-head instrumentation output, app PID/activity state, logs | app resumes after recreation without fatal error | Pending final PR head | Pending |
| A-5 | Existing baseline and device evidence contracts remain valid | exact-head Actions runs and artifact validator | baseline and API-35 device gate succeed with ticket-specific evidence | Pending final PR head | Pending |

## Risk and gate classification

- **Risk classification:** High. MapLibre rendering, network-dependent hosted style, Android lifecycle, runtime location control, and visual acceptance require device evidence.
- **Delivery gate:** Device gate required.
- **Credential/security boundary:** No credentials, secret configuration, third-party source/data packages, or runtime map package import are introduced. The hosted public endpoint is MVP-only and has no SLA.

### Operational gate mapping

- Exact-head `Android baseline / build and unit tests` must pass.
- Exact-head manually dispatched API-35 device gate must pass and retain automated, device, and visual artifacts.
- The rendered screenshot must be independently inspected; semantic nodes or a successful HTTP request alone are insufficient.
- Any missing, stale, failed, cancelled, skipped, or unreviewed mandatory evidence is **NO-GO/BLOCKED**.

## Evidence packet

- **Exact PR-head SHA (40 characters):** Pending final PR head.
- **Baseline CI run/check:** Pending exact-head run.
- **API-35 automated/device/visual evidence:** Pending exact-head run and artifact inspection.
- **Release APK:** N/A — no release requested.
- **Other evidence:** Unit tests, instrumentation reports, screenshot, UI XML, activity state, logcat, APK checksum, provenance, factory ledger.

## Independent review and findings

- **Review performed against exact SHA:** Pending final PR head.
- **Verifier/integrator result:** Pending.
- **Terra UX review request comment:** Pending PR and exact head; required before independent Terra review dispatch.
- **Terra UX final report:** Pending.
- **Material findings:** None yet.
- **Fix-or-defer record:** Pending if review finds a material issue.

### Independence attestation

- **Implementer identity/role:** Teo / implementation controller
- **Verifier identity/role:** Independent Luna reviewer / verifier
- **Attestation result:** Pending
- **Basis:** The verifier will not edit the implementation worktree and will inspect the final exact PR head independently.

## Decision and missing evidence

- **Missing evidence / blockers:** All implementation, CI, device, visual, and independent-review evidence remains pending.
- **Explicit state:** BLOCKED pending implementation and required gates.
- **Richard final decision in Linear:** Pending.
- **Release/merge authority:** Human-only; no autonomous merge or release.
