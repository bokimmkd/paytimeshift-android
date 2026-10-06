package com.paytimeshift.pts.domain

import com.paytimeshift.pts.data.LocalStore
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import java.time.LocalDate

class ShiftRotationTest {
    private val first=LocalDate.of(2026,10,1)
    private val job=Job(id="j",name="Factory",extraShifts=listOf(ShiftTemplate("15:00","23:00",30,true,true),ShiftTemplate("23:00","07:00",15,false,true)))
    @Test fun twoOfEachThenOffRepeatsAndNightEndsTomorrow() {
        val rows=generateRotation(job,first,first.plusDays(13),listOf(RotationBlock(0),RotationBlock(1),RotationBlock(2),RotationBlock(null,1)),emptyList())
        assertEquals(12,rows.size)
        assertEquals(listOf("07:00","07:00","15:00","15:00","23:00","23:00","07:00","07:00","15:00","15:00","23:00","23:00"),rows.map {it.start})
        assertFalse(rows.any {it.date==first.plusDays(6).toString()})
        assertEquals(first.plusDays(5),rows[4].finishes.toLocalDate())
        assertTrue(rows[2].paidBreak);assertEquals(465L,rows[4].paidMinutes)
        assertTrue(generateRotation(job,first,first.plusDays(13),listOf(RotationBlock(0),RotationBlock(1),RotationBlock(2),RotationBlock(null,1)),rows).isEmpty())
    }
    @Test(expected=IllegalArgumentException::class) fun inactiveTemplateCannotGenerate() {
        generateRotation(job.copy(extraShifts=emptyList()),first,first.plusDays(10),listOf(RotationBlock(1)),emptyList())
    }
    @Test fun templatesRoundTripAndLegacyFirstShiftSurvives() {
        val encoded=LocalStore.encode(AppData(jobs=listOf(job)))
        assertEquals(job,LocalStore.decode(encoded).jobs.single())
        val root=JSONObject(encoded);root.getJSONArray("jobs").getJSONObject(0).remove("extraShifts")
        val old=LocalStore.decode(root.toString()).jobs.single()
        assertEquals("07:00",old.shiftTemplates()[0].start);assertFalse(old.shiftTemplates()[1].active)
    }
    @Test fun colleagueRoundTripUsesRecipientPayAndExcludesPrivateData() {
        val source=job.templateShift(first,job.shiftTemplates()[2]).copy(note="SECRET NOTE",bonus="123")
        val json=encodeColleagueSchedule(job,listOf(source),first,first.plusDays(1))
        assertFalse(json.contains("SECRET"));assertFalse(json.contains("bonus"));assertFalse(json.contains("rate"));assertFalse(json.contains("costs"))
        val target=Job(id="target",name="Other",currency="USD",rate="19")
        val row=decodeColleagueSchedule(json,target,emptyList()).single()
        assertEquals("target",row.jobId);assertEquals("19",row.rate);assertEquals("USD",row.currency);assertEquals("",row.note)
        assertEquals(15,row.breakMinutes);assertEquals(source.finishes,row.finishes)
        assertTrue(decodeColleagueSchedule(json,target,listOf(row)).isEmpty())
    }

    @Test fun replacementOnlyTouchesReviewedJobAndStartDates() {
        val before=job.templateShift(first.minusDays(1),job.shiftTemplates()[2])
        val old=job.templateShift(first,job.shiftTemplates()[0])
        val leave=job.templateShift(first.plusDays(1),job.shiftTemplates()[0]).copy(kind="Vacation")
        val after=job.templateShift(first.plusDays(2),job.shiftTemplates()[0])
        val other=job.copy(id="other").templateShift(first,job.shiftTemplates()[0])
        val data=AppData(jobs=listOf(job,job.copy(id="other")),shifts=listOf(before,old,leave,after,other))
        val removed=patternShiftsInPeriod(data.shifts,job.id,first,first.plusDays(1)).map {it.id}.toSet()
        assertEquals(setOf(old.id,leave.id),removed)
        val generated=generateRotation(job,first,first.plusDays(1),listOf(RotationBlock(1)),data.shifts.filterNot {it.id in removed})
        val next=data.withPatternChange(PatternChange(job.id,first,first.plusDays(1),generated,removed))
        assertEquals(listOf(before,after,other)+generated,next.shifts)
        assertEquals(data.jobs,next.jobs)
        assertEquals(data,LocalStore.decode(LocalStore.encode(data)))
        assertEquals(next,LocalStore.decode(LocalStore.encode(next)))
    }
    @Test fun disabledReplacementKeepsOldRowsAndAddsOnlyNewRows() {
        val old=job.templateShift(first,job.shiftTemplates()[0])
        val data=AppData(jobs=listOf(job),shifts=listOf(old))
        val generated=generateRotation(job,first,first.plusDays(1),listOf(RotationBlock(0)),data.shifts)
        assertEquals(1,generated.size)
        assertEquals(listOf(old)+generated,data.withPatternChange(PatternChange(job.id,first,first.plusDays(1),generated)).shifts)
    }
    @Test(expected=IllegalArgumentException::class) fun replacementCannotDeleteAnotherJobsShift() {
        val other=job.copy(id="other").templateShift(first,job.shiftTemplates()[0])
        val added=job.templateShift(first,job.shiftTemplates()[1])
        AppData(shifts=listOf(other)).withPatternChange(PatternChange(job.id,first,first,listOf(added),setOf(other.id)))
    }
    @Test(expected=IllegalArgumentException::class) fun emptySelectionCannotDeleteExistingSchedule() {
        val old=job.templateShift(first,job.shiftTemplates()[0])
        AppData(shifts=listOf(old)).withPatternChange(PatternChange(job.id,first,first,emptyList(),setOf(old.id)))
    }
    @Test(expected=IllegalArgumentException::class) fun staleReplacementDoesNotDeleteUnreviewedSchedule() {
        val added=job.templateShift(first,job.shiftTemplates()[1])
        AppData().withPatternChange(PatternChange(job.id,first,first,listOf(added),setOf("no-longer-present")))
    }
}
