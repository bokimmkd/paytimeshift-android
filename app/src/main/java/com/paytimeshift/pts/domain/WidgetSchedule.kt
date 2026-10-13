package com.paytimeshift.pts.domain

import java.time.LocalDate

/** A shift belongs to its start date, including overnight work, just like Calendar. */
data class WidgetDay(val date: LocalDate, val shifts: List<Shift>)
fun widgetDays(data: AppData, today: LocalDate): List<WidgetDay> {
    val activeJobs = data.jobs.filterNot { it.archived }.map { it.id }.toSet()
    return listOf(today, today.plusDays(1)).map { date ->
        WidgetDay(date, data.shifts.filter { it.jobId in activeJobs && it.date == date.toString() }
            .sortedWith(compareBy<Shift> { it.begins }.thenBy { it.id }))
    }
}
