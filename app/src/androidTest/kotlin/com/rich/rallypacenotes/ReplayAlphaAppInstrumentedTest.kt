package com.rich.rallypacenotes

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class ReplayAlphaAppInstrumentedTest {
    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeTestRule)

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
    fun mapFirstScreenDoesNotRenderAnInAppLocationPermissionButton() {
        composeTestRule.onNodeWithContentDescription("Enable current location").assertDoesNotExist()
    }

    @Test
    fun navigationCameraToggleCommunicatesTheNextAction() {
        composeTestRule.onNodeWithContentDescription("Switch to north-up map")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithContentDescription("Switch to navigation view")
            .assertIsDisplayed()
    }

    @Test
    fun mapFirstScreenShowsOnlyHostedMapAndEssentialControls() {
        composeTestRule.onNodeWithContentDescription("hosted MapLibre map").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("location permission granted").assertIsDisplayed()
        composeTestRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Start replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("route fixture name").assertDoesNotExist()
    }
}
