package com.paytimeshift.pts

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.premium.*
import com.paytimeshift.pts.ui.*
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth
import java.io.File

/** Read-only UI fixtures. No local database writes or production entitlement overrides. */
class PremiumUiTest {
    @get:Rule val ui=createComposeRule()
    private val fixture=AppData(jobs=listOf(Job(id="j",name="Regular Job",currency="MKD",rate="180",costs=listOf(JobCost(id="fuel",category="Fuel",amount="250",frequency="Per workday")))),
        shifts=(1..20).map {Shift(id="s$it",jobId="j",date="2026-10-${it.toString().padStart(2,'0')}",start="07:00",end="16:00",breakMinutes=30,rate="180",currency="MKD",rules=PayRules(useHourlyRates=true,overtimeRate="250"))})
    @Composable private fun Theme(content:@Composable ()->Unit) {
        MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF00857C),secondary=Color(0xFF00857C),background=Color(0xFFF8FAFB),surface=Color.White,onSurface=Color(0xFF071C43),onBackground=Color(0xFF071C43),onSurfaceVariant=Color(0xFF4F5F7B),outlineVariant=Color(0xFFE5E8ED)),content=content)
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        val inst=InstrumentationRegistry.getInstrumentation()
        val file=File(inst.targetContext.getExternalFilesDir("screenshots"),"$name.png")
        file.parentFile!!.mkdirs();inst.uiAutomation.takeScreenshot().useBitmap {image->file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}}
    }
    private fun Bitmap.useBitmap(block:(Bitmap)->Unit) {try {block(this)} finally {recycle()}}
    @Test fun analyticsRendersInEverySupportedLanguage() {
        var language by mutableStateOf("en")
        ui.setContent {CompositionLocalProvider(LocalLanguage provides language) {Theme {AnalyticsDialog(fixture.copy(preferences=Preferences(language=language)),YearMonth.of(2026,10),{})}}}
        languageNames.keys.forEach {code->
            ui.runOnIdle {language=code}
            ui.onAllNodesWithText(translate("Work Analytics",code)).onFirst().assertIsDisplayed()
            ui.onAllNodesWithText(translate("Real estimated earnings",code)).onFirst().assertIsDisplayed()
            screenshot("analytics-$code")
        }
    }
    @Test fun freeAccountShowsUpgradeAndNoActiveSubscription() {
        val repo=PremiumRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {AccountDialog(repo,AccountStatus(),null,{},{},{},fixture,{}, {})}}}
        ui.onNodeWithText("Subscribe yearly").assertIsNotEnabled()
        ui.onNodeWithText("Premium active").assertDoesNotExist()
        ui.onNodeWithText("No Ads").assertIsDisplayed()
        screenshot("free-premium-gate")
    }
}
