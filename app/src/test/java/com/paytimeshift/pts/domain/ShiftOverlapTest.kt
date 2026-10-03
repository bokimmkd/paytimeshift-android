package com.paytimeshift.pts.domain

import org.junit.Assert.*
import org.junit.Test

class ShiftOverlapTest {
    private fun shift(id:String,job:String,date:String="2026-10-03",start:String="07:00",end:String="15:00",kind:String="Work") =
        Shift(id=id,jobId=job,date=date,start=start,end=end,breakMinutes=0,rate="10",currency="EUR",kind=kind)

    @Test fun findsAllOverlapsAcrossJobsIncludingContainingShifts() {
        val candidate=shift("new","second",start="10:00",end="11:00")
        val first=shift("first","first")
        val second=shift("second","third",start="10:30",end="12:00")
        assertEquals(listOf(first,second),overlappingShifts(candidate,listOf(second,first)))
    }

    @Test fun touchingEndpointsAreAllowedAndDifferentDatesDoNotConflict() {
        val candidate=shift("new","second",start="15:00",end="18:00")
        assertTrue(overlappingShifts(candidate,listOf(shift("first","first"),shift("later","third",start="18:00",end="22:00"),shift("other-date","first",date="2026-10-04"))).isEmpty())
    }

    @Test fun overnightShiftsAreCheckedOnBothDates() {
        val night=shift("night","first",start="22:00",end="06:00")
        val nextDay=shift("new","second",date="2026-10-04",start="05:00",end="09:00")
        assertEquals(listOf(night),overlappingShifts(nextDay,listOf(night)))
        assertTrue(overlappingShifts(nextDay.copy(start="06:00"),listOf(night)).isEmpty())
    }

    @Test fun excludesEditedShiftAndNonWorkingEntries() {
        val candidate=shift("editing","first")
        val off=shift("off","second",kind="Off")
        val vacation=shift("leave","second",kind="Vacation")
        val sick=shift("sick","second",kind="Sick")
        assertTrue(overlappingShifts(candidate,listOf(candidate,off,vacation,sick)).isEmpty())
        assertTrue(overlappingShifts(off,listOf(candidate)).isEmpty())
    }
}
