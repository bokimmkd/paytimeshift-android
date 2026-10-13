package com.paytimeshift.pts.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

val dayKinds = listOf("Work", "Off", "Vacation", "Sick", "Non-working day")
data class ManualShiftChange(val shift: Shift, val replacedIds: Set<String> = emptySet())
fun dayStatusReplacements(candidate: Shift, shifts: List<Shift>) = shifts.filter {
    (it.id != candidate.id || it.kind != candidate.kind) && it.jobId == candidate.jobId && it.date == candidate.date &&
        (candidate.kind != "Work" || it.kind != "Work")
}
fun AppData.withManualShiftChange(change: ManualShiftChange): AppData {
    require(change.shift.kind in dayKinds && jobs.any { it.id == change.shift.jobId })
    require(dayStatusReplacements(change.shift, shifts).map { it.id }.toSet() == change.replacedIds)
    return copy(shifts = shifts.filterNot { it.id == change.shift.id || it.id in change.replacedIds } + change.shift)
}

/** Inclusive effective dates preserve historical salaries. Full months always pay the entered amount.
 * Partial employment months and sub-month report ranges allocate by calendar days, not attendance. */
data class SalaryPeriod(val from: String, val until: String = "", val amount: String, val currency: String)
fun Job.monthlySalaryOn(date: LocalDate) = salaryPeriods.find {
    date >= LocalDate.parse(it.from) && (it.until.isBlank() || date <= LocalDate.parse(it.until))
}
fun salaryPeriodsAfterEdit(previous: List<SalaryPeriod>, from: String, amount: String?, currency: String, until: String = ""): List<SalaryPeriod> {
    val start = LocalDate.parse(from)
    require(until.isBlank() || LocalDate.parse(until) >= start)
    val history = previous.filter { LocalDate.parse(it.from) < start }.map {
        if (it.until.isBlank() || LocalDate.parse(it.until) >= start) it.copy(until = start.minusDays(1).toString()) else it
    }
    return history + if (amount == null) emptyList() else listOf(SalaryPeriod(from, until, amount, currency))
}
fun salaryTotals(job: Job, from: LocalDate, until: LocalDate): Map<String, BigDecimal> {
    val totals = linkedMapOf<String, BigDecimal>()
    job.salaryPeriods.forEach { period ->
        val first = maxOf(from, LocalDate.parse(period.from))
        val last = minOf(until, if (period.until.isBlank()) until else LocalDate.parse(period.until))
        if (last >= first) {
            var month = YearMonth.from(first)
            while (month <= YearMonth.from(last)) {
                val days = ChronoUnit.DAYS.between(maxOf(first, month.atDay(1)), minOf(last, month.atEndOfMonth())) + 1
                val value = period.amount.toBigDecimal().multiply(BigDecimal(days))
                    .divide(BigDecimal(month.lengthOfMonth()), 8, RoundingMode.HALF_UP)
                totals[period.currency] = (totals[period.currency] ?: BigDecimal.ZERO) + value
                month = month.plusMonths(1)
            }
        }
    }
    return totals
}

val deductionReasons = listOf("Absence", "Sick leave", "Unpaid leave", "Lateness", "Other penalty", "Custom reason")
data class MonthlyAdjustment(val id: String = UUID.randomUUID().toString(), val jobId: String,
    val month: String, val type: String, val amount: String, val currency: String,
    val reason: String, val note: String = "")
/** Monthly items belong to the last day of their month, so weekly/yearly ranges never duplicate them. */
fun MonthlyAdjustment.inPeriod(from: LocalDate, until: LocalDate) = YearMonth.parse(month).atEndOfMonth() in from..until

fun salaryPeriodsEnding(previous: List<SalaryPeriod>, until: LocalDate): List<SalaryPeriod> = previous
    .filter { LocalDate.parse(it.from)<=until }.map {
        if(it.until.isBlank() || LocalDate.parse(it.until)>until) it.copy(until=until.toString()) else it
    }
