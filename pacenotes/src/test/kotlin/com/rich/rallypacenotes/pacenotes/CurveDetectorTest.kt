package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CurveDetectorTest {
    private fun fixture(id: String, vararg points: Pair<GeoPoint, Double>): NormalizedRoute =
        NormalizedRoute(id, points.map { (point, distance) -> NormalizedRouteSample(point, distance) })

    private fun point(latitude: Double, longitude: Double): GeoPoint = GeoPoint(latitude, longitude)

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
        point(0.0004, 0.0006) to 84.0,
        point(0.0006, 0.0008) to 115.0,
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
        val gentle = CurveDetector.detect(gentleFixture)
        val sharp = CurveDetector.detect(sharpFixture)
        val sBend = CurveDetector.detect(sBendFixture)

        assertEquals(1, gentle.size)
        assertEquals(CurveDirection.LEFT, gentle.single().direction)
        assertTrue(gentle.single().signedTurnDegrees < 0.0)
        assertEquals(22.0, gentle.single().startDistanceMeters)
        assertEquals(91.0, gentle.single().endDistanceMeters)
        assertEquals(5, gentle.single().severity)

        assertEquals(1, sharp.size)
        assertEquals(CurveDirection.LEFT, sharp.single().direction)
        assertTrue(sharp.single().signedTurnDegrees < 0.0)
        assertEquals(22.0, sharp.single().startDistanceMeters)
        assertEquals(53.0, sharp.single().endDistanceMeters)
        assertEquals(4, sharp.single().severity)

        assertEquals(2, sBend.size)
        assertEquals(listOf(CurveDirection.LEFT, CurveDirection.RIGHT), sBend.map { it.direction })
        assertTrue(sBend.all { it.signedTurnDegrees != 0.0 })
        assertTrue(sBend[0].signedTurnDegrees < 0.0)
        assertTrue(sBend[1].signedTurnDegrees > 0.0)
        assertEquals(22.0, sBend[0].startDistanceMeters)
        assertEquals(75.0, sBend[0].endDistanceMeters)
        assertEquals(75.0, sBend[1].startDistanceMeters)
        assertEquals(137.0, sBend[1].endDistanceMeters)
        assertEquals(listOf(4, 4), sBend.map { it.severity })
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
        val candidates = CurveDetector.detect(gentleFixture)

        assertEquals(1, candidates.size)
        assertEquals(CurveDirection.LEFT, candidates.single().direction)
        assertTrue(candidates.single().signedTurnDegrees < 0.0)
        assertEquals(emptyList(), CurveDetector.detect(straightFixture))
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
