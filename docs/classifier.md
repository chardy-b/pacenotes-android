# Geometry-only curve classifier contract

`CurveDetector.detect(route)` consumes only synthetic or normalized route geometry:
sample coordinates and their route distances. It does not use GPS, map tiles,
providers, GPX, network data, vehicle state, or maneuver metadata.

The result is an ordered list of validated curve candidates. Direction, signed
turn, span, and severity are deterministic descriptions of the observed shape.
Severity is a **shape label**, not speed advice and not a hazard prediction.
The classifier must not infer visibility, surface, crests, jumps, or safe driving
speed from geometry.

Geometry patterns that are short, noisy, discontinuous, ambiguous, or suggestive
of a junction/roundabout are conservatively suppressible. Geometry alone does
not identify a real road junction or roundabout. Actual maneuver proximity and
navigation-adapter maneuver data remain deferred.

Within each same-direction candidate, a single heading step greater than
60 degrees is treated as abrupt geometry and suppresses the candidate. The
comparison allows a 1e-9-degree tolerance for floating-point noise in the
spherical calculation, so an exact mathematical 60-degree step remains
inclusive while a 61-degree step remains suppressed. This
conservative geometry-only guard retains the corpus's ordinary gentle and
sharp multi-sample curves while avoiding classification of the right-angle
junction and roundabout-like fixtures; it does not identify actual topology.

Heading changes below the 3-degree noise floor are not independently material.
A signed delta whose absolute value is **at most 1e-8 degrees** is inclusive
numerical zero: it is assigned sign zero even when floating-point arithmetic
produces a non-zero residue. This numerical-zero deadband is distinct from the
3-degree noise floor and the 1e-9-degree abrupt-step comparison tolerance.
Numerical-zero deltas are neutral while a group is active, cannot start or
accumulate pending evidence, and do not refresh the last evidence sample, so
they cannot extend an endpoint. They therefore follow the same bounded neutral
handling as exact zero.

When no group is active, consecutive non-zero, same-sign sub-floor deltas are
retained as pending evidence; they start a group only when their cumulative
magnitude reaches 3 degrees, preserving the pending turn and span. Isolated,
zero, or oscillating sub-floor jitter resets that pending run. Once active,
same-sign sub-floor deltas continue the group. Opposite-sign or zero sub-floor
deltas are neutral evidence, not reversal evidence; at most two consecutive
neutral samples are tolerated, and a third ends the active group. A material
opposite-sign delta ends the group and starts a separate group. A bounded
neutral run therefore cannot merge separated turns.

Candidate end distance is the last sample that contributed accepted turn
evidence. Trailing neutral samples used to finish a group do not extend its
reported curve span.
