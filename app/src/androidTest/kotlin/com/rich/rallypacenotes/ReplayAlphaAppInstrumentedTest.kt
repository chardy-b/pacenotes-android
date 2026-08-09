package com.rich.rallypacenotes

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReplayAlphaAppInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun replayAlphaProvisionsBundledMapPackageOnFirstLaunch() {
        val importedMap = File(composeTestRule.activity.filesDir, "local-maps/norcal.mbtiles")
        assertTrue("Hosted emulator must provide the verified NorCal package before the test removes it", importedMap.isFile)
        assertTrue("Test must remove the previously provisioned package", importedMap.delete())

        composeTestRule.activityRule.scenario.recreate()

        assertTrue("App must restore its bundled NorCal package into private storage", importedMap.isFile)
        composeTestRule.onNodeWithContentDescription("offline MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Import offline map package").assertIsDisplayed()
        composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()
    }

    @Test
    fun mapFirstScreenShowsOnlyTheOfflineMapAndEssentialControls() {
        composeTestRule.onNodeWithContentDescription("offline MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("current location marker").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Import offline map package").assertIsDisplayed()
        composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Start replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("route fixture name").assertDoesNotExist()
    }
}
