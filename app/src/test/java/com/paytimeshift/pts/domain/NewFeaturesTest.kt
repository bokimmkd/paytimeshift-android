package com.paytimeshift.pts.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.math.BigDecimal
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.platform.parseRoster

class NewFeaturesTest {
    private fun shift(start:String="07:00",end:String="15:00")=Shift(jobId="j",date="2026-10-02",start=start,end=end,rate="10",currency="EUR")
    @Test fun fractionalGapAndExactBoundary() {
        val a=shift(end="15:00");val b=shift(start="16:30",end="20:00")
        assertTrue(warnings(listOf(a,b),1.5).isEmpty())
        assertEquals(1,warnings(listOf(a,b),1.51).size)
        assertEquals(1,warnings(listOf(a,b),0.0,listOf(Job(id="j",name="Job",minGapHours=2.0))).size)
    }
    @Test fun overtimeNightSundayAndBonusStackIndependently() {
        val s=shift("22:00","06:00").copy(date="2026-10-03",rules=PayRules(6.0,25.0,20.0,50.0),bonus="4")
        assertEquals(0,s.earnings().compareTo(BigDecimal("135")))
        val paused=s.copy(breakMinutes=30,bonus="0")
        assertEquals(0,paused.earnings().compareTo(BigDecimal("121.875")))
    }
    @Test fun paidBreakAndDaysOff() {
        val s=shift().copy(breakMinutes=30,paidBreak=true)
        assertEquals(480L,s.paidMinutes);assertEquals(0,s.earnings().compareTo(BigDecimal("80")))
        val off=s.copy(kind="Vacation",bonus="100")
        assertEquals(0L,off.paidMinutes);assertEquals(0,off.earnings().compareTo(BigDecimal.ZERO))
        assertTrue(warnings(listOf(off,shift()),8.0).isEmpty())
    }
    @Test fun fixedPayOnlyAcceptsExplicitBonus() {
        val s=shift().copy(fixedPay=true,rate="55",bonus="5",rules=PayRules(1.0,100.0,100.0,100.0))
        assertEquals(0,s.earnings().compareTo(BigDecimal("60")))
    }
    @Test fun patternSkipsExistingAndUsesSeparateJobSettings() {
        val j=Job(id="j",name="Job",rate="12",defaultBreakMinutes=30,paidBreak=true,rules=PayRules(nightPercent=20.0))
        val rows=generatePattern(j,LocalDate.of(2026,10,1),LocalDate.of(2026,10,7),setOf(1,2,3,4,5),"07:00","15:00",30,listOf(shift()))
        assertEquals(4,rows.size);assertTrue(rows.none {it.date=="2026-10-02"})
        assertTrue(rows.all {it.paidBreak && it.rate=="12" && it.rules.nightPercent==20.0})
    }
    @Test fun periodsAndLeapMonth() {
        val j=Job(name="Job")
        assertEquals(LocalDate.of(2028,2,1) to LocalDate.of(2028,2,29),payPeriod(j,LocalDate.of(2028,3,15)))
        assertEquals(LocalDate.of(2026,9,30) to LocalDate.of(2026,10,6),payPeriod(j.copy(payCycle="Weekly"),LocalDate.of(2026,10,7)))
    }
    @Test fun rosterStrictDatesReviewAndDuplicateSafety() {
        val j=Job(id="j",name="Работа",defaultBreakMinutes=15)
        val rows=parseRoster("2026-10-02 07:00-15:00\n03.10.2026 18:00–23:00\n2026-02-30 07:00-15:00\n2026-10-05 25:00-29:00\n2026-10-02 07:00-15:00",j,emptyList())
        assertEquals(2,rows.size)
        assertEquals(15,rows[0].breakMinutes)
        assertEquals(1,parseRoster("2026-10-02 07:00-15:00\n03.10.2026 18:00–23:00",j,listOf(rows.first())).size)
    }
    @Test fun legacyDataLoadsWithoutLosingIdsRatesOrGap() {
        val legacy="""{"schemaVersion":1,"jobs":[{"id":"j","name":"Factory","color":4280584447,"currency":"EUR","rate":"6","fixedPay":false,"payCycle":"Monthly","paydayAnchor":"2026-10-15"}],"shifts":[{"id":"s","jobId":"j","date":"2026-10-02","start":"07:00","end":"15:00","breakMinutes":0,"note":"old","rate":"6","currency":"EUR","fixedPay":false}],"preferences":{"currency":"EUR","time24":true,"mondayFirst":true,"appearance":"Light","gapHours":8}}"""
        val d=LocalStore.decode(legacy)
        assertEquals("s",d.shifts.single().id);assertEquals("6",d.shifts.single().rate)
        assertEquals(8.0,d.preferences.gapHours,0.0);assertEquals("mk",d.preferences.language)
        assertEquals(d,LocalStore.decode(LocalStore.encode(d)))
    }
    @Test fun newFieldsRoundTripAndRemainSnapshots() {
        val j=Job(id="j",name="Такси",rate="10",defaultStart="18:00",defaultEnd="23:00",defaultBreakMinutes=15,paidBreak=true,reminderMinutes=30,minGapHours=1.5,rules=PayRules(nightPercent=20.0))
        val s=shift().copy(rules=j.rules,bonus="7.5",paidBreak=true)
        val d=AppData(listOf(j.copy(rate="20",rules=PayRules())),listOf(s),Preferences(gapHours=1.5))
        assertEquals(d,LocalStore.decode(LocalStore.encode(d)))
        assertEquals(20.0,d.shifts.single().rules.nightPercent,0.0)
    }
}
