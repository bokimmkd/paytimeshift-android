package com.paytimeshift.pts

import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.premium.AccountStatus
import com.paytimeshift.pts.premium.cloudBackupLabel
import org.junit.Assert.*
import org.junit.Test

class CalendarStatusTest {
    private fun shift(id:String,date:String="2026-10-07",start:String="07:00",end:String="15:00",kind:String="Work") =
        Shift(id=id,jobId=id,date=date,start=start,end=end,rate="10",currency="EUR",kind=kind,bonus="20")

    @Test fun absenceMarkersRetainTheirJobAndNeverCreateHoursOrDailyPay() {
        for(kind in listOf("Off","Vacation","Sick","Non-working day")) {
            val absence=shift("factory",kind=kind)
            val working=shift("wolt",start="18:00",end="21:00")
            assertEquals(listOf(CalendarMarker("factory",kind),CalendarMarker("wolt","Work")),calendarMarkers(listOf(absence,working)))
            assertEquals(0L,absence.paidMinutes)
            assertEquals(0,absence.earnings().signum())
            assertEquals(0,absence.copy(fixedPay=true).earnings().signum())
            assertEquals(180L,working.paidMinutes)
        }
    }
    @Test fun shortRestDatesCrossTheMonthBoundaryAndIgnoreAbsence() {
        val night=shift("factory",date="2026-09-30",start="23:00",end="07:00")
        val next=shift("wolt",date="2026-10-01",start="08:00",end="12:00")
        val rows=listOf(night,next,shift("off",date="2026-10-01",kind="Vacation"))
        val alerts=scheduleWarnings(rows,2.0)
        assertEquals(1,alerts.size)
        assertEquals(setOf("2026-09-30","2026-10-01"),alerts.single().dates)
        assertEquals(60L,alerts.single().gapMinutes)
        assertTrue(alerts.single().shortRest)
        assertTrue(scheduleWarnings(rows,1.0).isEmpty())
    }
    @Test fun jobThresholdAndOverlapRemainDistinct() {
        val a=shift("factory")
        val b=shift("wolt",start="16:00",end="20:00")
        assertEquals(1,scheduleWarnings(listOf(a,b),0.0,listOf(Job(id="wolt",name="Wolt",minGapHours=2.0))).size)
        val overlap=scheduleWarnings(listOf(a,b.copy(start="14:00")),8.0).single()
        assertFalse(overlap.shortRest)
        assertEquals(warnings(listOf(a,b.copy(start="14:00")),8.0).single(),overlap.message)
    }
    @Test fun cloudBindingNeverOverridesUnresolvedOrFreeEntitlement() {
        val premium=AccountStatus(uid="owner",premium=true,backupAt=123)
        assertEquals("Checking account…",cloudBackupLabel(premium,true,false,false))
        assertEquals("Cloud backup status unavailable. Refresh account.",cloudBackupLabel(premium,true,false,true))
        assertEquals("Cloud backup paused. Premium is required.",cloudBackupLabel(premium.copy(premium=false),true,true,false))
        assertEquals("Automatic backup is ready. No cloud backup saved yet.",cloudBackupLabel(premium.copy(backupAt=0),true,true,false))
        assertEquals("Automatic backup enabled on this phone",cloudBackupLabel(premium,true,true,false))
    }
}
