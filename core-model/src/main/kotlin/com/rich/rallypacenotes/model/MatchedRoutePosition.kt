package com.rich.rallypacenotes.model

data class MatchedRoutePosition(
    val routeDistanceMeters: Double,
    val matchConfidence: Double,
) {
    init {
        require(routeDistanceMeters.isFinite() && routeDistanceMeters >= 0.0) {
            "Route distance must be finite and non-negative"
        }
        require(matchConfidence.isFinite() && matchConfidence in 0.0..1.0) {
            "Match confidence must be finite and between 0 and 1"
        }
    }
}
