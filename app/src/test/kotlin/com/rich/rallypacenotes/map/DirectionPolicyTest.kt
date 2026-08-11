package com.rich.rallypacenotes.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionPolicyTest {
    @Test
    fun movingFreshAccurateCourseIsPreferredOverDeviceHeading() {
        val decision = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = 92.0,
                courseBearingAccuracyDegrees = 12.0,
                speedMetresPerSecond = 8.0,
                locationAgeMillis = 500,
                deviceHeadingDegrees = 180.0,
                deviceHeadingAgeMillis = 100,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.HIGH,
            ),
        )

        assertEquals(DirectionSource.COURSE, decision.source)
        assertEquals(92.0, decision.bearingDegrees, 0.001)
    }

    @Test
    fun staleOrPoorCourseFallsBackToFreshHighConfidenceDeviceHeading() {
        val staleDecision = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = 45.0,
                courseBearingAccuracyDegrees = 5.0,
                speedMetresPerSecond = 6.0,
                locationAgeMillis = 6_000,
                deviceHeadingDegrees = 270.0,
                deviceHeadingAgeMillis = 300,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.HIGH,
            ),
        )
        val inaccurateDecision = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = 45.0,
                courseBearingAccuracyDegrees = 40.0,
                speedMetresPerSecond = 6.0,
                locationAgeMillis = 300,
                deviceHeadingDegrees = 270.0,
                deviceHeadingAgeMillis = 300,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.MEDIUM,
            ),
        )

        assertEquals(DirectionSource.DEVICE_HEADING, staleDecision.source)
        assertEquals(DirectionSource.DEVICE_HEADING, inaccurateDecision.source)
        assertEquals(270.0, staleDecision.bearingDegrees, 0.001)
    }

    @Test
    fun lowSpeedMissingCourseRetainsRecentCourseBrieflyThenHoldsNorthUp() {
        val retained = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = null,
                courseBearingAccuracyDegrees = null,
                speedMetresPerSecond = 0.1,
                locationAgeMillis = 200,
                deviceHeadingDegrees = null,
                deviceHeadingAgeMillis = null,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.UNRELIABLE,
                lastReliableCourseDegrees = 358.0,
                lastReliableCourseAgeMillis = 2_000,
            ),
        )
        val northUp = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = null,
                courseBearingAccuracyDegrees = null,
                speedMetresPerSecond = 0.1,
                locationAgeMillis = 200,
                deviceHeadingDegrees = null,
                deviceHeadingAgeMillis = null,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.UNRELIABLE,
                lastReliableCourseDegrees = 358.0,
                lastReliableCourseAgeMillis = 4_000,
            ),
        )

        assertEquals(DirectionSource.RETAINED_COURSE, retained.source)
        assertEquals(358.0, retained.bearingDegrees, 0.001)
        assertEquals(DirectionSource.NORTH_UP, northUp.source)
        assertEquals(0.0, northUp.bearingDegrees, 0.001)
    }

    @Test
    fun unavailableRotationVectorLeavesNoDeviceHeadingAndFallsNorthUp() {
        val decision = DirectionPolicy.select(
            DirectionInput(
                courseBearingDegrees = null,
                courseBearingAccuracyDegrees = null,
                speedMetresPerSecond = 0.0,
                locationAgeMillis = 200,
                deviceHeadingDegrees = null,
                deviceHeadingAgeMillis = null,
                deviceHeadingAccuracy = DeviceHeadingAccuracy.UNRELIABLE,
            ),
        )

        assertEquals(DirectionSource.NORTH_UP, decision.source)
        assertEquals(0.0, decision.bearingDegrees, 0.001)
    }

    @Test
    fun navigationCameraUsesSelectedBearingAndTiltAfterRealLocation() {
        val camera = cameraSpecFor(
            viewMode = MapViewMode.NAVIGATION,
            latitude = 37.7759,
            longitude = -122.4184,
            navigationBearingDegrees = 83.0,
        )

        assertEquals(37.7759, camera.latitude, 0.001)
        assertEquals(-122.4184, camera.longitude, 0.001)
        assertEquals(83.0, camera.bearingDegrees, 0.001)
        assertEquals(NAVIGATION_CAMERA_PITCH, camera.pitchDegrees, 0.001)
        assertEquals(NAVIGATION_CAMERA_ZOOM, camera.zoom, 0.001)
    }

    @Test
    fun northUpCameraRetainsLocationButForcesBearingAndPitchToZero() {
        val camera = cameraSpecFor(
            viewMode = MapViewMode.NORTH_UP,
            latitude = 37.7759,
            longitude = -122.4184,
            navigationBearingDegrees = 83.0,
        )

        assertEquals(37.7759, camera.latitude, 0.001)
        assertEquals(-122.4184, camera.longitude, 0.001)
        assertEquals(0.0, camera.bearingDegrees, 0.001)
        assertEquals(0.0, camera.pitchDegrees, 0.001)
    }

    @Test
    fun cameraModeToggleAlwaysDescribesTheNextView() {
        assertEquals("Switch to north-up map", MapViewMode.NAVIGATION.nextActionContentDescription)
        assertEquals(MapViewMode.NORTH_UP, MapViewMode.NAVIGATION.toggled())
        assertEquals("Switch to navigation view", MapViewMode.NORTH_UP.nextActionContentDescription)
        assertEquals(MapViewMode.NAVIGATION, MapViewMode.NORTH_UP.toggled())
    }

    @Test
    fun smootherTakesShortPathAcrossNorthAndDeadbandsJitter() {
        val smoother = CircularHeadingSmoother()

        assertEquals(359.0, smoother.update(359.0, 0), 0.001)
        val acrossNorth = smoother.update(9.0, 300)
        assertTrue(
            "must turn through north rather than spin almost 360 degrees",
            acrossNorth in 0.0..9.0,
        )
        assertEquals(acrossNorth, smoother.update(acrossNorth + 1.0, 600), 0.001)
    }

    @Test
    fun smootherRateLimitsUpdates() {
        val smoother = CircularHeadingSmoother()
        assertEquals(20.0, smoother.update(20.0, 0), 0.001)
        assertEquals(20.0, smoother.update(120.0, 100), 0.001)
        assertTrue(smoother.update(120.0, 300) > 20.0)
    }
}
