package com.rich.rallypacenotes.pacenotes

object CurveDetector {
    private const val HEADING_NOISE_FLOOR_DEGREES = 3.0
    private const val MINIMUM_ACCUMULATED_TURN_DEGREES = 20.0
    private const val MINIMUM_CURVE_LENGTH_METERS = 15.0
    private const val MAXIMUM_CURVE_SPAN_METERS = 250.0

    fun detect(route: NormalizedRoute): List<CurveCandidate> {
        val headings = route.samples.zipWithNext { from, to ->
            GeometryMath.initialHeadingDegrees(from.point, to.point)
        }
        val candidates = mutableListOf<CurveCandidate>()
        var groupStartIndex: Int? = null
        var groupTurnDegrees = 0.0
        var groupSign = 0

        fun finishGroup(endSampleIndex: Int) {
            val startIndex = groupStartIndex ?: return
            val startDistance = route.samples[startIndex].routeDistanceMeters
            val endDistance = route.samples[endSampleIndex].routeDistanceMeters
            if (
                kotlin.math.abs(groupTurnDegrees) >= MINIMUM_ACCUMULATED_TURN_DEGREES &&
                endDistance - startDistance >= MINIMUM_CURVE_LENGTH_METERS &&
                endDistance - startDistance <= MAXIMUM_CURVE_SPAN_METERS
            ) {
                candidates += CurveCandidate(
                    direction = if (groupTurnDegrees < 0.0) CurveDirection.LEFT else CurveDirection.RIGHT,
                    startDistanceMeters = startDistance,
                    endDistanceMeters = endDistance,
                    signedTurnDegrees = groupTurnDegrees,
                    severity = severityFor(kotlin.math.abs(groupTurnDegrees)),
                )
            }
            groupStartIndex = null
            groupTurnDegrees = 0.0
            groupSign = 0
        }

        headings.zipWithNext().forEachIndexed { index, (from, to) ->
            val delta = GeometryMath.signedHeadingDeltaDegrees(from, to)
            val sign = if (delta < 0.0) -1 else 1
            if (kotlin.math.abs(delta) < HEADING_NOISE_FLOOR_DEGREES) {
                finishGroup(index + 1)
            } else if (groupStartIndex != null && sign != groupSign) {
                finishGroup(index + 1)
                groupStartIndex = index + 1
                groupSign = sign
                groupTurnDegrees = delta
            } else {
                if (groupStartIndex == null) {
                    groupStartIndex = index + 1
                    groupSign = sign
                }
                groupTurnDegrees += delta
            }
        }
        finishGroup(route.samples.lastIndex)
        return candidates
    }

    private fun severityFor(absoluteTurnDegrees: Double): Int = when {
        absoluteTurnDegrees >= 100.0 -> 1
        absoluteTurnDegrees >= 75.0 -> 2
        absoluteTurnDegrees >= 55.0 -> 3
        absoluteTurnDegrees >= 40.0 -> 4
        absoluteTurnDegrees >= 30.0 -> 5
        else -> 6
    }
}
