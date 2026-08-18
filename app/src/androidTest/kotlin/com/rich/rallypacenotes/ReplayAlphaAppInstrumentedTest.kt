package com.rich.rallypacenotes

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    private fun assertStatus(status: String, distance: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Status: $status | Current distance: $distance m")
            .assertIsDisplayed()
    }

    private fun captureScenario(fileName: String) {
        composeTestRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = instrumentation.arguments
        val relativeOutputDir = arguments.getString("screenshot_output_dir")
            ?.trim()
            ?.takeUnless { it.isEmpty() }
            ?: error("screenshot_output_dir instrumentation argument must not be blank")
        require(!File(relativeOutputDir).isAbsolute) {
            "screenshot_output_dir must be a relative app-private path"
        }

        val context = instrumentation.targetContext
        val root = context.getExternalFilesDir(null)
            ?: error("app-private external-files directory is unavailable")
        val outputDir = File(root, relativeOutputDir).canonicalFile
        require(outputDir.toPath().startsWith(root.canonicalFile.toPath())) {
            "screenshot_output_dir must remain under app-private external-files"
        }
        check(outputDir.mkdirs() || outputDir.isDirectory) {
            "cannot create screenshot output directory: $outputDir"
        }

        val output = File(outputDir, fileName).canonicalFile
        require(output.parentFile == outputDir) { "scenario screenshot path escaped output directory" }
        check(UiDevice.getInstance(instrumentation).takeScreenshot(output)) {
            "UiDevice.takeScreenshot failed for $fileName"
        }
    }
}
