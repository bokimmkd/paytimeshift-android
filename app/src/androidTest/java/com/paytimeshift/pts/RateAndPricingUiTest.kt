package com.paytimeshift.pts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class RateAndPricingUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun settingsHasManualRateShortcutAndJobsHasNoHardcodedOldPrice() {
        ui.waitUntil(10000) {ui.onAllNodesWithText("Skip").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() || ui.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
        if(ui.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithText("Skip").performClick()
        ui.onNodeWithContentDescription("Settings").performClick()
        ui.onNodeWithText("Rate PTS").performScrollTo().assertIsDisplayed()
        captureGuideScreenshot(ui,"pts-rate-settings.png")
        ui.onNodeWithContentDescription("Close settings").performClick()
        ui.onNodeWithText("Jobs",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Buy us a coffee ☕").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("$1.99 / year").assertDoesNotExist()
        captureGuideScreenshot(ui,"pts-live-price-jobs.png")
    }
}
