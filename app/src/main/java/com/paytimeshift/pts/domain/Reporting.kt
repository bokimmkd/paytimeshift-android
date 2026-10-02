package com.paytimeshift.pts.domain

import java.time.LocalDate
import java.time.YearMonth

/** Both calendar and earnings use the full selected year and month. */
fun shiftsForMonth(shifts: List<Shift>, month: YearMonth): List<Shift> =
    shifts.filter { YearMonth.from(LocalDate.parse(it.date)) == month }

/** The picker starts in the selected year, with exactly one entry per month. */
fun monthsForYear(year: Int): List<YearMonth> {
    require(year in 1..9999)
    return (1..12).map { YearMonth.of(year, it) }
}
