package com.paytimeshift.pts

import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import java.math.BigDecimal

class AnalyticsTest {
    private fun shift(day: String, start: String="07:00",end: String="15:00",currency: String="MKD",paidBreak: Boolean=false)=Shift(jobId="j",date=day,start=start,end=end,rate="180",currency=currency,rules=PayRules(useHourlyRates=true),breakMinutes=30,paidBreak=paidBreak)
    private fun data(rows: List<Shift>,costs: List<JobCost>)=AppData(listOf(Job(id="j",name="Regular job",currency="MKD",rate="180",costs=costs)),rows)
    private fun eq(expected: String,actual: BigDecimal)=assertEquals(0,BigDecimal(expected).compareTo(actual))
    @Test fun costsDoNotChangeGrossOrSavedShiftPrices() {
        val s=shift("2026-11-02");val before=s.earnings()
        val d=data(listOf(s),listOf(JobCost(category="Fuel",amount="100",frequency="Per shift")))
        val c=monthlyAnalytics(d,YearMonth.of(2026,11)).currencies.single()
        eq("1350",c.gross);eq("100",c.costs);eq("1250",c.real);assertEquals(before,d.shifts.single().earnings())
    }
    @Test fun frequencyCountsMultipleShiftsOneWorkdayAndDisabledCost() {
        val rows=listOf(shift("2026-11-02"),shift("2026-11-02","18:00","20:00"),shift("2026-11-03"))
        val c=monthlyAnalytics(data(rows,listOf(JobCost(amount="10",frequency="Per shift"),JobCost(amount="20",frequency="Per workday"),JobCost(amount="70",frequency="Weekly"),JobCost(amount="100",frequency="Monthly"),JobCost(amount="999",enabled=false))),YearMonth.of(2026,11)).currencies.single()
        eq("240",c.costs)
    }
    @Test fun workweekIsNotChargedTwiceAcrossMonthOrYearBoundary() {
        val d=data(listOf(shift("2026-12-31"),shift("2027-01-01")),listOf(JobCost(amount="70",frequency="Weekly")))
        eq("70",yearlyAnalytics(d,2026).currencies.single().costs)
        eq("0",yearlyAnalytics(d,2027).currencies.single().costs)
    }
    @Test fun overnightStartMonthPaidBreakAndOverlapClassifications() {
        val s=shift("2026-10-31","22:00","08:00",paidBreak=true).copy(rules=PayRules(useHourlyRates=true,overtimeAfterHours=8.0,holidayDates=listOf("2026-11-01")))
        val t=s.workTime();assertEquals(570.0,t.worked,.001);assertEquals(600.0,t.paid,.001)
        assertEquals(120.0,t.overtime,.001);assertEquals(480.0,t.night,.001);assertEquals(600.0,t.weekend,.001);assertEquals(480.0,t.holiday,.001)
        assertEquals(1,monthlyAnalytics(data(listOf(s),emptyList()),YearMonth.of(2026,10)).currencies.single().time.shifts)
        assertTrue(monthlyAnalytics(data(listOf(s),emptyList()),YearMonth.of(2026,11)).currencies.isEmpty())
    }
    @Test fun currenciesAndHistoricalCurrencyStaySeparate() {
        val d=data(listOf(shift("2026-11-02"),shift("2026-11-03",currency="EUR")),listOf(JobCost(amount="100",frequency="Monthly")))
        val c=monthlyAnalytics(d,YearMonth.of(2026,11)).currencies
        assertEquals(listOf("EUR","MKD"),c.map {it.currency});eq("0",c[0].costs);eq("100",c[1].costs)
    }
    @Test fun nonWorkDoesNotAccrueCostsAndEmptyYearTrendHasTwelveMonths() {
        val d=data(listOf(shift("2026-11-02").copy(kind="Day off")),listOf(JobCost(amount="100",frequency="Monthly")))
        assertTrue(monthlyAnalytics(d,YearMonth.of(2026,11)).currencies.isEmpty());assertEquals(12,yearlyTrend(d,2026).size)
    }
    @Test fun backupRoundTripAndLegacyWithoutCosts() {
        val d=data(listOf(shift("2026-11-02")),listOf(JobCost(category="Other",name="Custom",amount="1.5")))
        assertEquals(d,LocalStore.decode(LocalStore.encode(d)))
        val json=org.json.JSONObject(LocalStore.encode(d));json.getJSONArray("jobs").getJSONObject(0).remove("costs")
        assertTrue(LocalStore.decode(json.toString()).jobs.single().costs.isEmpty())
    }
    @Test fun invalidCostAndUnsupportedFrequencyRejected() {
        listOf(JobCost(amount="-1"),JobCost(frequency="Daily"),JobCost(category="Invented")).forEach {c->
            try {LocalStore.decode(LocalStore.encode(data(emptyList(),listOf(c))));fail("Invalid costs accepted")} catch(_: IllegalArgumentException) { }
        }
    }
    @Test fun effectiveHourlyAndNegativeRealAndZeroComparison() {
        eq("100",hourlyValue(BigDecimal("750"),450.0)!!)
        assertNull(hourlyValue(BigDecimal.ONE,0.0));assertNull(percentChange(BigDecimal.ONE,BigDecimal.ZERO))
        eq("-50",percentChange(BigDecimal("50"),BigDecimal("100"))!!)
        eq("25",costPercentage(BigDecimal("10"),BigDecimal("40")))
        val c=monthlyAnalytics(data(listOf(shift("2026-11-02")),listOf(JobCost(amount="2000"))),YearMonth.of(2026,11)).currencies.single()
        eq("-650",c.real)
    }
    @Test fun annualCostsAndGrossEqualTwelveMonthTotals() {
        val d=data(listOf(shift("2026-01-02"),shift("2026-02-02"),shift("2026-12-31")),listOf(JobCost(amount="10",frequency="Monthly")))
        val annual=yearlyAnalytics(d,2026).currencies.single();val trend=yearlyTrend(d,2026)
        eq(annual.gross.toPlainString(),trend.flatMap {it.currencies}.fold(BigDecimal.ZERO){a,c->a+c.gross})
        eq(annual.costs.toPlainString(),trend.flatMap {it.currencies}.fold(BigDecimal.ZERO){a,c->a+c.costs})
    }
}
