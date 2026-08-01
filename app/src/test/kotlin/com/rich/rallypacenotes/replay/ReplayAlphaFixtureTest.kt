package com.rich.rallypacenotes.replay

import com.rich.rallypacenotes.pacenotes.CurveDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReplayAlphaFixtureTest {
    @Test
    fun fixtureIsNormalizedAndBridgesCurveDetectorToReplayController() {
        val fixture = ReplayAlphaFixture.route
        assertEquals("synthetic-right-curve", fixture.sourceRouteId)
        assertEquals(100.0, fixture.samples.last().routeDistanceMeters, 0.0)
        assertFalse(ReplayAlphaFixture.candidates.isEmpty())
        assertEquals(CurveDirection.RIGHT, ReplayAlphaFixture.candidates.first().direction)

        val controller = ReplayController(fixture.samples.last().routeDistanceMeters)
        controller.start()
        controller.advanceBy(25.0)
        assertEquals(25.0, controller.state.currentDistanceMeters, 0.0)
    }
}
