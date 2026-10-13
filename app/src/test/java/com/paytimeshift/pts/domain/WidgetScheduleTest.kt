package com.paytimeshift.pts.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WidgetScheduleTest {
    private val today = LocalDate.of(2026, 12, 31)
    private fun shift(id: String, date: LocalDate, start: String = "07:00", end: String = "15:00", job: String = "a", kind: String = "Work") =
        Shift(id=id, jobId=job, date=date.toString(), start=start, end=end, rate="10", currency="EUR", kind=kind)
    @Test fun bothDaysAreSortedAndArchivedJobsAreExcludedAcrossTheYearBoundary() {
        val data=AppData(jobs=listOf(Job(id="a",name="Job A"),Job(id="old",name="Old",archived=true)),
            shifts=listOf(shift("late",today,"18:00","21:00"),shift("next",today.plusDays(1)),
                shift("early",today),shift("old",today,job="old"),shift("past",today.minusDays(1))))
        val days=widgetDays(data,today)
        assertEquals(listOf(today,today.plusDays(1)),days.map {it.date})
        assertEquals(listOf("early","late"),days[0].shifts.map {it.id})
        assertEquals(listOf("next"),days[1].shifts.map {it.id})
    }
    @Test fun overnightWorkStaysOnItsStartDateAndNonWorkingEntriesAreRetained() {
        val night=shift("night",today,"22:00","06:00")
        val leave=shift("leave",today.plusDays(1),kind="Non-working day")
        val days=widgetDays(AppData(jobs=listOf(Job(id="a",name="A")),shifts=listOf(night,leave)),today)
        assertEquals(listOf(night),days[0].shifts)
        assertEquals(listOf(leave),days[1].shifts)
        assertEquals(today.plusDays(1),night.finishes.toLocalDate())
    }
    @Test fun editsDeletesAndMidnightRolloverUseTheLatestData() {
        val next=shift("next",today.plusDays(1))
        val data=AppData(jobs=listOf(Job(id="a",name="A")),shifts=listOf(next))
        assertTrue(widgetDays(data,today)[0].shifts.isEmpty())
        assertEquals(listOf(next),widgetDays(data,today.plusDays(1))[0].shifts)
        assertEquals("17:00",widgetDays(data.copy(shifts=listOf(next.copy(end="17:00"))),today)[1].shifts.single().end)
        assertTrue(widgetDays(data.copy(shifts=emptyList()),today).all {it.shifts.isEmpty()})
    }
}
