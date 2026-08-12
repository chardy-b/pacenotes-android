package com.rich.rallypacenotes

import android.content.Context
import android.graphics.Bitmap
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import org.xmlpull.v1.XmlSerializer

/** CI-only GPS fixture for the WIL-76 Highway 1 / Shoreline Highway evidence replay. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MendocinoGpsFixtureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val evidenceDirectory = File(context.filesDir, EVIDENCE_DIRECTORY)

    @get:Rule
    val composeRule = createEmptyComposeRule()

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
        assertLogEventually("Hosted map render completed", MAP_RENDER_TIMEOUT_MILLIS)

        inject(39.3200773, -123.8027450)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3223861, -123.8015397)
        Thread.sleep(FIXTURE_INTERVAL_MILLIS)
        inject(39.3247032, -123.8003182)

        assertLogEventually("GPS fix latitude=39.3247032", CALLBACK_TIMEOUT_MILLIS)
        composeRule.waitForIdle()
        assertLogEventually(
            "Camera request mode=NAVIGATION latitude=39.3247032",
            CALLBACK_TIMEOUT_MILLIS,
        )

        composeRule.onNodeWithContentDescription("Switch to north-up map")
            .assertIsDisplayed()
        Thread.sleep(CAMERA_SETTLE_MILLIS)
        captureScreenshot("navigation-map.png")
        dumpWindowHierarchy("app-window.xml")
        captureLocationState()

        composeRule.onNodeWithContentDescription("Switch to north-up map")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Switch to navigation view")
            .assertIsDisplayed()
        assertLogEventually(
            "Camera request mode=NORTH_UP latitude=39.3247032 longitude=-123.8003182 bearing=0.0 pitch=0.0",
            CALLBACK_TIMEOUT_MILLIS,
        )
        Thread.sleep(CAMERA_SETTLE_MILLIS)
        captureScreenshot("north-up-map.png")
        dumpWindowHierarchy("north-up-window.xml")
        println("Mendocino GPS evidence captured: navigation and north-up camera states verified")
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

    private fun captureScreenshot(name: String) {
        evidenceDirectory.mkdirs()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "UiAutomation did not return a screenshot for $name"
        }
        try {
            File(evidenceDirectory, name).outputStream().use { output ->
                assertTrue(
                    "Failed to encode screenshot $name",
                    bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output),
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun dumpWindowHierarchy(name: String) {
        evidenceDirectory.mkdirs()
        val root = requireNotNull(instrumentation.uiAutomation.rootInActiveWindow) {
            "UiAutomation did not return an active window for $name"
        }
        File(evidenceDirectory, name).outputStream().use { output ->
            val serializer = android.util.Xml.newSerializer()
            serializer.setOutput(output, Charsets.UTF_8.name())
            serializer.startDocument(Charsets.UTF_8.name(), true)
            serializer.startTag(null, "hierarchy")
            writeNode(serializer, root)
            serializer.endTag(null, "hierarchy")
            serializer.endDocument()
        }
    }

    private fun writeNode(serializer: XmlSerializer, node: AccessibilityNodeInfo) {
        serializer.startTag(null, "node")
        serializer.attribute(null, "class", node.className?.toString().orEmpty())
        serializer.attribute(null, "text", node.text?.toString().orEmpty())
        serializer.attribute(null, "content-desc", node.contentDescription?.toString().orEmpty())
        serializer.attribute(null, "resource-id", node.viewIdResourceName.orEmpty())
        serializer.attribute(null, "clickable", node.isClickable.toString())
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child -> writeNode(serializer, child) }
        }
        serializer.endTag(null, "node")
    }

    private fun captureLocationState() {
        evidenceDirectory.mkdirs()
        instrumentation.uiAutomation.executeShellCommand("dumpsys location").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                File(evidenceDirectory, "location-state.txt").outputStream().use(input::copyTo)
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.rich.rallypacenotes"
        const val FIXTURE_INTERVAL_MILLIS = 2_000L
        const val LISTENER_TIMEOUT_MILLIS = 30_000L
        const val CALLBACK_TIMEOUT_MILLIS = 30_000L
        const val MAP_RENDER_TIMEOUT_MILLIS = 60_000L
        const val CAMERA_SETTLE_MILLIS = 1_000L
        const val LOG_POLL_MILLIS = 500L
        const val EVIDENCE_DIRECTORY = "wil76-evidence"
        const val PNG_QUALITY = 100
        const val ACCURACY_METRES = 3f
        const val ALTITUDE_METRES = 10.0
        const val SPEED_METRES_PER_SECOND = 10f
        const val COURSE_BEARING_DEGREES = 22f
    }
}
