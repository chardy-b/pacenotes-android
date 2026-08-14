package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CurveDetectorTest {
    private fun fixture(id: String, vararg points: Pair<GeoPoint, Double>) =
        NormalizedRoute(id, points.map { (point, distance) -> NormalizedRouteSample(point, distance) })

    private fun point(latitude: Double, longitude: Double) = GeoPoint(latitude, longitude)

    private val straightFixture = fixture(
        "straight-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0002) to 22.0,
        point(0.0, 0.0004) to 44.0,
        point(0.0, 0.0006) to 66.0,
    )

    private val gentleFixture = fixture(
        "gentle-left-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0002) to 22.0,
        point(0.00005, 0.0004) to 45.0,
        point(0.00015, 0.0006) to 68.0,
        point(0.0003, 0.0008) to 91.0,
    )

    private val sharpFixture = fixture(
        "sharp-left-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0002) to 22.0,
        point(0.0002, 0.0004) to 53.0,
        point(0.0004, 0.0004) to 75.0,
        point(0.0004, 0.0006) to 97.0,
    )

    private val sBendFixture = fixture(
        "s-bend-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0002) to 22.0,
        point(0.0002, 0.0004) to 53.0,
        point(0.0004, 0.0004) to 75.0,
        point(0.0002, 0.0006) to 106.0,
        point(0.0, 0.0008) to 137.0,
        point(0.0, 0.0010) to 159.0,
    )

    private val shortZigZagNoiseFixture = fixture(
        "short-zig-zag-noise-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0001) to 11.0,
        point(0.00003, 0.0002) to 22.0,
        point(0.0, 0.0003) to 33.0,
        point(0.00003, 0.0004) to 44.0,
    )

    private val junctionLikeFixture = fixture(
        "junction-like-right-angle-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0, 0.0003) to 33.0,
        point(0.0003, 0.0003) to 66.0,
        point(0.0003, 0.0006) to 99.0,
    )

    private val roundaboutLikeFixture = fixture(
        "roundabout-like-cumulative-turn-v1",
        point(0.0, 0.0) to 0.0,
        point(0.0002, 0.0002) to 31.0,
        point(0.0004, 0.0) to 62.0,
        point(0.0002, -0.0002) to 93.0,
        point(0.0, 0.0) to 124.0,
    )

    @Test
    fun syntheticCorpusDocumentsConservativeShapeBoundaries() {
        assertEquals(emptyList(), CurveDetector.detect(straightFixture))
        assertEquals(1, CurveDetector.detect(gentleFixture).size)
        assertEquals(1, CurveDetector.detect(sharpFixture).size)
        assertEquals(2, CurveDetector.detect(sBendFixture).size)
        assertEquals(emptyList(), CurveDetector.detect(shortZigZagNoiseFixture))
        assertEquals(emptyList(), CurveDetector.detect(junctionLikeFixture))
        assertEquals(emptyList(), CurveDetector.detect(roundaboutLikeFixture))
    }
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
