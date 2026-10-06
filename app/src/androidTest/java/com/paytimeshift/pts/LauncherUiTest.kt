package com.paytimeshift.pts

import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.ShortcutManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.platform.ShortcutAction
import com.paytimeshift.pts.platform.publishPtsShortcuts
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LauncherUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()

    private fun open(action: ShortcutAction, expected: String) {
        ui.activityRule.scenario.onActivity { it.startActivity(action.intent(it)) }
        // Shortcut navigation briefly replaces the Compose root. Wait for the new
        // screen without requiring a root during that intermediate lifecycle state.
        ui.waitUntil(10000) {ui.onAllNodesWithText(expected).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
    }
    @Test fun warmShortcutsNavigateAndAddJobOnAnEmptyInstall() {
        val activity=ui.activity
        val launchIntent=Intent(activity.intent)
        try {
            open(ShortcutAction.Calendar,"Plan shifts and mark holidays")
            ui.onNodeWithText("Plan shifts and mark holidays").assertIsDisplayed()
            open(ShortcutAction.Earnings,"Estimated earnings")
            ui.onNodeWithText("Estimated earnings").assertIsDisplayed()
            open(ShortcutAction.Jobs,"Your jobs")
            ui.onNodeWithText("Your jobs").assertIsDisplayed()
            ui.waitForIdle()
            android.os.SystemClock.sleep(300)
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            val screenshot=instrumentation.uiAutomation.takeScreenshot()
            try {
                val file=File(instrumentation.targetContext.getExternalFilesDir("screenshots"),"jobs-turquoise-add-button.png")
                file.parentFile!!.mkdirs()
                file.outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
            } finally {screenshot.recycle()}
            open(ShortcutAction.AddShift,"Pay and shift rules")
            ui.onNodeWithText("Pay and shift rules").assertIsDisplayed()
        } finally {
            // MainActivity.setIntent correctly retains the shortcut intent. ActivityScenario
            // filters lifecycle events by its original launch intent, so restore that intent
            // only in the test before the rule destroys its activity.
            InstrumentationRegistry.getInstrumentation().runOnMainSync {activity.intent=launchIntent}
        }
    }
    @Test fun systemBarIconsFollowAppAppearanceAndSurviveRestart() {
        val store=com.paytimeshift.pts.data.LocalStore(ui.activity)
        val original=store.load()
        fun assertIcons(dark: Boolean) {
            ui.waitForIdle()
            ui.runOnIdle {
                val window=ui.activity.window
                val bars=androidx.core.view.WindowCompat.getInsetsController(window,window.decorView)
                assertEquals("Status bar clock/icons must contrast with PTS",!dark,bars.isAppearanceLightStatusBars)
                assertEquals("Navigation icons must contrast with PTS",!dark,bars.isAppearanceLightNavigationBars)
            }
        }
        fun screenshot(name: String) {
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            val bitmap=instrumentation.uiAutomation.takeScreenshot()
            try {
                val file=File(instrumentation.targetContext.getExternalFilesDir("screenshots"),name)
                file.parentFile!!.mkdirs()
                file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            } finally {bitmap.recycle()}
        }
        try {
            store.save(original.copy(preferences=original.preferences.copy(language="en",appearance="Light",showHints=false)))
            ui.activityRule.scenario.recreate()
            ui.waitUntil(10000) {ui.onAllNodes(hasContentDescription("Settings") and isEnabled()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
            ui.onNodeWithContentDescription("Settings").performClick()
            ui.onNodeWithText("Display").assertIsDisplayed()
            assertIcons(false)
            screenshot("status-bars-light.png")
            ui.onNode(hasText("Appearance") and hasClickAction()).performClick()
            ui.onNodeWithText("Dark").performClick()
            ui.waitUntil(10000) {store.load().preferences.appearance=="Dark"}
            assertIcons(true)
            screenshot("status-bars-dark.png")
            ui.onNode(hasText("Appearance") and hasClickAction()).performClick()
            ui.onNodeWithText("Light").performClick()
            ui.waitUntil(10000) {store.load().preferences.appearance=="Light"}
            assertIcons(false)
            ui.activityRule.scenario.recreate()
            ui.waitUntil(10000) {ui.onAllNodes(hasContentDescription("Settings") and isEnabled()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
            assertIcons(false)
        } finally {
            store.save(original)
            ui.activityRule.scenario.recreate()
        }
    }
    @Test fun profileButtonOpensTheAccountFromTheHeader() {
        ui.waitUntil(10000) {ui.onAllNodes(hasContentDescription("Account & Premium") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Account & Premium").performClick()
        ui.onNodeWithText("Account & Premium").assertIsDisplayed()
        ui.onNodeWithText("No Ads").assertIsDisplayed()
        ui.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Email address").assertExists()
        ui.onNodeWithText("Create account").performScrollTo().performClick()
        ui.onNodeWithText("Continue with Google").assertExists()
        ui.onNodeWithText("Your jobs remain on this phone. Signing in does not replace your local data.").assertExists()
    }
    @Test fun shortcutsUseTheChosenAppLanguageAndExplicitRoutes() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val manager=context.getSystemService(ShortcutManager::class.java)
        ui.activityRule.scenario.onActivity {publishPtsShortcuts(it,"mk")}
        val shortcuts=manager.dynamicShortcuts.associateBy {it.id}
        assertEquals(setOf("add_shift","calendar","earnings","jobs"),shortcuts.keys)
        assertEquals("Додај смена",shortcuts.getValue("add_shift").shortLabel.toString())
        assertEquals("Заработка",shortcuts.getValue("earnings").shortLabel.toString())
        ShortcutAction.entries.forEach {action->
            val intent=shortcuts.getValue(action.id).intent!!
            assertEquals(MainActivity::class.java.name,intent.component!!.className)
            assertEquals(action,ShortcutAction.fromIntent(intent))
        }
        assertNull(ShortcutAction.fromIntent(Intent(Intent.ACTION_VIEW)))
        ui.activityRule.scenario.onActivity {publishPtsShortcuts(it,"en")}
    }
}
