package com.rich.rallypacenotes

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
    fun hostedMapDoesNotRequireLocalMapPackage() {
        val importedMap = File(composeTestRule.activity.filesDir, "local-maps/norcal.mbtiles")
        importedMap.delete()

        composeTestRule.activityRule.scenario.recreate()

        assertTrue("Hosted map MVP must not provision a local MBTiles package", !importedMap.exists())
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Import offline map package").assertDoesNotExist()
    }

    @Test
    fun mapFirstScreenShowsOnlyHostedMapAndEssentialControls() {
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("current location marker").assertIsDisplayed()
        composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Start replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("route fixture name").assertDoesNotExist()
    }
}
