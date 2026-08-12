package com.rich.rallypacenotes

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

/** CI-only GPS fixture for the WIL-76 Highway 1 / Shoreline Highway evidence replay. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MendocinoGpsFixtureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Test
    fun injectsNorthboundHighwayOneCourse() {
        replaceGpsWithTestProvider()
        instrumentation.uiAutomation.executeShellCommand(
            "am start -W -n $PACKAGE_NAME/.MainActivity",
        ).use { it.close() }
        assertLogEventually(
            "GPS provider/listener registered provider=gps",
            LISTENER_TIMEOUT_MILLIS,
        )

        inject(39.3200773, -123.8027450)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3223861, -123.8015397)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3247032, -123.8003182)

        assertLogEventually("GPS fix latitude=39.3247032", CALLBACK_TIMEOUT_MILLIS)
        assertLogEventually(
            "Camera request mode=NAVIGATION latitude=39.3247032",
            CALLBACK_TIMEOUT_MILLIS,
        )
        println("Mendocino GPS fixture ready: final production callback and navigation camera observed")
        Thread.sleep(EVIDENCE_HOLD_MILLIS)
    }

    private fun replaceGpsWithTestProvider() {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: IllegalArgumentException) {
            // No prior disposable test replacement.
        }
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER, false, false, false, false,
            true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
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

    private fun assertLogEventually(needle: String, timeoutMillis: Long) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            val output = readLogcat()
            if (output.contains(needle)) return
            Thread.sleep(LOG_POLL_MILLIS)
        }
        assertTrue("Timed out waiting for logcat: $needle", readLogcat().contains(needle))
    }

    private fun readLogcat(): String = instrumentation.uiAutomation
        .executeShellCommand("logcat -d -v brief PlatformGpsLocationEngine:I HostedMapView:I '*:S'")
        .use { descriptor -> BufferedReader(InputStreamReader(java.io.FileInputStream(descriptor.fileDescriptor))).readText() }

    private companion object {
        const val PACKAGE_NAME = "com.rich.rallypacenotes"
        const val FIXTURE_INTERVAL_MILLIS = 2_000L
        const val LISTENER_TIMEOUT_MILLIS = 30_000L
        const val CALLBACK_TIMEOUT_MILLIS = 30_000L
        const val EVIDENCE_HOLD_MILLIS = 60_000L
        const val LOG_POLL_MILLIS = 500L
        const val ACCURACY_METRES = 3f
        const val ALTITUDE_METRES = 10.0
        const val SPEED_METRES_PER_SECOND = 10f
        const val COURSE_BEARING_DEGREES = 22f
    }
}
