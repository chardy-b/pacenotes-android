package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint
import com.rich.rallypacenotes.model.RouteGeometry

object RouteNormalizer {
    fun normalize(
        route: RouteGeometry,
        sampleIntervalMeters: Double = 5.0,
        maximumSegmentLengthMeters: Double = 1_000.0,
    ): RouteNormalizationResult {
        require(sampleIntervalMeters.isFinite() && sampleIntervalMeters > 0.0) {
            "Sample interval must be finite and positive"
        }
        require(maximumSegmentLengthMeters.isFinite() && maximumSegmentLengthMeters > 0.0) {
            "Maximum segment length must be finite and positive"
        }

        val samples = mutableListOf(NormalizedRouteSample(route.points.first(), 0.0))
        var accumulatedDistanceMeters = 0.0
        var nextSampleDistanceMeters = sampleIntervalMeters

        route.points.zipWithNext().forEach { (from, to) ->
            val segmentLengthMeters = GeometryMath.distanceMeters(from, to)
            if (segmentLengthMeters > maximumSegmentLengthMeters) {
                return RouteNormalizationResult.Suppressed(RouteSuppressionReason.DISCONTINUITY)
            }

            while (nextSampleDistanceMeters < accumulatedDistanceMeters + segmentLengthMeters) {
                val fraction = (nextSampleDistanceMeters - accumulatedDistanceMeters) / segmentLengthMeters
                samples += NormalizedRouteSample(
                    point = interpolate(from, to, fraction),
                    routeDistanceMeters = nextSampleDistanceMeters,
                )
                nextSampleDistanceMeters += sampleIntervalMeters
            }
            accumulatedDistanceMeters += segmentLengthMeters
        }

        samples += NormalizedRouteSample(route.points.last(), accumulatedDistanceMeters)
        return RouteNormalizationResult.Accepted(NormalizedRoute(route.id, samples))
    }

    private fun interpolate(from: GeoPoint, to: GeoPoint, fraction: Double): GeoPoint {
        val longitudeDelta = ((to.longitude - from.longitude + 540.0) % 360.0) - 180.0
        val longitude = ((from.longitude + longitudeDelta * fraction + 540.0) % 360.0) - 180.0

        return GeoPoint(
            latitude = from.latitude + (to.latitude - from.latitude) * fraction,
            longitude = longitude,
        )
    }
}
