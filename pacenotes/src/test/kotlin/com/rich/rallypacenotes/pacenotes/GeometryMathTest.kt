package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeometryMathTest {
    @Test
    fun identicalPointsHaveZeroDistance() {
        val point = GeoPoint(0.0, 0.0)

        assertEquals(0.0, GeometryMath.distanceMeters(point, point))
    }

    @Test
    fun oneDegreeOfLongitudeAtEquatorIsAboutOneHundredElevenKilometres() {
        val distance = GeometryMath.distanceMeters(
            from = GeoPoint(0.0, 0.0),
            to = GeoPoint(0.0, 1.0),
        )

        assertTrue(distance in 111_000.0..112_000.0)
    }

    @Test
    fun distanceHandlesAntimeridianAndNearAntipodalPoints() {
        val antimeridianDistance = GeometryMath.distanceMeters(
            from = GeoPoint(0.0, 179.5),
            to = GeoPoint(0.0, -179.5),
        )
        val nearAntipodalDistance = GeometryMath.distanceMeters(
            from = GeoPoint(0.0, 0.0),
            to = GeoPoint(0.000001, 179.999999),
        )

        assertTrue(antimeridianDistance in 111_000.0..112_000.0)
        assertTrue(nearAntipodalDistance.isFinite())
        assertTrue(nearAntipodalDistance in 20_000_000.0..20_020_000.0)
    }

    @Test
    fun initialHeadingUsesNorthClockwiseDegrees() {
        assertTrue(
            GeometryMath.initialHeadingDegrees(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0)) in 89.9..90.1,
        )
        assertTrue(
            GeometryMath.initialHeadingDegrees(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)) in -0.1..0.1,
        )
    }

    @Test
    fun initialHeadingRejectsIdenticalPoints() {
        val point = GeoPoint(0.0, 0.0)

        assertFailsWith<IllegalArgumentException> {
            GeometryMath.initialHeadingDegrees(point, point)
        }
    }

    @Test
    fun signedHeadingDeltaWrapsAtNorth() {
        assertEquals(20.0, GeometryMath.signedHeadingDeltaDegrees(350.0, 10.0))
        assertEquals(-20.0, GeometryMath.signedHeadingDeltaDegrees(10.0, 350.0))
        assertEquals(0.0, GeometryMath.signedHeadingDeltaDegrees(720.0, 0.0))
        assertEquals(-180.0, GeometryMath.signedHeadingDeltaDegrees(0.0, 180.0))
    }

    @Test
    fun signedHeadingDeltaRejectsNonFiniteInputs() {
        assertFailsWith<IllegalArgumentException> {
            GeometryMath.signedHeadingDeltaDegrees(Double.NaN, 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            GeometryMath.signedHeadingDeltaDegrees(0.0, Double.POSITIVE_INFINITY)
        }
    }
}
