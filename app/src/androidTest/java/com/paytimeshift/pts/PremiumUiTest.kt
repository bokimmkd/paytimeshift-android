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
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {PatternDialog(fixture.copy(jobs=listOf(job),shifts=emptyList()),{}) {saved=it.added}}}}
        ui.onNodeWithContentDescription("Second shift").performClick()
        ui.onNodeWithContentDescription("Third shift").performClick()
        ui.onNodeWithText("Rotation").performClick()
        screenshot("three-shift-rotation")
        ui.onNodeWithText("Preview shifts").performClick()
        ui.runOnIdle {org.junit.Assert.assertNull(saved)}
        ui.onNodeWithText("Add these shifts").performClick()
        ui.runOnIdle {org.junit.Assert.assertEquals(setOf("07:00","15:00","23:00"),saved!!.map {it.start}.toSet());org.junit.Assert.assertTrue(saved!!.size>20)}
    }
    @Test fun rotationDaysCanBeClearedAndInvalidPreviewCannotUseTheOldNumber() {
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {PatternDialog(fixture.copy(shifts=emptyList()),{}) {}}}}
        ui.onNodeWithText("Rotation").performClick()
        ui.onAllNodesWithContentDescription("Days").onFirst().performTextClearance()
        ui.onAllNodesWithContentDescription("Days").onFirst().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("")))
        ui.onNodeWithText("Preview shifts").performClick()
        ui.onNodeWithText("Add these shifts").assertDoesNotExist()
        ui.onAllNodesWithText("Enter 1–366 days.").onFirst().assertExists()
        ui.onAllNodesWithContentDescription("Days").onFirst().performTextInput("3")
        ui.onNodeWithText("Preview shifts").performClick()
        ui.onNodeWithText("Add these shifts").assertExists()
    }
    @Test fun premiumCloudActionsAndUnresolvedStatusAreDistinct() {
        val repo=PremiumRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        var resolved by mutableStateOf(true)
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {
            AccountDialog(repo,AccountStatus(uid="ui-fixture",email="owner@example.test",verified=true,premium=true,testAccess=true),"$1.99",{},{},{},fixture,{}, {},resolved=resolved)
        }}}
        ui.onNodeWithText("Premium active").assertIsDisplayed()
        ui.onNodeWithText("Back up now").performScrollTo().assertIsEnabled()
        ui.onNodeWithText("Restore backup").assertIsEnabled()
        ui.runOnIdle {resolved=false}
        ui.onNodeWithText("Back up now").assertIsNotEnabled()
        ui.onNodeWithText("Premium is required.").assertDoesNotExist()
        ui.onNodeWithText("Subscribe yearly").assertDoesNotExist()
    }
    @Test fun calendarKeepsAbsenceAndWorkMarkersAlongsideRestIndicator() {
        val factory=Job(id="factory",name="Factory",rate="10",color=0xFFDE5353)
        val wolt=Job(id="wolt",name="Wolt",rate="10")
        val rows=listOf(
            Shift(jobId="factory",date="2026-10-07",start="07:00",end="15:00",rate="10",currency="EUR"),
            Shift(jobId="wolt",date="2026-10-07",start="16:00",end="20:00",rate="10",currency="EUR"),
            Shift(jobId="factory",date="2026-10-08",start="07:00",end="15:00",rate="10",currency="EUR",kind="Vacation"),
            Shift(jobId="wolt",date="2026-10-08",start="18:00",end="21:00",rate="10",currency="EUR"))
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {
            CalendarScreen(AppData(jobs=listOf(factory,wolt),shifts=rows),"2026-10",{},{},{},{},{},{})
        }}}
        ui.onNodeWithContentDescription("Factory: Vacation",useUnmergedTree=true).assertExists()
        ui.onAllNodesWithContentDescription("Wolt: Work",useUnmergedTree=true).assertCountEquals(2)
        ui.onNodeWithText("−",useUnmergedTree=true).assertExists()
        ui.onNodeWithText("!",useUnmergedTree=true).assertExists()
        screenshot("calendar-rest-and-absence-markers")
    }
    @Test fun manualShiftOverlapCanBeEditedOrExplicitlySaved() {
        val first=Job(id="first",name="Morning job",rate="10")
        val second=Job(id="second",name="Second job",rate="12",defaultStart="10:00",defaultEnd="18:00")
        val existing=first.templateShift(java.time.LocalDate.of(2026,10,3),first.shiftTemplates().first())
        val data=AppData(jobs=listOf(second,first),shifts=listOf(existing))
        var saved:Shift?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {ShiftDialog(null,data,"2026-10-03",{},{}) {saved=it.shift}}}}
        ui.onNodeWithText("Save shift").performClick()
        ui.onNodeWithText("Overlapping shifts").assertIsDisplayed()
        ui.onNodeWithText("Morning job").assertIsDisplayed()
        ui.runOnIdle {org.junit.Assert.assertNull(saved)}
        ui.onNodeWithText("Edit shift").performClick()
        ui.onNodeWithText("Overlapping shifts").assertDoesNotExist()
        ui.runOnIdle {org.junit.Assert.assertNull(saved);org.junit.Assert.assertEquals(listOf(existing),data.shifts)}
        ui.onNodeWithText("Save shift").performClick()
        screenshot("manual-shift-overlap-confirmation")
        ui.onNodeWithText("Save anyway").performClick()
        ui.runOnIdle {org.junit.Assert.assertEquals("second",saved!!.jobId);org.junit.Assert.assertEquals("10:00",saved!!.start)}
    }

    @Test fun adjacentManualShiftSavesWithoutOverlapConfirmation() {
        val first=Job(id="first",name="Morning job",rate="10")
        val second=Job(id="second",name="Second job",rate="12",defaultStart="15:00",defaultEnd="18:00")
        val existing=first.templateShift(java.time.LocalDate.of(2026,10,3),first.shiftTemplates().first())
        val data=AppData(jobs=listOf(second,first),shifts=listOf(existing))
        var saved:Shift?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {ShiftDialog(null,data,"2026-10-03",{},{}) {saved=it.shift}}}}
        ui.onNodeWithText("Save shift").performClick()
        ui.onNodeWithText("Overlapping shifts").assertDoesNotExist()
        ui.runOnIdle {org.junit.Assert.assertNotNull(saved)}
    }

    @Test fun dayStatusReplacementKeepsTheSecondJob() {
        val first=Job(id="first",name="First job",rate="10")
        val second=Job(id="second",name="Second job",rate="12")
        val date=java.time.LocalDate.of(2026,10,3)
        val a=first.templateShift(date,first.shiftTemplates().first())
        val b=a.copy(id="late",start="18:00",end="21:00")
        val other=second.templateShift(date,second.shiftTemplates().first())
        val data=AppData(listOf(first,second),listOf(a,b,other))
        var saved:ManualShiftChange?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {ShiftDialog(a,data,date.toString(),{},{}) {saved=it}}}}
        ui.onNodeWithText("Work").performClick();ui.onNodeWithText("Off").performClick()
        ui.onNodeWithText("Starts").assertDoesNotExist()
        ui.onNodeWithText("Save shift").performClick()
        ui.onNodeWithText("Replace this job’s day?").assertIsDisplayed()
        ui.runOnIdle {org.junit.Assert.assertNull(saved)}
        screenshot("day-status-replacement")
        ui.onNodeWithText("Replace entries").performClick()
        ui.runOnIdle {
            val next=data.withManualShiftChange(saved!!)
            org.junit.Assert.assertEquals(setOf(a.id,b.id),saved!!.replacedIds)
            org.junit.Assert.assertEquals(other,next.shifts.first {it.jobId=="second"})
            org.junit.Assert.assertEquals("Off",next.shifts.single {it.jobId=="first"}.kind)
        }
    }

    @Test fun fieldHintsDismissIndependentlyAndUseSelectedLanguage() {
        var preferences by mutableStateOf(Preferences())
        var value by mutableStateOf("")
        val explanation=fieldHints.getValue("Monthly salary")
        ui.setContent {CompositionLocalProvider(LocalLanguage provides preferences.language) {Theme {
            HintProvider(preferences,{preferences=it}) {androidx.compose.foundation.layout.Column {
                CompactField("Monthly salary",value,{value=it})
                CompactField("Rest (hours, e.g. 1.5)","1.5",{})
            }}
        }}}
        ui.onNodeWithText(explanation).assertDoesNotExist()
        ui.onNodeWithContentDescription("Monthly salary").performClick()
        ui.onNodeWithText(explanation).assertIsDisplayed()
        ui.onNodeWithContentDescription("Monthly salary").performTextInput("40000")
        ui.runOnIdle {org.junit.Assert.assertEquals("40000",value);preferences=preferences.copy(language="mk")}
        ui.onNodeWithText(translate(explanation,"mk")).assertIsDisplayed()
        screenshot("localized-field-hint")
        ui.onNodeWithContentDescription(translate("Hide this hint","mk")).performClick()
        ui.runOnIdle {org.junit.Assert.assertTrue("field:Monthly salary" in preferences.hiddenHintIds);preferences=preferences.copy(language="en")}
        ui.onNodeWithContentDescription("Rest (hours, e.g. 1.5)").performClick()
        ui.onNodeWithText(fieldHints.getValue("Rest (hours, e.g. 1.5)")).assertIsDisplayed()
        ui.runOnIdle {preferences=preferences.copy(showHints=false)}
        ui.onNodeWithText(fieldHints.getValue("Rest (hours, e.g. 1.5)")).assertDoesNotExist()
        ui.runOnIdle {preferences=preferences.copy(showHints=true)}
        ui.onNodeWithContentDescription("Monthly salary").performClick()
        ui.onNodeWithText(explanation).assertDoesNotExist()
    }

    @Test fun updateOfferCanBeDeferredWithoutBlockingTheApp() {
        var visible by mutableStateOf(true)
        var downloads=0
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "mk") {Theme {
            if(visible) UpdateOffer(false,{downloads++},{visible=false}) else UiText("Today")
        }}}
        ui.onNodeWithText(translate("New version available","mk")).assertIsDisplayed()
        screenshot("branded-update-offer")
        ui.onNodeWithText(translate("Later","mk")).performClick()
        ui.onNodeWithText(translate("Today","mk")).assertIsDisplayed()
        ui.runOnIdle {org.junit.Assert.assertEquals(0,downloads)}
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

    @Test fun replacingExistingPatternRequiresConfirmationAndCancelKeepsData() {
        val job=fixture.jobs.single()
        val date=java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
        val old=job.templateShift(date,job.shiftTemplates().first())
        val otherJob=job.copy(id="other",name="Other job")
        val other=otherJob.templateShift(date,otherJob.shiftTemplates().first())
        val data=fixture.copy(jobs=listOf(job,otherJob),shifts=listOf(old,other))
        var saved:PatternChange?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {Theme {PatternDialog(data,{}) {saved=it}}}}
        ui.onNodeWithText("Replace existing shifts in this period").performScrollTo().performClick()
        ui.onNodeWithText("Preview shifts").performClick()
        ui.runOnIdle {org.junit.Assert.assertNull(saved)}
        ui.onNodeWithText("Add these shifts").performClick()
        ui.onNodeWithText("Replace existing shifts?").assertIsDisplayed()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle {org.junit.Assert.assertNull(saved);org.junit.Assert.assertEquals(listOf(old,other),data.shifts)}
        ui.onNodeWithText("Add these shifts").performClick()
        screenshot("replace-pattern-confirmation")
        ui.onNodeWithText("Replace shifts").performClick()
        ui.runOnIdle {
            org.junit.Assert.assertEquals(setOf(old.id),saved!!.removedIds)
            val next=data.withPatternChange(saved!!)
            org.junit.Assert.assertTrue(other in next.shifts)
            org.junit.Assert.assertFalse(old in next.shifts)
            org.junit.Assert.assertTrue(next.shifts.any {it.jobId==job.id && it.date==date.toString()})
        }
    }
}
