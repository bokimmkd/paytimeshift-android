package com.paytimeshift.pts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GuideNavigationUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun settingsGuideReplayReturnsToHelpAndLeavesDataIntact() {
        ui.waitUntil(10000) {ui.onAllNodesWithText("Skip").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() || ui.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
        if(ui.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithText("Skip").performClick()
        val store=LocalStore(ui.activity)
        val before=store.load()
        ui.onNodeWithContentDescription("Settings").performClick()
        ui.onNodeWithText("Help & Guide").performScrollTo().performClick()
        ui.onNodeWithText("Help & Guide").assertIsDisplayed()
        captureGuideScreenshot(ui,"pts-branded-help-light.png")
        ui.onNodeWithText("Watch introduction again").performClick()
        ui.onNodeWithText("Add your jobs").assertIsDisplayed()
        captureGuideScreenshot(ui,"pts-branded-introduction-jobs.png")
        repeat(3) {ui.onNodeWithText("Next").performClick()}
        ui.onNodeWithText("The full picture with PTS Premium").assertIsDisplayed()
        captureGuideScreenshot(ui,"pts-branded-introduction-premium.png")
        ui.onNodeWithText("Done").performClick()
        ui.onNodeWithText("Watch introduction again").performClick()
        ui.onNodeWithText("Close").performClick()
        ui.onNodeWithText("Help & Guide").assertIsDisplayed()
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithContentDescription("Close settings").assertIsDisplayed()
        try {
            ui.onNode(hasText("Appearance") and hasClickAction()).performClick()
            ui.onNodeWithText("Dark").performClick()
            ui.waitUntil(10000) {store.load().preferences.appearance=="Dark"}
            ui.onNodeWithText("Help & Guide").performScrollTo().performClick()
            ui.onNodeWithText("Help & Guide").assertIsDisplayed()
            captureGuideScreenshot(ui,"pts-branded-help-dark.png")
        } finally {
            store.save(before)
            ui.activityRule.scenario.recreate()
        }
        assertEquals(before,store.load())
    }
}
