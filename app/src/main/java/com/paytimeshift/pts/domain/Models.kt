package com.paytimeshift.pts.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.Currency
import java.util.Locale
import java.util.UUID

data class Job(
    val id: String = UUID.randomUUID().toString(), val name: String,
    val color: Long = 0xFF2488FF, val currency: String = "EUR",
    val rate: String = "6", val fixedPay: Boolean = false,
    val payCycle: String = "Monthly", val paydayAnchor: String = LocalDate.now().withDayOfMonth(15).toString(),
    val rules: PayRules = PayRules(useHourlyRates = true), val archived: Boolean = false,
    val defaultStart: String = "07:00", val defaultEnd: String = "15:00", val defaultBreakMinutes: Int = 0,
    val paidBreak: Boolean = false, val reminderMinutes: Int = -1, val minGapHours: Double = -1.0,
    val costs: List<JobCost> = emptyList(), val extraShifts: List<ShiftTemplate> = emptyList()
)
data class Shift(
    val id: String = UUID.randomUUID().toString(), val jobId: String,
    val date: String, val start: String, val end: String, val breakMinutes: Int = 0,
    val note: String = "", val rate: String, val currency: String, val fixedPay: Boolean = false,
    val rules: PayRules = PayRules(), val bonus: String = "0", val kind: String = "Work", val paidBreak: Boolean = false
) {
    val begins: LocalDateTime get() = LocalDate.parse(date).atTime(LocalTime.parse(start))
    val finishes: LocalDateTime get() {
        val value = LocalDate.parse(date).atTime(LocalTime.parse(end))
        return if (value <= begins) value.plusDays(1) else value
    }
    val paidMinutes: Long get() = if(kind != "Work") 0 else (Duration.between(begins, finishes).toMinutes() - (if(paidBreak) 0 else breakMinutes)).coerceAtLeast(0)
    fun baseEarnings(): BigDecimal = when {
        kind != "Work" -> BigDecimal.ZERO
        fixedPay -> BigDecimal(rate)
        rules.useHourlyRates -> hourlyLines().fold(BigDecimal.ZERO) { a, line -> a + line.amount }
        else -> BigDecimal(rate).multiply(BigDecimal(paidMinutes)).divide(BigDecimal(60), 8, RoundingMode.HALF_UP)
    }
    fun earnings(): BigDecimal = baseEarnings() + additions().values.fold(BigDecimal.ZERO, BigDecimal::add)
}
data class Preferences(val currency: String = "EUR", val time24: Boolean = true,
    val mondayFirst: Boolean = true, val appearance: String = "Light", val gapHours: Double = 8.0, val language: String = "en", val reminderMinutes: Int = 0)
data class AppData(val jobs: List<Job> = emptyList(), val shifts: List<Shift> = emptyList(), val preferences: Preferences = Preferences(),
    val holidays: List<String> = emptyList())

/** Holiday dates are user-selected, and update only holiday classification, never saved rates. */
fun AppData.withHoliday(date: String): AppData {
    LocalDate.parse(date)
    val next = (if (date in holidays) holidays - date else holidays + date).distinct().sorted()
    return copy(holidays = next, jobs = jobs.map { it.copy(rules = it.rules.copy(holidayDates = next)) },
        shifts = shifts.map { it.copy(rules = it.rules.copy(holidayDates = next)) })
}

fun AppData.withJob(job: Job, applyUpcoming: Boolean, today: LocalDate): AppData {
    val saved = job.copy(rules = job.rules.copy(holidayDates = holidays))
    val updatedJobs = if (jobs.any { it.id == job.id }) jobs.map { if (it.id == job.id) saved else it } else jobs + saved
    return copy(jobs = updatedJobs, shifts = shifts.map { shift ->
        if (applyUpcoming && shift.jobId == job.id && shift.begins.toLocalDate() >= today)
            shift.copy(rate = saved.rate, currency = saved.currency, fixedPay = saved.fixedPay, rules = saved.hourlyRules())
        else shift
    })
}

fun currencyCatalog(): List<Currency> = Currency.getAvailableCurrencies()
    .filter { it.defaultFractionDigits >= 0 && it.currencyCode !in setOf("XXX", "XTS") }
    .sortedBy { it.currencyCode }
