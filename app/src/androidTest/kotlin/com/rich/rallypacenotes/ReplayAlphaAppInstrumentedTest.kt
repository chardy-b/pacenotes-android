package com.rich.rallypacenotes

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplayAlphaAppInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchStoppedScenarioCapturesContractScreenshot() {
        assertStatus("stopped", "0.0")
        captureScenario("launch-stopped-v1.png")
    }

    @Test
    fun runningScenarioCapturesContractScreenshot() {
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        assertStatus("running", "20.0")
        captureScenario("running-v1.png")
    }

    @Test
    fun pausedScenarioCapturesContractScreenshot() {
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        composeTestRule.onNodeWithContentDescription("Pause replay").performClick()
        assertStatus("paused", "20.0")
        captureScenario("paused-v1.png")
    }

    @Test
    fun resetScenarioCapturesContractScreenshot() {
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        composeTestRule.onNodeWithContentDescription("Reset replay").performClick()
        assertStatus("stopped", "0.0")
        captureScenario("reset-v1.png")
    }

    @Test
    fun wil86FeatureScenarioCapturesContractScreenshot() {
        // The feature evidence reuses the deterministic stopped visual state while
        // independently proving the geometry candidate required by WIL-86.
        assertStatus("stopped", "0.0")
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate")
            .assertIsDisplayed()
        captureScenario("wil-86-screenshot-evidence-v1.png")
    }

    @Test
    fun wil70HostedMapFeatureScenarioCapturesContractScreenshot() {
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("map attribution").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Enable location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("current location status").assertIsDisplayed()
        awaitHostedMapRendered()
        captureScenario("wil-70-hosted-map-evidence-v1.png")
    }

    @Test
    fun wil70LifecycleRecreationScenarioCapturesAfterResume() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        assertStatus("stopped", "0.0")
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        awaitHostedMapRendered()
        captureScenario("wil-70-lifecycle-recreation-v1.png")
    }

    @Test
    fun wil70EvidenceStorageProbeWritesDurableTestOwnedExport() {
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        awaitHostedMapRendered()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val root = checkNotNull(testContext.getExternalFilesDir("factory-evidence")) {
            "test-owned external-files directory is unavailable"
        }.canonicalFile
        val outputDir = File(root, "wil-70-probe").canonicalFile
        require(outputDir.toPath().startsWith(root.toPath())) {
            "probe output directory escaped test-owned external storage"
        }
        outputDir.deleteRecursively()
        check(outputDir.mkdirs()) { "cannot create probe output directory: $outputDir" }

        val screenshot = File(outputDir, "probe.png").canonicalFile
        val hierarchy = File(outputDir, "app-window.xml").canonicalFile
        check(UiDevice.getInstance(instrumentation).takeScreenshot(screenshot)) {
            "UiDevice.takeScreenshot failed for the durable-storage probe"
        }
        UiDevice.getInstance(instrumentation).dumpWindowHierarchy(hierarchy)
        check(screenshot.isFile && screenshot.length() > 0) { "probe screenshot was empty" }
        check(hierarchy.isFile && hierarchy.length() > 0) { "probe UI XML was empty" }
        File(outputDir, "identity.txt").writeText(
            "test_package=${testContext.packageName}\n" +
                "target_package=${instrumentation.targetContext.packageName}\n" +
                "output_dir=${outputDir.absolutePath}\n",
        )
    }

    private fun awaitHostedMapRendered() {
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithText("Map status: ready").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertStatus(status: String, distance: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Status: $status | Current distance: $distance m")
            .assertIsDisplayed()
    }

    private fun captureScenario(fileName: String) {
        composeTestRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val relativeOutputDir = arguments.getString("screenshot_output_dir")
            ?.trim()
            ?.takeUnless { it.isEmpty() }
            ?: error("screenshot_output_dir instrumentation argument must not be blank")
        require(!File(relativeOutputDir).isAbsolute) {
            "screenshot_output_dir must be a relative app-private path"
        }

        val context = instrumentation.targetContext
        // The device gate extracts this same app-private tree with run-as.
        val root = context.filesDir
        val outputDir = File(root, relativeOutputDir).canonicalFile
        require(outputDir.toPath().startsWith(root.canonicalFile.toPath())) {
            "screenshot_output_dir must remain under app-private files"
        }
        check(outputDir.mkdirs() || outputDir.isDirectory) {
            "cannot create screenshot output directory: $outputDir"
        }

        val output = File(outputDir, fileName).canonicalFile
        require(output.parentFile == outputDir) { "scenario screenshot path escaped output directory" }
        check(UiDevice.getInstance(instrumentation).takeScreenshot(output)) {
            "UiDevice.takeScreenshot failed for $fileName"
        }
        val hierarchy = File(outputDir, "app-window.xml").canonicalFile
        require(hierarchy.parentFile == outputDir) { "UI hierarchy path escaped output directory" }
        UiDevice.getInstance(instrumentation).dumpWindowHierarchy(hierarchy)
        check(hierarchy.isFile && hierarchy.length() > 0) {
            "UiDevice.dumpWindowHierarchy produced no app-window.xml"
        }
    }
}
