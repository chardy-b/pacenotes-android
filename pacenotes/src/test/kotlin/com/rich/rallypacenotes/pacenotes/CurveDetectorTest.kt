package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun detectsSustainedLeftGeometryButSuppressesStraightGeometry() {
        val leftCurve = NormalizedRoute(
            sourceRouteId = "left-fixture",
            samples = listOf(
                NormalizedRouteSample(GeoPoint(0.0, 0.0), 0.0),
                NormalizedRouteSample(GeoPoint(0.0, 0.0002), 22.0),
                NormalizedRouteSample(GeoPoint(0.0002, 0.0004), 53.0),
                NormalizedRouteSample(GeoPoint(0.0004, 0.0004), 75.0),
            ),
        )
        val straight = NormalizedRoute(
            sourceRouteId = "straight-fixture",
            samples = listOf(
                NormalizedRouteSample(GeoPoint(0.0, 0.0), 0.0),
                NormalizedRouteSample(GeoPoint(0.0, 0.0002), 22.0),
                NormalizedRouteSample(GeoPoint(0.0, 0.0004), 44.0),
            ),
        )

        val candidates = CurveDetector.detect(leftCurve)

        assertEquals(1, candidates.size)
        assertEquals(CurveDirection.LEFT, candidates.single().direction)
        assertTrue(candidates.single().signedTurnDegrees <= -80.0)
        assertEquals(emptyList(), CurveDetector.detect(straight))
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
