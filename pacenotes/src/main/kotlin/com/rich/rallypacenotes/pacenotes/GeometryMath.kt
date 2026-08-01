package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeometryMath {
    private const val MEAN_EARTH_RADIUS_METERS = 6_371_008.8

    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val latitudeDeltaRadians = Math.toRadians(to.latitude - from.latitude)
        val longitudeDeltaRadians = Math.toRadians(to.longitude - from.longitude)
        val fromLatitudeRadians = Math.toRadians(from.latitude)
        val toLatitudeRadians = Math.toRadians(to.latitude)
        val halfChordSquared = sin(latitudeDeltaRadians / 2.0) * sin(latitudeDeltaRadians / 2.0) +
            cos(fromLatitudeRadians) * cos(toLatitudeRadians) *
            sin(longitudeDeltaRadians / 2.0) * sin(longitudeDeltaRadians / 2.0)
        val angularDistanceRadians = 2.0 * asin(sqrt(halfChordSquared))

        return MEAN_EARTH_RADIUS_METERS * angularDistanceRadians
    }

    fun initialHeadingDegrees(from: GeoPoint, to: GeoPoint): Double {
        require(from != to) { "Heading is undefined for identical points" }

        val fromLatitudeRadians = Math.toRadians(from.latitude)
        val toLatitudeRadians = Math.toRadians(to.latitude)
        val longitudeDeltaRadians = Math.toRadians(to.longitude - from.longitude)
        val eastComponent = sin(longitudeDeltaRadians) * cos(toLatitudeRadians)
        val northComponent = cos(fromLatitudeRadians) * sin(toLatitudeRadians) -
            sin(fromLatitudeRadians) * cos(toLatitudeRadians) * cos(longitudeDeltaRadians)
        val headingDegrees = Math.toDegrees(atan2(eastComponent, northComponent))

        return (headingDegrees + 360.0) % 360.0
    }

    fun signedHeadingDeltaDegrees(from: Double, to: Double): Double {
        require(from.isFinite() && to.isFinite()) { "Headings must be finite" }

        return (((to - from + 180.0) % 360.0 + 360.0) % 360.0) - 180.0
    }
}
