package com.paytimeshift.pts

import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyPayTest {
    private val job=Job(id="j",name="Monthly job",currency="MKD",rate="180",monthlyPay=true,
        salaryPeriods=listOf(SalaryPeriod("2026-01-01",amount="40000",currency="MKD")))
    private fun eq(expected:String,actual:java.math.BigDecimal) = assertEquals(0,expected.toBigDecimal().compareTo(actual))
    @Test fun fullCalendarMonthsHaveOneSalaryWithOrWithoutShifts() {
        for(m in listOf(2,4,10)) {
            val month=YearMonth.of(2026,m)
            val rows=(1..20).map {job.templateShift(month.atDay(it),job.shiftTemplates().first())}
            for(shifts in listOf(emptyList(),rows,rows+job.templateShift(month.atDay(21),job.shiftTemplates().first()))) {
                eq("40000",monthlyAnalytics(AppData(listOf(job),shifts),month).currencies.single().gross)
            }
        }
        eq("40000",salaryTotals(job,LocalDate.of(2028,2,1),LocalDate.of(2028,2,29)).getValue("MKD"))
    }
    @Test fun effectiveDatesHistoryArchivesAndPartialRangesAreExplicit() {
        val partial=job.copy(salaryPeriods=listOf(SalaryPeriod("2026-04-16","2026-05-15","30000","MKD")))
        eq("15000",salaryTotals(partial,LocalDate.of(2026,4,1),LocalDate.of(2026,4,30)).getValue("MKD"))
        assertTrue(salaryTotals(partial,LocalDate.of(2026,3,1),LocalDate.of(2026,3,31)).isEmpty())
        assertTrue(salaryTotals(partial.copy(archived=true),LocalDate.of(2026,6,1),LocalDate.of(2026,6,30)).isEmpty())
        val edited=job.copy(salaryPeriods=salaryPeriodsAfterEdit(job.salaryPeriods,"2026-07-01","45000","MKD"))
        eq("40000",salaryTotals(edited,LocalDate.of(2026,6,1),LocalDate.of(2026,6,30)).getValue("MKD"))
        eq("45000",salaryTotals(edited,LocalDate.of(2026,7,1),LocalDate.of(2026,7,31)).getValue("MKD"))
        eq("9333.33333333",salaryTotals(job,LocalDate.of(2026,4,1),LocalDate.of(2026,4,7)).getValue("MKD"))
    }
    @Test fun salaryBasisChangesKeepEarlierSnapshotsAndDoNotInventPayAfterEmploymentEnds() {
        val hourly=Job(id="j",name="Old hourly",rate="10",currency="MKD")
        val october=hourly.templateShift(LocalDate.of(2026,10,15),hourly.shiftTemplates().first())
        val november=hourly.templateShift(LocalDate.of(2026,11,2),hourly.shiftTemplates().first())
        val monthly=job.copy(salaryPeriods=listOf(SalaryPeriod("2026-11-01",amount="40000",currency="MKD")))
        val converted=AppData(listOf(hourly),listOf(october,november)).withJob(monthly,true,LocalDate.of(2026,10,3))
        assertEquals(october,converted.shifts[0]);assertTrue(converted.shifts[1].monthlyPay)
        val ended=monthly.copy(archived=true,salaryPeriods=salaryPeriodsEnding(monthly.salaryPeriods,LocalDate.of(2026,11,1)))
        val closed=converted.withJob(ended,true,LocalDate.of(2026,11,1))
        eq("0",closed.shifts[1].earnings())
        eq("0",ended.templateShift(LocalDate.of(2026,12,1),ended.shiftTemplates().first()).earnings())
        assertTrue(monthlyAnalytics(closed,YearMonth.of(2026,12)).currencies.isEmpty())
    }

    @Test fun monthlyAdjustmentsAreIndependentAndCostsRemainSeparate() {
        val d=AppData(listOf(job.copy(costs=listOf(JobCost(amount="200",frequency="Monthly")))),listOf(job.templateShift(LocalDate.of(2026,10,1),job.shiftTemplates().first()).copy(bonus="50")),adjustments=listOf(
            MonthlyAdjustment(jobId="j",month="2026-10",type="Bonus",amount="2000",currency="MKD",reason="Performance"),
            MonthlyAdjustment(jobId="j",month="2026-10",type="Deduction",amount="500",currency="MKD",reason="Lateness"),
            MonthlyAdjustment(jobId="j",month="2026-11",type="Bonus",amount="999",currency="EUR",reason="Other")))
        val c=monthlyAnalytics(d,YearMonth.of(2026,10)).currencies.single()
        eq("40050",c.gross);eq("41550",c.adjusted);eq("200",c.costs);eq("41350",c.real)
        eq("0",monthlyAnalytics(d,YearMonth.of(2026,9)).currencies.single().monthlyBonuses)
        val yearly=yearlyAnalytics(d,2026).currencies.associateBy {it.currency}
        eq("2000",yearly.getValue("MKD").monthlyBonuses);eq("500",yearly.getValue("MKD").monthlyDeductions);eq("999",yearly.getValue("EUR").monthlyBonuses)
        assertNull(yearly.getValue("EUR").realHourly)
        assertTrue(analytics(d,LocalDate.of(2026,10,1),LocalDate.of(2026,10,30)).currencies.single().jobs.single().adjustments.isEmpty())
    }
    @Test fun extraPayIsExplicitAndLegacyEngineUnchanged() {
        val s=job.templateShift(LocalDate.of(2026,10,3),job.shiftTemplates().first()).copy(end="17:00",rules=PayRules(useHourlyRates=true,overtimeRate="100",saturdayRate="20"))
        eq("360",s.earnings());eq("360",s.copy(monthlyPay=false).earnings())
    }
    @Test fun replacingDayTouchesOnlySelectedJobAndDateAndIsReviewed() {
        val a=job.templateShift(LocalDate.of(2026,10,1),job.shiftTemplates().first())
        val b=a.copy(id="evening",start="18:00",end="21:00")
        val other=a.copy(id="other",jobId="second")
        val d=AppData(listOf(job,Job(id="second",name="Other")),listOf(a,b,other))
        for(kind in dayKinds.filter {it!="Work"}) {
            val status=a.copy(kind=kind,bonus="0",breakMinutes=0)
            val ids=dayStatusReplacements(status,d.shifts).map {it.id}.toSet()
            assertEquals(setOf(a.id,b.id),ids)
            val next=d.withManualShiftChange(ManualShiftChange(status,ids))
            assertEquals(listOf(other,status),next.shifts);eq("0",status.earnings())
            eq("40000",monthlyAnalytics(next,YearMonth.of(2026,10)).currencies.find {it.currency=="MKD"}!!.gross)
            try {d.withManualShiftChange(ManualShiftChange(status));fail("Unreviewed replacement accepted")} catch(_:IllegalArgumentException) { }
            assertEquals(setOf(status.id),dayStatusReplacements(status.copy(kind="Work"),next.shifts).map {it.id}.toSet())
        }
    }
    @Test fun newFieldsRoundTripAndOldBackupsDefaultSafely() {
        val d=AppData(listOf(job),preferences=Preferences(showHints=false,hiddenHintIds=listOf("field:Pay basis")),adjustments=listOf(MonthlyAdjustment(jobId="j",month="2026-10",type="Bonus",amount="10",currency="MKD",reason="Test")))
        assertEquals(d,LocalStore.decode(LocalStore.encode(d)))
        val legacy=AppData(listOf(Job(id="old",name="Old")))
        val json=org.json.JSONObject(LocalStore.encode(legacy));json.remove("adjustments")
        json.getJSONObject("preferences").remove("showHints");json.getJSONObject("preferences").remove("hiddenHintIds")
        json.getJSONArray("jobs").getJSONObject(0).remove("salaryPeriods");json.getJSONArray("jobs").getJSONObject(0).remove("monthlyPay")
        assertEquals(legacy,LocalStore.decode(json.toString()))
    }
}
