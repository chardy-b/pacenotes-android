package com.rich.rallypacenotes

import androidx.compose.ui.test.assertExists
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
        composeTestRule.onNodeWithContentDescription("Start replay").assertExists()
        composeTestRule.onNodeWithContentDescription("Pause replay").assertExists()
        composeTestRule.onNodeWithContentDescription("Reset replay").assertExists()
        composeTestRule.onNodeWithContentDescription("geometry-only detected candidate").assertExists()
        composeTestRule.onNodeWithContentDescription("Start replay").performClick()
        composeTestRule.onNodeWithText("Status: running | Current distance: 20.0 m").assertExists()
    }
}
