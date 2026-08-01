package com.rich.rallypacenotes.pacenotes

import com.rich.rallypacenotes.model.GeoPoint

data class NormalizedRouteSample(
    val point: GeoPoint,
    val routeDistanceMeters: Double,
) {
    init {
        require(routeDistanceMeters.isFinite() && routeDistanceMeters >= 0.0) {
            "Route distance must be finite and non-negative"
        }
    }
}

data class NormalizedRoute(
    val sourceRouteId: String,
    val samples: List<NormalizedRouteSample>,
) {
    init {
        require(sourceRouteId.isNotBlank()) { "Source route ID must not be blank" }
        require(samples.size >= 2) { "Normalized route must contain at least two samples" }
        require(samples.zipWithNext().all { (first, second) ->
            second.routeDistanceMeters > first.routeDistanceMeters
        }) { "Normalized route distances must be strictly increasing" }
    }
}

enum class RouteSuppressionReason {
    DISCONTINUITY,
}

sealed interface RouteNormalizationResult {
    data class Accepted(val route: NormalizedRoute) : RouteNormalizationResult

    data class Suppressed(val reason: RouteSuppressionReason) : RouteNormalizationResult
}
