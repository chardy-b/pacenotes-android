package com.rich.rallypacenotes.model

class RouteGeometry(
    val id: String,
    points: List<GeoPoint>,
) {
    val points: List<GeoPoint> = points.toList()
    init {
        require(id.isNotBlank()) { "Route ID must not be blank" }
        require(points.size >= 2) { "Route must contain at least two points" }
        require(points.zipWithNext().none { (first, second) -> first == second }) {
            "Route must not contain consecutive duplicate points"
        }
    }
}
