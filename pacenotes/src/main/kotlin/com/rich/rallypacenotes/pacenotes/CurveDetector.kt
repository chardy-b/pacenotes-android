package com.rich.rallypacenotes.pacenotes

object CurveDetector {
    private const val HEADING_NOISE_FLOOR_DEGREES = 3.0
    private const val MAX_NEUTRAL_SAMPLES = 2
    private const val MAXIMUM_ABRUPT_STEP_DEGREES = 60.0
    // Spherical heading calculations can overshoot exact boundaries by tiny FP noise.
    private const val ANGULAR_COMPARISON_EPSILON_DEGREES = 1e-9
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
        var groupMaximumStepDegrees = 0.0
        var lastEvidenceSampleIndex: Int? = null
        var neutralSamples = 0
        var pendingStartIndex: Int? = null
        var pendingTurnDegrees = 0.0
        var pendingSign = 0

        fun finishGroup(endSampleIndex: Int) {
            val startIndex = groupStartIndex ?: return
            val startDistance = route.samples[startIndex].routeDistanceMeters
            val endDistance = route.samples[lastEvidenceSampleIndex ?: endSampleIndex].routeDistanceMeters
            val spanMeters = endDistance - startDistance
            if (
                kotlin.math.abs(groupTurnDegrees) >= MINIMUM_ACCUMULATED_TURN_DEGREES &&
                spanMeters >= MINIMUM_CURVE_LENGTH_METERS &&
                spanMeters <= MAXIMUM_CURVE_SPAN_METERS &&
                groupMaximumStepDegrees <= MAXIMUM_ABRUPT_STEP_DEGREES + ANGULAR_COMPARISON_EPSILON_DEGREES
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
            groupMaximumStepDegrees = 0.0
            lastEvidenceSampleIndex = null
            neutralSamples = 0
        }

        fun clearPending() {
            pendingStartIndex = null
            pendingTurnDegrees = 0.0
            pendingSign = 0
        }

        headings.zipWithNext().forEachIndexed { index, (from, to) ->
            val delta = GeometryMath.signedHeadingDeltaDegrees(from, to)
            val absoluteDelta = kotlin.math.abs(delta)
            val sign = when {
                delta < 0.0 -> -1
                delta > 0.0 -> 1
                else -> 0
            }
            if (absoluteDelta < HEADING_NOISE_FLOOR_DEGREES) {
                if (groupStartIndex != null && delta != 0.0 && sign == groupSign) {
                    groupTurnDegrees += delta
                    groupMaximumStepDegrees = maxOf(groupMaximumStepDegrees, absoluteDelta)
                    lastEvidenceSampleIndex = index + 1
                    neutralSamples = 0
                } else if (groupStartIndex != null) {
                    neutralSamples += 1
                    if (neutralSamples > MAX_NEUTRAL_SAMPLES) finishGroup(index + 1)
                } else if (groupStartIndex == null && delta != 0.0 && pendingStartIndex == null) {
                    pendingStartIndex = index + 1
                    pendingSign = sign
                    pendingTurnDegrees = delta
                } else if (groupStartIndex == null && delta != 0.0 && sign == pendingSign) {
                    pendingTurnDegrees += delta
                    if (kotlin.math.abs(pendingTurnDegrees) >= HEADING_NOISE_FLOOR_DEGREES) {
                        groupStartIndex = pendingStartIndex
                        groupSign = pendingSign
                        groupTurnDegrees = pendingTurnDegrees
                        groupMaximumStepDegrees = absoluteDelta
                        lastEvidenceSampleIndex = index + 1
                        neutralSamples = 0
                        clearPending()
                    }
                } else if (groupStartIndex == null) {
                    clearPending()
                }
            } else if (groupStartIndex != null && sign != groupSign) {
                finishGroup(index + 1)
                groupStartIndex = index + 1
                groupSign = sign
                groupTurnDegrees = delta
                groupMaximumStepDegrees = absoluteDelta
                lastEvidenceSampleIndex = index + 1
                neutralSamples = 0
            } else {
                if (groupStartIndex == null) {
                    groupStartIndex = index + 1
                    groupSign = sign
                    clearPending()
                }
                groupTurnDegrees += delta
                groupMaximumStepDegrees = maxOf(groupMaximumStepDegrees, absoluteDelta)
                lastEvidenceSampleIndex = index + 1
                neutralSamples = 0
            }
        }
        finishGroup(route.samples.lastIndex)
        return candidates
    }

    internal fun severityFor(absoluteTurnDegrees: Double): Int = when {
        absoluteTurnDegrees >= 100.0 -> 1
        absoluteTurnDegrees >= 75.0 -> 2
        absoluteTurnDegrees >= 55.0 -> 3
        absoluteTurnDegrees >= 40.0 -> 4
        absoluteTurnDegrees >= 30.0 -> 5
        else -> 6
    }
}
