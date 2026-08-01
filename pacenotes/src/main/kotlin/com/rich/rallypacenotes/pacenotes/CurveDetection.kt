package com.rich.rallypacenotes.pacenotes

enum class CurveDirection {
    LEFT,
    RIGHT,
}

data class CurveCandidate(
    val direction: CurveDirection,
    val startDistanceMeters: Double,
    val endDistanceMeters: Double,
    val signedTurnDegrees: Double,
    val severity: Int,
) {
    init {
        require(startDistanceMeters.isFinite() && startDistanceMeters >= 0.0) {
            "Start distance must be finite and non-negative"
        }
        require(endDistanceMeters.isFinite() && endDistanceMeters > startDistanceMeters) {
            "End distance must be finite and greater than start distance"
        }
        require(signedTurnDegrees.isFinite() && signedTurnDegrees != 0.0) {
            "Signed turn must be finite and non-zero"
        }
        require(direction == CurveDirection.LEFT == (signedTurnDegrees < 0.0)) {
            "Curve direction must match signed turn"
        }
        require(severity in 1..6) { "Severity must be in 1..6" }
    }
}
