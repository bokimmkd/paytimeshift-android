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
        MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF00857C),secondary=Color(0xFF00857C),primaryContainer=Color(0xFFD8EFEB),onPrimaryContainer=Color(0xFF071C43),secondaryContainer=Color(0xFFD8EFEB),onSecondaryContainer=Color(0xFF071C43),background=Color(0xFFF8FAFB),surface=Color.White,onSurface=Color(0xFF071C43),onBackground=Color(0xFF071C43),onSurfaceVariant=Color(0xFF4F5F7B),outlineVariant=Color(0xFFE5E8ED)),content=content)
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        // UiAutomation captures SurfaceFlinger pixels; allow the committed Compose
        // frame to reach the compositor after a language change.
        android.os.SystemClock.sleep(300)
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
    @Test fun threeShiftRotationUsesJobTemplatesAndPreviewsBeforeSaving() {
        val job=fixture.jobs.single().copy(extraShifts=listOf(ShiftTemplate("15:00","23:00",30,true,true),ShiftTemplate("23:00","07:00",15,false,true)))
        var saved:List<Shift>?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {PatternDialog(fixture.copy(jobs=listOf(job),shifts=emptyList()),{}) {saved=it}}}}
        ui.onNodeWithContentDescription("Second shift").performClick()
        ui.onNodeWithContentDescription("Third shift").performClick()
        ui.onNodeWithText("Rotation").performClick()
        screenshot("three-shift-rotation")
        ui.onNodeWithText("Preview shifts").performClick()
        ui.runOnIdle {org.junit.Assert.assertNull(saved)}
        ui.onNodeWithText("Add these shifts").performClick()
        ui.runOnIdle {org.junit.Assert.assertEquals(setOf("07:00","15:00","23:00"),saved!!.map {it.start}.toSet());org.junit.Assert.assertTrue(saved!!.size>20)}
    }
    @Test fun nativeTableReportsRenderInEveryLanguage() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        languageNames.keys.forEach {language->
            val file=com.paytimeshift.pts.platform.createWorkReportPdf(context,fixture.copy(preferences=Preferences(language=language)),YearMonth.of(2026,10),false)
            val destination=File(context.getExternalFilesDir("screenshots"),"table-report-$language.pdf")
            destination.parentFile!!.mkdirs();file.copyTo(destination,overwrite=true)
            android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)).use {pdf->
                org.junit.Assert.assertTrue(pdf.pageCount>0)
                pdf.openPage(0).use {page->
                    val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888)
                    bitmap.useBitmap {image->page.render(image,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(destination.parentFile,"table-report-$language.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}}
                }
            }
        }
    }
}
