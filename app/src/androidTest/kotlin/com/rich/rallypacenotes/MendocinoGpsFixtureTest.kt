package com.rich.rallypacenotes

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CI-only GPS fixture for the WIL-76 Highway 1 / Shoreline Highway evidence replay.
 *
 * The emulator's NMEA pipeline is not deterministic on API 35. This test runs only when
 * selected explicitly by the evidence workflow and uses Android's test-provider API to
 * deliver complete GPS [Location]s, including speed and bearing, to the real production
 * LocationManager.GPS_PROVIDER listener. It is never part of production code or the APK.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MendocinoGpsFixtureTest {
    private val locationManager = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Test
    fun injectsNorthboundHighwayOneCourse() {
        replaceGpsWithTestProvider()
        inject(39.3200773, -123.8027450)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3223861, -123.8015397)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3247032, -123.8003182)
    }

    private fun replaceGpsWithTestProvider() {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: IllegalArgumentException) {
            // The disposable emulator has no prior test replacement.
        }
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER,
            false,
            false,
            false,
            false,
            true,
            true,
            true,
            Criteria.POWER_LOW,
            Criteria.ACCURACY_FINE,
        )
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    private fun inject(latitude: Double, longitude: Double) {
        locationManager.setTestProviderLocation(
            LocationManager.GPS_PROVIDER,
            Location(LocationManager.GPS_PROVIDER).apply {
                this.latitude = latitude
                this.longitude = longitude
                accuracy = ACCURACY_METRES
                altitude = ALTITUDE_METRES
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                speed = SPEED_METRES_PER_SECOND
                bearing = COURSE_BEARING_DEGREES
            },
        )
    }

    private companion object {
        const val FIXTURE_INTERVAL_MILLIS = 2_000L
        const val ACCURACY_METRES = 3f
        const val ALTITUDE_METRES = 10.0
        const val SPEED_METRES_PER_SECOND = 10f
        const val COURSE_BEARING_DEGREES = 22f
    }
}
