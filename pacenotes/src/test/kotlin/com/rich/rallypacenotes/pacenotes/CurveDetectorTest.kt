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

    private fun headingSequenceFixture(id: String, headingChanges: List<Double>): NormalizedRoute {
        var latitude = 0.0
        var longitude = 0.0
        var heading = 90.0
        val points = mutableListOf(point(latitude, longitude) to 0.0)
        headingChanges.forEachIndexed { index, change ->
            heading += change
            val radians = Math.toRadians(heading)
            latitude += kotlin.math.cos(radians) * 0.0002
            longitude += kotlin.math.sin(radians) * 0.0002
            points += point(latitude, longitude) to (index + 1) * 20.0
        }
        return fixture(id, *points.toTypedArray())
    }


    private fun sustainedArcFixture(id: String, stepMeters: Double): NormalizedRoute {
        val coordinates = listOf(
            0.0000000 to 0.0000000,
            0.0000000 to 0.0002300,
            0.0000399 to 0.0004565,
            0.0001186 to 0.0006726,
            0.0002336 to 0.0008718,
            0.0003814 to 0.0010480,
            0.0005576 to 0.0011959,
            0.0007568 to 0.0013109,
            0.0009730 to 0.0013895,
            0.0011995 to 0.0014295,
            0.0014295 to 0.0014295,
            0.0016560 to 0.0013895,
        )
        return fixture(id, *coordinates.map { (latitude, longitude) ->
            point(latitude, longitude) to coordinates.indexOf(latitude to longitude) * stepMeters
        }.toTypedArray())
    }

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
        point(0.0006, 0.0006) to 106.0,
        point(0.0006, 0.0008) to 128.0,
    )

    private val maximumSpanArcFixture = sustainedArcFixture("maximum-span-arc-v1", 25.0)
    private val overlongSameDirectionFixture = sustainedArcFixture("overlong-same-direction-v1", 26.0)

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

    private fun headingStepFixture(id: String, secondHeadingDegrees: Double): NormalizedRoute =
        fixture(
            id,
            point(0.0, 0.0) to 0.0,
            point(0.0, 0.0002) to 20.0,
            point(
                latitude = kotlin.math.cos(Math.toRadians(secondHeadingDegrees)) * 0.0002,
                longitude = 0.0002 + kotlin.math.sin(Math.toRadians(secondHeadingDegrees)) * 0.0002,
            ) to 40.0,
        )

    @Test
    fun acceptsExactSixtyDegreeHeadingStepButSuppressesGreaterStep() {
        assertEquals(1, CurveDetector.detect(headingStepFixture("exact-60-degree-step-v1", 30.0)).size)
        assertEquals(0, CurveDetector.detect(headingStepFixture("greater-than-60-degree-step-v1", 29.0)).size)
    }

    @Test
    fun accumulatesSustainedSameDirectionSubNoiseHeadingChanges() {
        // Ten route segments produce nine computed heading deltas: 9 * 2.5 = 22.5°.
        val denseSmoothCurve = headingSequenceFixture("dense-smooth-sub-noise-v1", List(10) { 2.5 })

        val candidates = CurveDetector.detect(denseSmoothCurve)

        assertEquals(1, candidates.size)
        assertEquals(CurveDirection.RIGHT, candidates.single().direction)
        assertEquals(22.5, candidates.single().signedTurnDegrees, 1e-8)
        assertEquals(180.0, candidates.single().endDistanceMeters - candidates.single().startDistanceMeters, 1e-9)
    }

    @Test
    fun boundedNeutralRunDoesNotBridgeSubThresholdTurns() {
        val separatedSubNoiseTurns = headingSequenceFixture(
            "separated-sub-noise-turns-v1",
            List(6) { 2.5 } + List(3) { 0.0 } + List(6) { 2.5 },
        )

        assertEquals(emptyList(), CurveDetector.detect(separatedSubNoiseTurns))
    }

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
        assertEquals(128.0, sBend[1].endDistanceMeters)
        assertEquals(listOf(2, 2), sBend.map { it.severity })
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
    fun suppressesSameDirectionCandidateBeyond250Meters() {
        assertEquals(emptyList(), CurveDetector.detect(overlongSameDirectionFixture))
    }

    @Test
    fun retainsSameDirectionCandidateAtExactly250Meters() {
        val candidates = CurveDetector.detect(maximumSpanArcFixture)

        assertEquals(1, candidates.size)
        assertEquals(250.0, candidates.single().endDistanceMeters - candidates.single().startDistanceMeters)
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
    fun mapsSeverityAtEveryDocumentedBoundaryAndJustBelow() {
        val exactBoundaryExpected = mapOf(20.0 to 6, 30.0 to 5, 40.0 to 4, 55.0 to 3, 75.0 to 2, 100.0 to 1)
        val justBelowExpected = mapOf(19.9 to 6, 29.9 to 6, 39.9 to 5, 54.9 to 4, 74.9 to 3, 99.9 to 2)

        exactBoundaryExpected.forEach { (turn, severity) ->
            assertEquals(severity, CurveDetector.severityFor(turn), "Exact turn $turn")
        }
        justBelowExpected.forEach { (turn, severity) ->
            assertEquals(severity, CurveDetector.severityFor(turn), "Just below turn $turn")
        }
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
