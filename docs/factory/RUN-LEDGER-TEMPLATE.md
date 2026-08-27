# Factory run ledger template

Copy this file to `docs/factory/runs/<WIL-ID>/<RUN-ID>.md` for one actual run only, and add its row to `docs/factory/runs/<WIL-ID>/INDEX.md`. `RUN-ID` must be exactly `YYYYMMDDTHHMMSSZ-<short-head>-NN`, where `<short-head>` is the first seven lowercase hexadecimal characters of the exact 40-character head SHA and `NN` is two-digit, zero-padded, and starts at `01`. Before creating the record, read the index; for the same exact `started_utc` second and exact head, choose one greater than the largest existing `NN`. Create record and row together in the same PR/commit; review/CI static validation checks uniqueness. If concurrent branches allocate the same ID, the later-to-merge branch must rebase, re-read the index, allocate the next `NN`, and update both filename and row before merge. This is deterministic and makes no locking claim. Create one entry for every actual run, never fake entries. This records evidence, not authority; keep secrets and unnecessary personal data out.

## Run identity
- `wil_id`: `<WIL-ID>`
- `run_id`: `<actual run ID>`
- `run_path`: `docs/factory/runs/<WIL-ID>/<RUN-ID>.md`
- `started_utc`: `<UTC timestamp, second precision>`
- `allocation_state`: `allocated | superseded`
- `linear_ticket`: `<URL>`
- `packet`: `docs/factory/<WIL-ID>.md`
- `exact_head_sha`: `<40-character SHA>`
- `bounded_scope`: `<one sentence>`
- `risk_classification`: `<classification and rationale>`
- `implementation_role`: `<identity and role>`
- `review_role`: `<identity and role>`
- `elapsed_time_utc`: `{start: <UTC>, end: <UTC>, duration: <ISO-8601>}`

## `evidence_requested` (required list; one item per requested artifact)

Each list item must explicitly include `name`, `required`, `disposition`, `exact_head_sha`, `reference`, and `rationale`. Allowed dispositions are `supplied`, `missing`, `stale`, `not-applicable`, and `rejected`; `not-applicable` requires a written rationale.

| name | required? | disposition | exact_head_sha | reference | rationale |
|---|---|---|---|---|---|
| baseline-ci | yes | `supplied\|missing\|stale\|not-applicable\|rejected` | `<SHA or null>` | `<URL/ID or null>` | `<required rationale for N/A or gap>` |
| api-35-device | `<yes/no>` | `<allowed value>` | `<SHA or null>` | `<artifact or null>` | `<rationale>` |
| visual | `<yes/no>` | `<allowed value>` | `<SHA or null>` | `<screenshots/UI XML or null>` | `<rationale>` |
| release-apk | `<yes/no>` | `<allowed value>` | `<SHA or null>` | `<artifact or null>` | `<rationale>` |

- `evidence_completeness`: `complete | partial | missing`
- `quality_outcome`: `pass | findings | blocked`
- `max_finding_severity`: `none | low | medium | high | critical`

## Rework, decision, deferral
- `rework_requested`: `yes | no`
- `rework_completed`: `yes | no | not-applicable`
- `rework_evidence`: `<link or null>`
- `final_decision`: `GO | NO-GO | BLOCKED | DEFERRED`
- `richard_linear_decision`: `<URL/date/decision or null>`
- `deferral`: `{state: not-deferred | deferred, rationale: <text or null>, owner: <text or null>, follow-up: <issue/date or null>}`

## Independence attestation
- `implementer`: `{identity: <text>, role: implementer}`
- `verifier`: `{identity: <text>, role: verifier/integrator}`
- `attestation`: `PASS | FAIL`
- `attestation_basis`: `<why identities/roles are independent>`

## Up-to-five comparison row

Use only populated actual entries; absent values serialize as `null`, never as zero or success:

`{wil_id, quality_outcome, max_finding_severity, evidence_completeness, rework_requested, rework_completed, elapsed_time_utc, final_decision}`
