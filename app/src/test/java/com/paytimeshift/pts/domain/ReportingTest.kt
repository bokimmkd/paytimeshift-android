package com.paytimeshift.pts.domain

import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ReportingTest {
    @Test fun monthPickerKeepsNovemberInTheSelectedYear() {
        val options = monthsForYear(2026)
        assertEquals(12, options.size)
        assertEquals(YearMonth.of(2026, 1), options.first())
        assertEquals(YearMonth.of(2026, 11), options[10])
        assertEquals(YearMonth.of(2026, 12), options.last())
        assertTrue(options.all { it.year == 2026 })
    }

    @Test fun novemberThenOctoberKeepsBothJobsAndTheirPlannedPay() {
        val factory = Job(id="factory", name="Редовна работа", currency="MKD", rate="180")
        val delivery = Job(id="delivery", name="Wolt", currency="MKD", rate="500")
        val jobs = listOf(factory, delivery)
        val shifts = jobs.flatMap { job ->
            generatePattern(job, LocalDate.of(2026,10,1), LocalDate.of(2026,11,30),
                setOf(1,2,3,4,5), if(job==factory) "07:00" else "18:00",
                if(job==factory) "15:00" else "21:00", 0, emptyList())
        }
        val saved = LocalStore.decode(LocalStore.encode(AppData(jobs, shifts)))
        val october = YearMonth.of(2026,10)
        val november = YearMonth.of(2026,11)
        for(month in listOf(october, november, october)) {
            val rows = shiftsForMonth(saved.shifts, month)
            val weekdays = if(month==october) 22 else 21
            assertEquals(weekdays*2, rows.size)
            assertEquals(weekdays*11*60L, rows.sumOf {it.paidMinutes})
            assertEquals(0, totals(rows).getValue("MKD").compareTo(BigDecimal(weekdays*2940)))
            assertEquals(setOf(factory.id,delivery.id), rows.map {it.jobId}.toSet())
        }
        assertTrue(shiftsForMonth(saved.shifts, YearMonth.of(2025,11)).isEmpty())
        assertEquals(shifts, saved.shifts)
    }

    @Test fun yearBoundaryAndSameMonthDifferentYearsStaySeparate() {
        fun shift(date:String, rate:String) = Shift(jobId="j", date=date, start="23:00", end="07:00", rate=rate, currency="EUR")
        val shifts = listOf(shift("2025-12-31","10"),shift("2026-01-01","20"),shift("2026-12-31","30"))
        val december2025 = shiftsForMonth(shifts, YearMonth.of(2025,12))
        val january2026 = shiftsForMonth(shifts, YearMonth.of(2026,1))
        val december2026 = shiftsForMonth(shifts, YearMonth.of(2026,12))
        assertEquals(listOf("2025-12-31"),december2025.map {it.date})
        assertEquals(0,totals(december2025).getValue("EUR").compareTo(BigDecimal("80")))
        assertEquals(0,totals(january2026).getValue("EUR").compareTo(BigDecimal("160")))
        assertEquals(0,totals(december2026).getValue("EUR").compareTo(BigDecimal("240")))
    }
}
