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