# Factory contract and evidence packet template

Copy this file to `docs/factory/<WIL-ID>.md`. Replace every placeholder. Keep the linked Linear issue authoritative for lifecycle state.

For each actual run, use one ledger path `docs/factory/runs/<WIL-ID>/<RUN-ID>.md` and one row in `docs/factory/runs/<WIL-ID>/INDEX.md`. The `RUN-ID` is exactly `YYYYMMDDTHHMMSSZ-<short-head>-NN`, with `NN` two-digit zero-padded from `01`. Before creating a record, read the index; for the same exact `started_utc` second and exact head choose one greater than the largest existing `NN`. Create record and row together in one PR/commit. Review/CI statically checks uniqueness. If concurrent branches allocate the same ID, the later-to-merge branch rebases, re-reads the index, allocates the next `NN`, and updates both filename and row before merge; no locking is claimed.

## Identity and lifecycle

- **Linear issue:** `<URL and ID>`
- **Contract owner / implementer:** `<identity and role>`
- **Verifier/integrator:** `<identity and role>`
- **PR:** `<URL>`
- **Lifecycle state:** `<Linear state; do not infer from this file>`

## Bounded contract

### Inputs
- `<input, source, format, and assumptions>`

### In scope
- `<bounded deliverable>`

### Non-goals
- `<explicit exclusions>`

### Acceptance evidence

Record every criterion in this table. The exact head is a 40-character SHA, not a branch name or abbreviated SHA.

| ID | criterion | required command or artifact | expected result | exact 40-character head SHA | verifier result |
|---|---|---|---|---|---|
| A-<N> | `<observable criterion>` | `<command or artifact link>` | `<observable expected result>` | `<40-character SHA>` | `<PASS/FINDINGS/BLOCKED>` |

## Risk and gate classification

- **Risk classification:** `<low / medium / high, with rationale>`
- **Delivery gate:** `<baseline-only | device gate required | release requested>`
- **Credential/security boundary:** `<what is not changed>`

### Operational gate mapping

- **Baseline-only:** exact-head baseline CI must pass.
- **Device-required categories:** UI/Compose, permissions, location/GPS replay, foreground service, audio/TTS, MapLibre/rendering, lifecycle, and any Android runtime-specific behavior require exact-head API-35 automated, device, and visual evidence.
- **Explicit user-requested release:** consider release only after reviewed successful applicable device evidence and confirmation that the release artifact is unchanged; include baseline/device evidence as applicable.
- Any mandatory evidence that is missing, stale, failed, skipped, cancelled, or unreviewed is **NO-GO/BLOCKED**.

## Evidence packet

- **Exact PR-head SHA (40 characters):** `<SHA>`
- **Baseline CI run/check:** `<URL, run ID, SHA, result>`
- **API-35 automated/device/visual evidence (required when gate classification requires it):** `<run/artifact URLs, SHA, results, or N/A with rationale>`
- **Release APK (if applicable):** `<URL, exact head, SHA-256, or N/A>`
- **Other evidence:** `<tests, screenshots, reports, logs; link exact artifacts>`

Evidence must identify the exact head. Prior-SHA, branch-only, skipped, cancelled, or unreviewed evidence does not satisfy a gate. Separate automated, device, and visual conclusions where relevant.

## Independent review and findings

- **Review performed against exact SHA:** `<40-character SHA and date>`
- **Verifier/integrator result:** `<PASS/FINDINGS/BLOCKED>`
- **Terra UX review request comment (frontend/major PR only):** `<Linear comment URL, exact head, or N/A>`
- **Terra UX final report (frontend/major PR only):** `<report URL, exact head, outcome, or N/A>`
- **Material findings:** `<finding, severity, evidence link>`
- **Fix-or-defer record:** `<fix commit/evidence, or explicit deferral, owner, rationale, and Linear follow-up>`

### Independence attestation

- **Implementer identity/role:** `<identity and role>`
- **Verifier identity/role:** `<identity and role>`
- **Attestation result:** `<PASS/FAIL>`
- **Basis:** `<why the verifier is independent of the implementer>`

The verifier must be separate from the implementer for the required review. A failed or absent attestation is **BLOCKED**.

### Terra protocol

For every frontend-facing change or major PR, the assigned verifier/integrator or controller posts a Terra review request as a Linear comment on the ticket before dispatching/assigning the independent Terra reviewer. The request must include: ticket URL, PR URL, exact 40-character head SHA, packet path, evidence links, review scope/checklist (hierarchy, layout, readability at 200% font, interaction clarity, accessibility cues, and product-scope mismatch), and requested outcome. The Linear comment and assigned reviewer are the handoff; do not invent or imply a Terra CLI or automatic dispatch. Provide the packet, exact head, and applicable screenshots/recordings, device artifacts, and UI XML/accessibility dump.

Record the request comment URL and final Terra report URL in both this ticket packet and the PR. The report includes exact head, evidence links, and outcome: `PASS`, `FINDINGS`, or `BLOCKED`. Terra is non-approving and non-merging and never replaces CI, device, verifier, artifact, or human gates. `FINDINGS` requires a fix-or-defer record; every fix requires Terra re-review on the exact new head. `BLOCKED` is escalated to Richard.

## Decision and missing evidence

- **Missing evidence / blockers:** `<list, or “None”>`
- **Explicit state:** `<GO | NO-GO | BLOCKED | DEFERRED>`
- **Richard final decision in Linear:** `<URL, date, decision, and risk acceptance if any>`
- **Release/merge authority:** Human-only; no autonomous merge or release.
