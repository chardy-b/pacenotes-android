package com.rich.rallypacenotes.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RouteGeometryTest {
    @Test
    fun preservesValidRouteIdentityAndPointOrder() {
        val first = GeoPoint(51.0, -1.0)
        val second = GeoPoint(51.5, -0.5)

        val route = RouteGeometry("stage-1", listOf(first, second))

        assertEquals("stage-1", route.id)
        assertEquals(listOf(first, second), route.points)
    }

    @Test
    fun retainsOriginalPointsWhenSourceListIsMutated() {
        val first = GeoPoint(51.0, -1.0)
        val second = GeoPoint(51.5, -0.5)
        val sourcePoints = mutableListOf(first, second)

        val route = RouteGeometry("stage-1", sourcePoints)
        sourcePoints.clear()

        assertEquals(listOf(first, second), route.points)
    }

    @Test
    fun rejectsBlankRouteId() {
        assertFailsWith<IllegalArgumentException> {
            RouteGeometry(" \t", listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 1.0)))
        }
    }

    @Test
    fun rejectsRouteWithFewerThanTwoPoints() {
        assertFailsWith<IllegalArgumentException> {
            RouteGeometry("stage-1", listOf(GeoPoint(0.0, 0.0)))
        }
    }

    @Test
    fun rejectsConsecutiveDuplicatePoints() {
        val point = GeoPoint(0.0, 0.0)

        assertFailsWith<IllegalArgumentException> {
            RouteGeometry("stage-1", listOf(point, point))
        }
    }
}

class GeoPointTest {
    @Test
    fun acceptsCoordinateBounds() {
        assertEquals(GeoPoint(-90.0, -180.0), GeoPoint(-90.0, -180.0))
        assertEquals(GeoPoint(90.0, 180.0), GeoPoint(90.0, 180.0))
    }

    @Test
    fun rejectsNonFiniteLatitude() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(Double.POSITIVE_INFINITY, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(Double.NEGATIVE_INFINITY, 0.0) }
    }

    @Test
    fun rejectsNonFiniteLongitude() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, Double.NEGATIVE_INFINITY) }
    }

    @Test
    fun rejectsLatitudeOutsideBounds() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(-90.000001, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(90.000001, 0.0) }
    }

    @Test
    fun rejectsLongitudeOutsideBounds() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, -180.000001) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, 180.000001) }
    }
}
