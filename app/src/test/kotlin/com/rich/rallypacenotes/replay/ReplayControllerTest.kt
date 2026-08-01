package com.rich.rallypacenotes.replay

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplayControllerTest {
    @Test
    fun startPauseAdvanceAndResetHaveDeterministicState() {
        val controller = ReplayController(routeLengthMeters = 120.0)

        assertEquals(ReplayStatus.STOPPED, controller.state.status)
        controller.start()
        controller.advanceBy(25.0)
        assertEquals(ReplayStatus.RUNNING, controller.state.status)
        assertEquals(25.0, controller.state.currentDistanceMeters, 0.0)

        controller.pause()
        controller.advanceBy(25.0)
        assertEquals(25.0, controller.state.currentDistanceMeters, 0.0)

        controller.reset()
        assertEquals(ReplayStatus.STOPPED, controller.state.status)
        assertEquals(0.0, controller.state.currentDistanceMeters, 0.0)
    }
}
