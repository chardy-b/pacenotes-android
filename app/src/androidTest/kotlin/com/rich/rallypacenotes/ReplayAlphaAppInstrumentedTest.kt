package com.rich.rallypacenotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.io.File
import org.junit.Rule
import org.junit.Test

class ReplayAlphaAppInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun replayAlphaShowsLocalMapSurfaceWhenAnImportedPackageIsAvailable() {
        val appFilesDir = File(composeTestRule.activity.filesDir, "map-test")
        val importedMap = File(appFilesDir, "local-maps/norcal.mbtiles")
        importedMap.parentFile!!.mkdirs()
        importedMap.writeBytes(byteArrayOf())

        try {
            composeTestRule.setContent { ReplayAlphaApp(appFilesDir = appFilesDir) }

            composeTestRule.onNodeWithContentDescription("offline MapLibre map").assertIsDisplayed()
            composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()
        } finally {
            appFilesDir.deleteRecursively()
        }
    }

    @Test
    fun replayAlphaShowsStableControlsAndGeometryCandidate() {
        composeTestRule.onNodeWithContentDescription("Start replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("offline map unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        composeTestRule.onNodeWithText("Status: running | Current distance: 20.0 m").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause replay").performClick()
        composeTestRule.onNodeWithText("Status: paused | Current distance: 20.0 m").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reset replay").performClick()
        composeTestRule.onNodeWithText("Status: stopped | Current distance: 0.0 m").assertIsDisplayed()
    }
}
