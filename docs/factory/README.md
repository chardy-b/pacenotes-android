# Pacenotes factory foundations

This directory is the canonical repository record for factory contracts, evidence packets, and run ledgers. Linear is the lifecycle source of truth: issue status, ownership, approvals, and final decisions stay in Linear. These files define work and preserve evidence links; they do not replace Linear or GitHub.

## Process

1. Create or update one packet at `docs/factory/<WIL-ID>.md` before implementation.
2. Keep scope, non-goals, acceptance rows, and risk classification bounded and explicit.
3. Implement on the PR branch and record the exact PR-head SHA plus required evidence.
4. A separate verifier/integrator reviews that exact head against the packet and gate matrix.
5. For each actual run only, create exactly one `docs/factory/runs/<WIL-ID>/<RUN-ID>.md` from [`RUN-LEDGER-TEMPLATE.md`](RUN-LEDGER-TEMPLATE.md), and create/update that ticket's `docs/factory/runs/<WIL-ID>/INDEX.md`; never create fake entries. `RUN-ID` is exactly `YYYYMMDDTHHMMSSZ-<short-head>-NN`, where `NN` is a two-digit, zero-padded sequence starting at `01`.
6. Before creating a record, read the per-ticket `INDEX.md`. For the same exact `started_utc` timestamp second and exact head, choose one greater than the largest existing `NN`. Create the run record and its index row together in the same PR/commit. Review/CI static validation checks uniqueness. If concurrent branches allocate the same ID, the later-to-merge branch must rebase, re-read the index, allocate the next `NN`, and update both its record filename and row before merge; this is deterministic and makes no locking claim.
7. Richard records the final decision in Linear. Missing evidence or a no-go state must be explicit; silence is not approval.

## Gate matrix

| Work | Required evidence |
|---|---|
| Baseline-only documentation/process | Exact-head baseline CI, passing |
| UI/Compose, permissions, location/GPS replay, foreground service, audio/TTS, MapLibre/rendering, lifecycle, or Android runtime-specific behavior | Exact-head baseline CI plus API-35 device artifact with automated, device, and visual conclusions |
| Explicit user-requested release | Reviewed successful applicable gates and unchanged release artifact; release includes baseline/device evidence as applicable |

Any missing, stale, failed, cancelled, skipped, or unreviewed mandatory evidence is `NO-GO/BLOCKED`.

## Terra protocol

For every frontend-facing change or major PR, the assigned verifier/integrator or controller posts a **Terra review request as a Linear comment on the ticket** before dispatching/assigning the independent Terra reviewer. The comment must include: ticket URL, PR URL, exact 40-character head SHA, packet path, evidence links, review scope/checklist, and requested outcome. The Linear comment/assigned reviewer is the handoff; no Terra CLI or automatic dispatch is implied. Record both the request comment URL and final Terra report URL in the packet and PR, with exact head, evidence links, and outcome `PASS`, `FINDINGS`, or `BLOCKED`. `FINDINGS` requires fix/defer; every fix requires re-review on the exact new head. `BLOCKED` escalates to Richard. Terra is non-approving and non-merging and never replaces CI, device, verifier, artifact, or human gates.

## Canonical files

- [`WIL-101.md`](WIL-101.md) — contract, gate, Terra, and acceptance schema.
- [`WIL-102.md`](WIL-102.md) — independent verification lane and attestation.
- [`WIL-103.md`](WIL-103.md) — normalized ledger and comparison schema.
- [`CONTRACT-TEMPLATE.md`](CONTRACT-TEMPLATE.md) — future packet template.
- [`RUN-LEDGER-TEMPLATE.md`](RUN-LEDGER-TEMPLATE.md) — one actual run entry template.

## Run index

No run entries currently exist. README maintains links to per-ticket indexes only; it does not allocate or enumerate runs. Create `docs/factory/runs/<WIL-ID>/INDEX.md` with the first actual run. Each index has this stable table:

| run_id | exact_head_sha | started_utc | record_path | allocation_state |
|---|---|---|---|---|
| `<RUN-ID>` | `<40-character SHA>` | `<UTC timestamp, second precision>` | `docs/factory/runs/<WIL-ID>/<RUN-ID>.md` | `allocated` |

Allowed `allocation_state` values are `allocated` and `superseded`. Link each per-ticket index from this README when one exists; do not create an index or run record before an actual run.
