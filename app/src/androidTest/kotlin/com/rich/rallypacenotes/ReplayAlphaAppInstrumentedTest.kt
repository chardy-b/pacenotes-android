package com.rich.rallypacenotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ReplayAlphaAppInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun replayAlphaShowsStableControlsAndGeometryCandidate() {
        composeTestRule.onNodeWithContentDescription("Start replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        composeTestRule.onNodeWithText("Status: running | Current distance: 20.0 m").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause replay").performClick()
        composeTestRule.onNodeWithText("Status: paused | Current distance: 20.0 m").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reset replay").performClick()
        composeTestRule.onNodeWithText("Status: stopped | Current distance: 0.0 m").assertIsDisplayed()
    }
}