fun money(value: BigDecimal, code: String): String {
    val currency = Currency.getInstance(code)
    return "$code ${value.setScale(currency.defaultFractionDigits.coerceAtLeast(0), RoundingMode.HALF_UP).toPlainString()}"
}
fun hours(minutes: Long): String = BigDecimal(minutes).divide(BigDecimal(60), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "h"
fun totals(shifts: List<Shift>): Map<String, BigDecimal> = shifts.groupBy { it.currency }.mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { a, s -> a + s.earnings() } }
/** Half-open work intervals: touching shifts are allowed, edited rows never conflict with themselves. */
fun overlappingShifts(candidate: Shift, shifts: List<Shift>): List<Shift> {
    if (candidate.kind != "Work") return emptyList()
    return shifts.filter { existing ->
        existing.id != candidate.id && existing.kind == "Work" &&
            candidate.begins < existing.finishes && existing.begins < candidate.finishes
    }.sortedBy { it.begins }
}
fun warnings(shifts: List<Shift>, gapHours: Int): List<String> = warnings(shifts, gapHours.toDouble())
fun warnings(shifts: List<Shift>, gapHours: Double, jobs: List<Job> = emptyList()): List<String> {
    val result = linkedSetOf<String>()
    val sorted = shifts.filter { it.kind == "Work" }.sortedBy { it.begins }
    sorted.forEachIndexed { i, first ->
        sorted.drop(i + 1).forEach { next ->
            val gap = Duration.between(first.finishes, next.begins).toMinutes()
            if (gap < 0) result.add("Overlapping shifts: ${first.date} ${first.start} and ${next.date} ${next.start}")
            else if (gap < (jobs.find {it.id==next.jobId}?.minGapHours?.takeIf {it>=0} ?: gapHours) * 60) result.add("Only ${hours(gap)} between shifts: ${first.date} ${first.start} and ${next.date} ${next.start}")
        }
    }
    return result.toList()
}
fun nextPayday(job: Job, from: LocalDate): LocalDate {
    val anchor = LocalDate.parse(job.paydayAnchor)
    return when (job.payCycle) {
        "Weekly", "Biweekly" -> {
            val step = if (job.payCycle == "Weekly") 7L else 14L
            val difference = java.time.temporal.ChronoUnit.DAYS.between(anchor, from)
            anchor.plusDays(if (difference <= 0) 0 else ((difference + step - 1) / step) * step)
        }
        "Monthly" -> {
            val month = YearMonth.from(from)
            val candidate = month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            if (candidate >= from) candidate else month.plusMonths(1).let { it.atDay(anchor.dayOfMonth.coerceAtMost(it.lengthOfMonth())) }
        }
        else -> anchor
    }
}
fun sampleData(): AppData {
    val today = LocalDate.now()
    val factory = Job(id="factory", name="Фабрика", rate="6")
    val taxi = Job(id="taxi", name="Такси", color=0xFFFFA000, rate="8", payCycle="Weekly", paydayAnchor=today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.WEDNESDAY)).toString())
    val restaurant = Job(id="restaurant", name="Ресторан", color=0xFFA040C5, rate="55", fixedPay=true, payCycle="Custom")
    val shifts = mutableListOf<Shift>()
    for (d in 1..YearMonth.from(today).lengthOfMonth()) {
        val date = today.withDayOfMonth(d)
        if (date.dayOfWeek.value <= 5 || date == today) shifts.add(Shift(jobId=factory.id, date=date.toString(), start="07:00", end="15:00", rate=factory.rate, currency="EUR",rules=factory.hourlyRules()))
        if (d % 3 == 1 || date == today) shifts.add(Shift(jobId=taxi.id, date=date.toString(), start="18:00", end="23:00", rate=taxi.rate, currency="EUR",rules=taxi.hourlyRules()))
    }
    return AppData(listOf(factory, taxi, restaurant), shifts)
}

/** Legacy percentage rules remain readable so old shifts keep their original earnings. */
data class PayRules(val overtimeAfterHours: Double = 8.0, val overtimePercent: Double = 0.0,
    val nightPercent: Double = 0.0, val sundayPercent: Double = 0.0,
    val nightStart: String = "22:00", val nightEnd: String = "06:00",
    val useHourlyRates: Boolean = false, val overtimeRate: String = "", val saturdayRate: String = "",
    val sundayRate: String = "", val holidayRate: String = "", val holidayDates: List<String> = emptyList())

/** Future shifts use concrete hourly prices; existing shifts are not converted. */
fun Job.hourlyRules(): PayRules {
    if (rules.useHourlyRates) return rules
    fun converted(percent: Double) = if (percent == 0.0) "" else BigDecimal(rate)
        .multiply(BigDecimal.ONE + BigDecimal.valueOf(percent).divide(BigDecimal(100)))
        .stripTrailingZeros().toPlainString()
    return rules.copy(useHourlyRates = true, overtimeRate = converted(rules.overtimePercent),
        sundayRate = converted(rules.sundayPercent), overtimePercent = 0.0, sundayPercent = 0.0)
}

data class PayLine(val label: String, val minutes: Double, val rate: String, val currency: String) {
    val amount: BigDecimal get() = BigDecimal(rate).multiply(BigDecimal.valueOf(minutes))
        .divide(BigDecimal(60), 8, RoundingMode.HALF_UP)
}

/** Calendar dates split at midnight. An untimed break is allocated proportionally.
 * A holiday replaces the weekend rate; overtime uses the higher of day and overtime price.
 */
