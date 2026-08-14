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
60 degrees is treated as abrupt geometry and suppresses the candidate. This
conservative geometry-only guard retains the corpus's ordinary gentle and
sharp multi-sample curves while avoiding classification of the right-angle
junction and roundabout-like fixtures; it does not identify actual topology.

Heading changes below the 3-degree noise floor are not independently material:
when an active group has the same sign they are accumulated as sustained
sub-noise evidence, so dense sampling does not erase a smooth turn. Opposite-
sign or zero sub-floor samples are neutral evidence, not turn evidence; at
most two consecutive neutral samples are tolerated. A third ends the active
group. Thus isolated or oscillating noise remains fail-closed, while a bounded
neutral run cannot merge separated turns. Sub-floor samples do not start a new
group by themselves.
