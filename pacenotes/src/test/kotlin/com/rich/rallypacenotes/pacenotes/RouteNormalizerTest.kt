package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import com.rich.rallypacenotes.model.RouteGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RouteNormalizerTest {
    @Test
    fun resamplesAtFixedDistanceAndPreservesEndpoints() {
        val route = RouteGeometry(
            id = "short-stage",
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0002)),
        )

        val result = RouteNormalizer.normalize(
            route = route,
            sampleIntervalMeters = 5.0,
            maximumSegmentLengthMeters = 1_000.0,
        )

        assertTrue(result is RouteNormalizationResult.Accepted)
        val samples = result.route.samples
        assertEquals(route.points.first(), samples.first().point)
        assertEquals(route.points.last(), samples.last().point)
        assertEquals(listOf(0.0, 5.0, 10.0, 15.0, 20.0), samples.dropLast(1).map { it.routeDistanceMeters })
        assertTrue(samples.last().routeDistanceMeters in 22.0..23.0)
    }

    @Test
    fun retainsEndpointWithoutDuplicateWhenRouteLengthEqualsSampleInterval() {
        val start = GeoPoint(0.0, 0.0)
        val end = GeoPoint(0.0, 0.0001)
        val route = RouteGeometry(id = "exact-interval-stage", points = listOf(start, end))
        val routeLengthMeters = GeometryMath.distanceMeters(start, end)

        val result = RouteNormalizer.normalize(
            route = route,
            sampleIntervalMeters = routeLengthMeters,
            maximumSegmentLengthMeters = 1_000.0,
        )

        assertTrue(result is RouteNormalizationResult.Accepted)
        assertEquals(listOf(0.0, routeLengthMeters), result.route.samples.map { it.routeDistanceMeters })
        assertEquals(listOf(start, end), result.route.samples.map { it.point })
    }

    @Test
    fun maintainsSamplingCadenceAcrossSegmentBoundary() {
        val route = RouteGeometry(
            id = "two-segment-stage",
            points = listOf(
                GeoPoint(0.0, 0.0),
                GeoPoint(0.0, 0.00006),
                GeoPoint(0.0, 0.0002),
            ),
        )

        val result = RouteNormalizer.normalize(
            route = route,
            sampleIntervalMeters = 5.0,
            maximumSegmentLengthMeters = 1_000.0,
        )

        assertTrue(result is RouteNormalizationResult.Accepted)
        assertEquals(
            listOf(0.0, 5.0, 10.0, 15.0, 20.0),
            result.route.samples.dropLast(1).map { it.routeDistanceMeters },
        )
    }

    @Test
    fun suppressesRouteContainingDiscontinuousSegment() {
        val route = RouteGeometry(
            id = "gap-stage",
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.02)),
        )

        val result = RouteNormalizer.normalize(
            route = route,
            sampleIntervalMeters = 5.0,
            maximumSegmentLengthMeters = 1_000.0,
        )

        assertEquals(
            RouteNormalizationResult.Suppressed(RouteSuppressionReason.DISCONTINUITY),
            result,
        )
    }

    @Test
    fun rejectsInvalidNormalizationParameters() {
        val route = RouteGeometry(
            id = "stage",
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0001)),
        )

        assertFailsWith<IllegalArgumentException> {
            RouteNormalizer.normalize(route, sampleIntervalMeters = 0.0, maximumSegmentLengthMeters = 1_000.0)
        }
        assertFailsWith<IllegalArgumentException> {
            RouteNormalizer.normalize(route, sampleIntervalMeters = 5.0, maximumSegmentLengthMeters = Double.NaN)
        }
    }
}
