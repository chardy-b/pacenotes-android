package com.rich.rallypacenotes.pacenotes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CurveDetectorTest {
    @Test
    fun curveCandidateCapturesValidatedGeometryOnlyClassification() {
        val candidate = CurveCandidate(
            direction = CurveDirection.LEFT,
            startDistanceMeters = 100.0,
            endDistanceMeters = 140.0,
            signedTurnDegrees = -42.0,
            severity = 4,
        )

        assertEquals(CurveDirection.LEFT, candidate.direction)
        assertEquals(-42.0, candidate.signedTurnDegrees)
        assertEquals(4, candidate.severity)
    }

    @Test
    fun curveCandidateRejectsInconsistentOrInvalidGeometry() {
        assertFailsWith<IllegalArgumentException> {
            CurveCandidate(CurveDirection.LEFT, 100.0, 100.0, -40.0, 4)
        }
        assertFailsWith<IllegalArgumentException> {
            CurveCandidate(CurveDirection.RIGHT, 100.0, 140.0, -40.0, 4)
        }
        assertFailsWith<IllegalArgumentException> {
            CurveCandidate(CurveDirection.LEFT, 100.0, 140.0, -40.0, 7)
        }
    }
}