fun Shift.hourlyLines(): List<PayLine> {
    if (kind != "Work" || fixedPay || !rules.useHourlyRates) return emptyList()
    val total = Duration.between(begins, finishes).toMinutes()
    if (total <= 0 || paidMinutes <= 0) return emptyList()
    val proportion = paidMinutes.toDouble() / total
    val threshold = rules.overtimeAfterHours * 60
    val grouped = linkedMapOf<Pair<String, String>, Double>()
    fun price(value: String) = (value.ifBlank { rate }).toBigDecimal()
    fun add(label: String, minutes: Double, value: BigDecimal) {
        if (minutes <= 1e-9) return
        val key = label to value.stripTrailingZeros().toPlainString()
        grouped[key] = (grouped[key] ?: 0.0) + minutes
    }
    var t = begins
    var paidSoFar = 0.0
    while (t < finishes) {
        val until = minOf(t.toLocalDate().plusDays(1).atStartOfDay(), finishes)
        val segmentMinutes = Duration.between(t, until).toMinutes() * proportion
        val (label, value) = when {
            t.toLocalDate().toString() in rules.holidayDates -> "Holiday work" to price(rules.holidayRate)
            t.dayOfWeek.value == 7 -> "Sunday work" to price(rules.sundayRate)
            t.dayOfWeek.value == 6 -> "Saturday work" to price(rules.saturdayRate)
            else -> "Regular hours" to BigDecimal(rate)
        }
        val regular = (threshold - paidSoFar).coerceIn(0.0, segmentMinutes)
        add(label, regular, value)
        add("Overtime", segmentMinutes - regular, if (rules.overtimeRate.isBlank()) value else value.max(price(rules.overtimeRate)))
        paidSoFar += segmentMinutes
        t = until
    }
    return grouped.map { (key, minutes) -> PayLine(key.first, minutes, key.second, currency) }
}

fun decimalHours(minutes: Double): String = BigDecimal.valueOf(minutes).divide(BigDecimal(60), 2, RoundingMode.HALF_UP)
    .stripTrailingZeros().toPlainString() + "h"

fun Shift.additions(): Map<String, BigDecimal> {
    if (kind != "Work") return emptyMap()
    val result = linkedMapOf<String,BigDecimal>()
    val total = Duration.between(begins, finishes).toMinutes()
    if (!fixedPay && total > 0) {
        fun amount(minutes: Double, percent: Double) = BigDecimal(rate).multiply(BigDecimal.valueOf(minutes))
            .multiply(BigDecimal.valueOf(percent)).divide(BigDecimal(6000), 8, RoundingMode.HALF_UP)
        val overtime = (paidMinutes - rules.overtimeAfterHours*60).coerceAtLeast(0.0)
        if (!rules.useHourlyRates && rules.overtimePercent > 0 && overtime > 0) result["Overtime"] = amount(overtime,rules.overtimePercent)
        var night=0L; var sunday=0L; var t=begins
        val ns=LocalTime.parse(rules.nightStart); val ne=LocalTime.parse(rules.nightEnd)
        while(t < finishes && (rules.nightPercent>0 || (!rules.useHourlyRates && rules.sundayPercent>0))) {
            val time=t.toLocalTime()
            if (if(ns>ne) time>=ns || time<ne else time>=ns && time<ne) night++
            if(t.dayOfWeek.value==7) sunday++
            t=t.plusMinutes(1)
        }
        val proportion=paidMinutes.toDouble()/total
        if(rules.nightPercent>0 && night>0) result["Night work"] = amount(night*proportion,rules.nightPercent)
        if(!rules.useHourlyRates && rules.sundayPercent>0 && sunday>0) result["Sunday work"] = amount(sunday*proportion,rules.sundayPercent)
    }
    if(BigDecimal(bonus).signum()!=0) result["Bonus"]=BigDecimal(bonus)
    return result
}
fun payPeriod(job: Job, payday: LocalDate): Pair<LocalDate,LocalDate> = when(job.payCycle) {
    "Monthly" -> YearMonth.from(payday).minusMonths(1).let {it.atDay(1) to it.atEndOfMonth()}
    "Weekly" -> payday.minusDays(7) to payday.minusDays(1)
    "Biweekly" -> payday.minusDays(14) to payday.minusDays(1)
    else -> payday.withDayOfMonth(1) to payday
}
fun generatePattern(job: Job, from: LocalDate, until: LocalDate, weekdays: Set<Int>,
    start: String, end: String, breakMinutes: Int, existing: List<Shift>): List<Shift> {
    require(until>=from && java.time.temporal.ChronoUnit.DAYS.between(from,until)<=366)
    return generateSequence(from) {it.plusDays(1)}.takeWhile {it<=until}
        .filter {it.dayOfWeek.value in weekdays}.map {d->Shift(jobId=job.id,date=d.toString(),start=start,end=end,
            breakMinutes=breakMinutes,rate=job.rate,currency=job.currency,fixedPay=job.fixedPay,rules=job.hourlyRules(),paidBreak=job.paidBreak)}
        .filter {s->existing.none {it.jobId==s.jobId && it.date==s.date && it.start==s.start && it.end==s.end}}.toList()
}
