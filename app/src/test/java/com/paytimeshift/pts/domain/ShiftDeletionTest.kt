package com.paytimeshift.pts.domain

import com.paytimeshift.pts.premium.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ShiftDeletionTest {
    private val from=LocalDate.of(2026,10,1)
    private val until=LocalDate.of(2026,10,31)
    private val factory=Job(id="f",name="Factory")
    private val wolt=Job(id="w",name="Wolt")
    private fun shift(id:String,job:String="f",date:String="2026-10-07",kind:String="Work")=Shift(id=id,jobId=job,date=date,start="23:00",end="07:00",rate="10",kind=kind)
    @Test fun selectedJobDeletionIncludesLeaveAndPreservesOtherJobsAndSettings() {
        val data=AppData(jobs=listOf(factory,wolt),shifts=listOf(shift("work"),shift("leave",kind="Sick"),shift("other","w"),shift("before",date="2026-09-30"),shift("after",date="2026-11-01")))
        val rows=shiftsForDeletion(data.shifts,"f",from,until)
        assertEquals(setOf("work","leave"),rows.map {it.id}.toSet())
        val next=data.withShiftDeletion(ShiftDeletion("f",from,until,rows.map {it.id}.toSet()))
        assertEquals(listOf("other","before","after"),next.shifts.map {it.id})
        assertEquals(data.jobs,next.jobs);assertEquals(data.preferences,next.preferences)
        assertEquals(5,data.shifts.size)
    }
    @Test fun allJobsAndInclusiveStartDatesIncludeOvernightAtTheEndOnly() {
        val rows=listOf(shift("before",date="2026-09-30"),shift("first",date="2026-10-01"),shift("last","w",date="2026-10-31"),shift("after",date="2026-11-01"))
        assertEquals(listOf("first","last"),shiftsForDeletion(rows,null,from,until).map {it.id})
    }
    @Test fun changedScheduleMustBeReviewedAgain() {
        val original=AppData(jobs=listOf(factory),shifts=listOf(shift("a")))
        val change=ShiftDeletion("f",from,until,setOf("a"))
        assertTrue(runCatching {original.copy(shifts=original.shifts+shift("b")).withShiftDeletion(change)}.isFailure)
        assertTrue(runCatching {original.withShiftDeletion(change.copy(reviewedIds=setOf("other")))}.isFailure)
        assertEquals(1,original.shifts.size)
    }
    @Test fun reversedRangeCannotDeleteAnything() {
        assertTrue(runCatching {shiftsForDeletion(listOf(shift("a")),null,until,from)}.isFailure)
    }
    @Test fun deletingWorkNeverDeletesFixedSalaryOrMonthlyAdjustments() {
        val job=factory.copy(monthlyPay=true,salaryPeriods=listOf(SalaryPeriod(from="2026-10-01",amount="40000",currency="MKD")))
        val data=AppData(jobs=listOf(job),shifts=listOf(shift("a").copy(monthlyPay=true)))
        val next=data.withShiftDeletion(ShiftDeletion("f",from,until,setOf("a")))
        assertEquals(monthlyAnalytics(data,java.time.YearMonth.of(2026,10)).currencies.single().salaryBase,monthlyAnalytics(next,java.time.YearMonth.of(2026,10)).currencies.single().salaryBase)
        assertEquals(data.adjustments,next.adjustments)
    }
    @Test fun warningIntervalsKeepJobNamesAndShowBothDatesOvernight() {
        val label=shiftIntervalLabel(shift("a"),listOf(factory),Locale.ENGLISH)
        assertEquals("Factory · 7 Oct 2026 23:00 – 8 Oct 2026 07:00",label)
        assertEquals("Factory · 7 Oct 2026 07:00 – 15:00",shiftIntervalLabel(shift("a").copy(start="07:00",end="15:00"),listOf(factory),Locale.ENGLISH))
    }
    @Test fun existingUnboundBackupHasSpecificErrorAndKeepsRestoreGuard() {
        val status=AccountStatus(uid="fixture",premium=true,revision=3,backupAt=123)
        assertTrue(cloudRestoreRequired(status,false))
        assertFalse(cloudRestoreRequired(status,true))
        assertFalse(cloudRestoreRequired(status.copy(revision=0),false))
        assertEquals(existingBackupMessage,cloudBackupLabel(status,false,true,false))
        assertEquals(existingBackupMessage,cloudError(IllegalStateException("Restore existing backup first.")))
    }
    @Test fun nextMonthlyPaymentExcludesCurrentMonthShifts() {
        val now=LocalDate.of(2026,10,4)
        val job=factory.copy(paydayAnchor="2026-10-15")
        val (a,b)=payPeriod(job,nextPayday(job,now))
        assertEquals(LocalDate.of(2026,9,1),a);assertEquals(LocalDate.of(2026,9,30),b)
        val data=AppData(jobs=listOf(job),shifts=listOf(shift("a")))
        assertTrue(analytics(data,a,b).currencies.isEmpty())
        assertEquals(480.0,monthlyAnalytics(data,java.time.YearMonth.of(2026,10)).currencies.single().time.paid,0.0)
    }
}
