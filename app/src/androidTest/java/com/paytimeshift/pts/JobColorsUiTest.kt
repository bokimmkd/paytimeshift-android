package com.paytimeshift.pts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JobColorsUiTest {
    @get:Rule val ui=createComposeRule()
    private val jobs=listOf(Job(id="a",name="Factory",rate="10"),Job(id="b",name="Wolt",color=0xFFFFA000L,rate="10"))
    @Test fun newJobStartsWithAnUnusedColorAndCannotSelectAnotherJobsColor() {
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {
            MaterialTheme {JobDialog(null,jobs,"EUR",{}) {_,_->}}
        }}
        ui.onNodeWithContentDescription("Color already used by another job. #2488ff").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Color already used by another job. #ffa000").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Job color #a040c5").assertIsSelected().assertIsEnabled()
        ui.onNodeWithContentDescription("Job color #00857c").performClick().assertIsSelected()
    }
    @Test fun editingKeepsItsOwnColorButBlocksAnotherJobsColor() {
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {
            MaterialTheme {JobDialog(jobs[0],jobs,"EUR",{}) {_,_->}}
        }}
        ui.onNodeWithContentDescription("Job color #2488ff").assertIsSelected().assertIsEnabled()
        ui.onNodeWithContentDescription("Color already used by another job. #ffa000").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Job color #a040c5").performClick().assertIsSelected()
    }
    @Test fun legacyDuplicateCannotBeSavedUntilAnUnusedColorIsSelected() {
        var saved:Job?=null
        val legacy=jobs[1].copy(color=jobs[0].color)
        ui.setContent {CompositionLocalProvider(LocalLanguage provides "en") {
            MaterialTheme {JobDialog(legacy,listOf(jobs[0],legacy),"EUR",{}) {job,_->saved=job}}
        }}
        ui.onNodeWithText("Save job").performClick()
        ui.onNodeWithText("Color already used by another job.").assertIsDisplayed()
        ui.runOnIdle {assertNull(saved)}
        ui.onNodeWithContentDescription("Job color #ffa000").performClick()
        ui.onNodeWithText("Save job").performClick()
        ui.runOnIdle {assertEquals(0xFFFFA000L,saved!!.color)}
    }
}
