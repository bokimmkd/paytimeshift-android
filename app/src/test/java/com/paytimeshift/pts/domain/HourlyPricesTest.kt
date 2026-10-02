package com.paytimeshift.pts.domain

import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class HourlyPricesTest {
    private val prices = PayRules(useHourlyRates=true, overtimeRate="15", saturdayRate="12", sundayRate="20", holidayRate="25")
    private fun shift(date:String="2026-10-02",start:String="07:00",end:String="17:00") =
        Shift(jobId="j",date=date,start=start,end=end,rate="10",currency="EUR",rules=prices)
    private fun money(expected:String, actual:BigDecimal) = assertEquals(0,actual.compareTo(BigDecimal(expected)))

    @Test fun weekdayUsesEightRegularHoursAndTwoOvertimeHours() {
        val s=shift()
        money("110",s.earnings())
        assertEquals(listOf("Regular hours","Overtime"),s.hourlyLines().map {it.label})
        assertEquals(480.0,s.hourlyLines().first().minutes,1e-6)
    }
    @Test fun saturdayAndSundayUseConcretePrices() {
        money("96",shift(date="2026-10-03",end="15:00").earnings())
        money("160",shift(date="2026-10-04",end="15:00").earnings())
        money("126",shift(date="2026-10-03").earnings())
        money("200",shift(date="2026-10-04").earnings()) // Sunday wins over lower overtime price.
    }
    @Test fun overtimeHigherThanWeekendWinsWithoutAddingTwoFullPrices() {
        val s=shift(date="2026-10-04").copy(rules=prices.copy(overtimeRate="30"))
        money("220",s.earnings())
    }
    @Test fun holidayOverridesWeekendAndCanBeRemoved() {
        val j=Job(id="j",name="Work",rate="10",rules=prices)
        val d=AppData(listOf(j),listOf(shift(date="2026-10-04")))
        val marked=d.withHoliday("2026-10-04")
        money("250",marked.shifts.single().earnings())
        assertEquals(listOf("2026-10-04"),marked.jobs.single().rules.holidayDates)
        assertEquals(d,marked.withHoliday("2026-10-04"))
    }
    @Test fun midnightSplitsFridaySaturdayAndSaturdaySunday() {
        money("92",shift(start="22:00",end="06:00").earnings())
        money("144",shift(date="2026-10-03",start="22:00",end="06:00").earnings())
    }
    @Test fun midnightHolidayAppliesOnlyToItsCalendarDate() {
        val s=shift(start="22:00",end="06:00").copy(rules=prices.copy(holidayDates=listOf("2026-10-03")))
        money("170",s.earnings())
    }
    @Test fun unpaidBreakReducesPaidHoursAndOvertimeThreshold() {
        val s=shift().copy(breakMinutes=60)
        assertEquals(540L,s.paidMinutes);money("95",s.earnings())
        money("110",s.copy(paidBreak=true).earnings())
        val overnight=shift(start="22:00",end="06:00").copy(breakMinutes=30)
        money("86.25",overnight.earnings())
    }
    @Test fun fractionalThresholdAndBlankPrices() {
        money("107.5",shift().copy(rules=prices.copy(overtimeAfterHours=8.5)).earnings())
        money("100",shift(date="2026-10-04").copy(rules=PayRules(useHourlyRates=true)).earnings())
        money("0",shift(end="15:00").copy(rules=prices.copy(holidayRate="0",holidayDates=listOf("2026-10-02"))).earnings())
    }
    @Test fun nightPercentAndBonusRemainSeparateFromHourlyPrices() {
        val s=shift(start="22:00",end="06:00").copy(rules=prices.copy(nightPercent=20.0),bonus="4")
        money("112",s.earnings()) // 92 hourly + 16 night + 4 bonus.
        money("0",s.copy(kind="Vacation").earnings())
    }
    @Test fun futureJobUpdateKeepsHistoryAndBreakSnapshots() {
        val j=Job(id="j",name="Work",rate="10",rules=prices)
        val old=shift(date="2026-10-01").copy(breakMinutes=30,note="History")
        val current=shift().copy(breakMinutes=60,bonus="3")
        val future=shift(date="2026-10-03")
        val d=AppData(listOf(j),listOf(old,current,future))
        val changed=j.copy(rate="20",rules=prices.copy(overtimeRate="25"),defaultBreakMinutes=0)
        val updated=d.withJob(changed,true,LocalDate.of(2026,10,2))
        assertEquals(old,updated.shifts[0]);assertEquals(60,updated.shifts[1].breakMinutes)
        assertEquals("3",updated.shifts[1].bonus);assertEquals("20",updated.shifts[1].rate)
        assertEquals("25",updated.shifts[2].rules.overtimeRate)
        assertEquals(d.shifts,d.withJob(changed,false,LocalDate.of(2026,10,2)).shifts)
    }
    @Test fun legacyPercentRulesRemainUnchangedAndConvertForFutureShifts() {
        val legacy=PayRules(overtimePercent=50.0,sundayPercent=100.0)
        val s=shift(date="2026-10-04").copy(rules=legacy)
        money("210",s.earnings())
        val j=Job(id="j",name="Old",rate="10",rules=legacy)
        assertEquals("15",j.hourlyRules().overtimeRate);assertEquals("20",j.hourlyRules().sundayRate)
        money("200",s.copy(rules=j.hourlyRules()).earnings())
    }
    @Test fun pricesHolidaysAndLegacySnapshotsSurviveBackupRoundTrip() {
        val j=Job(id="j",name="Работа",rate="10",rules=prices)
        val d=AppData(listOf(j),listOf(shift()),Preferences(gapHours=1.5)).withHoliday("2026-10-02")
        assertEquals(d,LocalStore.decode(LocalStore.encode(d)))
        money("250",LocalStore.decode(LocalStore.encode(d)).shifts.single().earnings())
    }
    @Test fun invalidImportedPriceIsRejected() {
        val d=AppData(listOf(Job(id="j",name="Work",rules=prices.copy(holidayRate="-5"))))
        try {LocalStore.decode(LocalStore.encode(d));fail("Negative price must fail")}
        catch(_:IllegalArgumentException) {}
    }
}
