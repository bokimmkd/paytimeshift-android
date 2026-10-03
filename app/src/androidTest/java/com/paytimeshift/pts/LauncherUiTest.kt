package com.paytimeshift.pts

import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.platform.ShortcutAction
import com.paytimeshift.pts.platform.publishPtsShortcuts
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LauncherUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()

    private fun open(action: ShortcutAction, expected: String) {
        ui.activityRule.scenario.onActivity { it.startActivity(action.intent(it)) }
        ui.waitUntil(10000) {ui.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun warmShortcutsNavigateAndAddJobOnAnEmptyInstall() {
        open(ShortcutAction.Calendar,"Plan shifts and mark holidays")
        ui.onNodeWithText("Plan shifts and mark holidays").assertIsDisplayed()
        open(ShortcutAction.Earnings,"Estimated earnings")
        ui.onNodeWithText("Estimated earnings").assertIsDisplayed()
        open(ShortcutAction.Jobs,"Your jobs")
        ui.onNodeWithText("Your jobs").assertIsDisplayed()
        open(ShortcutAction.AddShift,"Pay and shift rules")
        ui.onNodeWithText("Pay and shift rules").assertIsDisplayed()
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
