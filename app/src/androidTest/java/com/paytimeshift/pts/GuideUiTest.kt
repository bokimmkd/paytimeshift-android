package com.paytimeshift.pts

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class GuideUiTest {
    @get:Rule val ui=createComposeRule()
    private fun screenshot(name:String) {
        ui.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        try {
            val file=File(instrumentation.targetContext.getExternalFilesDir("screenshots"),name)
            file.parentFile!!.mkdirs()
            file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        } finally {bitmap.recycle()}
    }
    @Test fun firstIntroductionNavigatesBackAndFinishesWithoutPurchase() {
        var result:Boolean?=null
        val state=StateRestorationTester(ui)
        state.setContent {MaterialTheme {IntroductionScreen(false){result=it}}}
        ui.onNodeWithText("Add your jobs").assertIsDisplayed()
        screenshot("pts-introduction-jobs.png")
        ui.onNodeWithText("Next").performClick()
        state.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Track your shifts and earnings").assertIsDisplayed()
        ui.onNodeWithText("Back").performClick()
        ui.onNodeWithText("Add your jobs").assertIsDisplayed()
        repeat(3) {ui.onNodeWithText("Next").performClick()}
        ui.onNodeWithText("The full picture with PTS Premium").assertIsDisplayed()
        ui.onNodeWithText("Premium is optional. Start with the free features.").assertIsDisplayed()
        screenshot("pts-introduction-premium.png")
        ui.onNodeWithText("Get started").performClick()
        assertEquals(true,result)
    }
    @Test fun skipCompletesWithoutOpeningSetup() {
        var result:Boolean?=null
        ui.setContent {MaterialTheme {IntroductionScreen(false){result=it}}}
        ui.onNodeWithText("Skip").performClick()
        assertEquals(false,result)
    }
    @Test fun guideExpandsOneTopicAndKeepsItAfterRecreation() {
        var replay=false
        val state=StateRestorationTester(ui)
        state.setContent {MaterialTheme {HelpGuideScreen({}, {replay=true})}}
        ui.onNodeWithText("Jobs & pay rules").performClick()
        ui.onNodeWithText("Open Jobs and tap Add job.").assertIsDisplayed()
        state.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Open Jobs and tap Add job.").assertIsDisplayed()
        ui.onNodeWithText("Shifts & rotations").performScrollTo().performClick()
        ui.onNodeWithText("Open Jobs and tap Add job.").assertDoesNotExist()
        ui.onNodeWithText("Watch introduction again").performScrollTo().performClick()
        assertTrue(replay)
    }
    @Test fun darkGuideAndLargeTextRemainScrollable() {
        ui.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                MaterialTheme(colorScheme=darkColorScheme()) {HelpGuideScreen({}, {})}
            }
        }
        screenshot("pts-help-dark-large-text.png")
        ui.onNodeWithText("Premium & backup").performScrollTo().performClick()
        ui.onNodeWithText("Free local backups are in Settings → Local backup. Save a file before restoring.").performScrollTo().assertIsDisplayed()
    }
    @Test fun lightGuideUsesEnglishAndShowsBrandedOverview() {
        ui.setContent {MaterialTheme {HelpGuideScreen({}, {})}}
        ui.onNodeWithText("Help & Guide").assertIsDisplayed()
        ui.onNodeWithText("PTS").assertIsDisplayed()
        screenshot("pts-help-light.png")
    }
    @Test fun replayUsesDoneAndAllGuideTextHasTranslations() {
        var result:Boolean?=null
        ui.setContent {MaterialTheme {IntroductionScreen(true){result=it}}}
        repeat(3) {ui.onNodeWithText("Next").performClick()}
        ui.onNodeWithText("Done").performClick()
        assertEquals(true,result)
        val required=listOf("Help & Guide","Watch introduction again","Jobs & pay rules","Shifts & rotations","Earnings & costs","Home screen widget","Add your jobs","Track your shifts and earnings","See your shifts at a glance","The full picture with PTS Premium","Premium is optional. Start with the free features.")
        languageNames.keys.filter {it!="en"}.forEach {language -> required.forEach {key ->assertNotEquals("$language: $key",key,translate(key,language))}}
    }
}
