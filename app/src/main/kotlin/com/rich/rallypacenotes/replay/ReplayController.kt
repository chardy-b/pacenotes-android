package com.rich.rallypacenotes.replay

enum class ReplayStatus { STOPPED, RUNNING, PAUSED }

data class ReplayState(val status: ReplayStatus, val currentDistanceMeters: Double)

class ReplayController(private val routeLengthMeters: Double) {
    init { require(routeLengthMeters.isFinite() && routeLengthMeters > 0.0) }

    var state = ReplayState(ReplayStatus.STOPPED, 0.0)
        private set

    fun start() { state = state.copy(status = ReplayStatus.RUNNING) }
    fun pause() { if (state.status == ReplayStatus.RUNNING) state = state.copy(status = ReplayStatus.PAUSED) }
    fun reset() { state = ReplayState(ReplayStatus.STOPPED, 0.0) }
    fun advanceBy(meters: Double) {
        require(meters.isFinite() && meters >= 0.0)
        if (state.status == ReplayStatus.RUNNING) {
            state = state.copy(currentDistanceMeters = (state.currentDistanceMeters + meters).coerceAtMost(routeLengthMeters))
        }
    }
}
