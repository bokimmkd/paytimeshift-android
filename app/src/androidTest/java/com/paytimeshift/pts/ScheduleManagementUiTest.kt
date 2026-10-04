package com.paytimeshift.pts

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.premium.*
import com.paytimeshift.pts.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleManagementUiTest {
    @get:Rule val ui=createComposeRule()
    private val data=AppData(jobs=listOf(Job(id="f",name="Factory"),Job(id="w",name="Wolt")),shifts=listOf(
        Shift(id="f1",jobId="f",date="2026-10-07",start="07:00",end="15:00",rate="10"),
        Shift(id="f2",jobId="f",date="2026-10-08",start="07:00",end="15:00",rate="10",kind="Sick"),
        Shift(id="w1",jobId="w",date="2026-10-07",start="15:00",end="23:00",rate="10")))
    private fun screenshot(name:String) {
        ui.waitForIdle();android.os.SystemClock.sleep(300)
        val inst=InstrumentationRegistry.getInstrumentation()
        val file=File(inst.targetContext.getExternalFilesDir("screenshots"),"$name.png")
        val bitmap=inst.uiAutomation.takeScreenshot()
        try {file.parentFile!!.mkdirs();file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}} finally {bitmap.recycle()}
    }
    @Test fun deletingOneJobRequiresConfirmationAndCancelPreservesData() {
        var saved:AppData?=null
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {MaterialTheme {DeleteShiftsDialog(data,"2026-10-07",{}) {saved=data.withShiftDeletion(it)}}}}
        ui.onNodeWithText("Review deletion").performClick()
        ui.runOnIdle {assertNull(saved)}
        ui.onNodeWithText("Delete these entries?").assertIsDisplayed()
        screenshot("delete-shifts-review")
        ui.onAllNodesWithText("Cancel").onLast().performClick()
        ui.runOnIdle {assertNull(saved);assertEquals(3,data.shifts.size)}
        ui.onNodeWithText("Review deletion").performClick()
        ui.onNodeWithText("Delete").performClick()
        ui.runOnIdle {assertEquals(listOf("w1"),saved!!.shifts.map {it.id});assertEquals(data.jobs,saved!!.jobs)}
    }
    @Test fun warningShowsBothJobsFullTimesAndZeroRest() {
        val warning=scheduleWarnings(listOf(data.shifts[0],data.shifts[2]),8.0,data.jobs).single()
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {MaterialTheme {ScheduleWarningDetails(warning,data.jobs)}}}
        ui.onNodeWithText("0h between shifts").assertIsDisplayed()
        ui.onNodeWithText("Factory · 7 Oct 2026 07:00 – 15:00").assertIsDisplayed()
        ui.onNodeWithText("Wolt · 7 Oct 2026 15:00 – 23:00").assertIsDisplayed()
        screenshot("clear-shift-warning")
    }
    @Test fun existingUnboundCloudBackupOpensRestoreGuidanceBeforeAnyUpload() {
        val repo=PremiumRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        repo.unbind()
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {MaterialTheme {AccountDialog(repo,AccountStatus(uid="ui-fixture",premium=true,testAccess=true,revision=3,backupAt=123),null,{},{},{},data,{}, {})}}}
        ui.onNodeWithText("Back up now").performScrollTo().performClick()
        ui.onNodeWithText("Existing cloud backup").assertIsDisplayed()
        ui.onAllNodesWithText(existingBackupMessage).onLast().assertIsDisplayed()
        ui.onNodeWithText("Could not connect. Check your connection and try again.").assertDoesNotExist()
        screenshot("existing-cloud-guidance")
        ui.onAllNodesWithText("Restore backup").onLast().performClick()
        ui.onNodeWithText("Save backup file").assertIsDisplayed()
        screenshot("cloud-restore-save-local")
    }
    @Test fun emptyPayPeriodStillShowsCoveredDatesForEachJob() {
        val today=LocalDate.now()
        val jobs=data.jobs.map {it.copy(paydayAnchor=today.withDayOfMonth(15).toString())}
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {MaterialTheme {EarningsScreen(data.copy(jobs=jobs,shifts=emptyList()),YearMonth.from(today).toString(),{},{},{})}}}
        ui.onNodeWithText("Pay period").performClick()
        val (from,to)=payPeriod(jobs.first(),nextPayday(jobs.first(),today))
        val format=DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ENGLISH)
        ui.onAllNodesWithText("${from.format(format)} – ${to.format(format)}").assertCountEquals(2)
        ui.onNodeWithText("Factory").assertIsDisplayed();ui.onNodeWithText("Wolt").assertIsDisplayed()
        screenshot("pay-period-covered-dates")
    }
}
