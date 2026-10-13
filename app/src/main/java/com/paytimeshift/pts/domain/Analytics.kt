package com.paytimeshift.pts.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.UUID

/** Currency is always inherited from the job. No exchange-rate conversion is performed. */
data class JobCost(val id: String = UUID.randomUUID().toString(), val category: String = "Other",
    val name: String = "", val amount: String = "0", val frequency: String = "Per shift", val enabled: Boolean = true)
val costCategories = listOf("Transport / Commute", "Fuel", "Parking & Tolls", "Food / Meals at work",
    "Work Clothing & Equipment", "Phone / Mobile Data", "Other")
val costFrequencies = listOf("Per shift", "Per workday", "Weekly", "Monthly")

/** Time classifications overlap: night/weekend/holiday hours are not extra hours. */
data class WorkTime(val shifts: Int = 0, val worked: Double = 0.0, val paid: Double = 0.0,
    val regular: Double = 0.0, val overtime: Double = 0.0, val night: Double = 0.0,
    val weekend: Double = 0.0, val holiday: Double = 0.0) {
    operator fun plus(b: WorkTime) = WorkTime(shifts+b.shifts,worked+b.worked,paid+b.paid,
        regular+b.regular,overtime+b.overtime,night+b.night,weekend+b.weekend,holiday+b.holiday)
}
fun Shift.workTime(): WorkTime {
    if (kind != "Work") return WorkTime()
    val total = Duration.between(begins, finishes).toMinutes().toDouble()
    val paid = paidMinutes.toDouble()
    val worked = (total-breakMinutes).coerceAtLeast(0.0)
    val overtime = (paid-rules.overtimeAfterHours*60).coerceAtLeast(0.0)
    val ns=LocalTime.parse(rules.nightStart); val ne=LocalTime.parse(rules.nightEnd)
    var night=0.0; var weekend=0.0; var holiday=0.0; var time=begins
    while(time<finishes) {
        val t=time.toLocalTime()
        if(if(ns>ne) t>=ns || t<ne else t>=ns && t<ne) night++
        if(time.dayOfWeek.value>=6) weekend++
        if(time.toLocalDate().toString() in rules.holidayDates) holiday++
        time=time.plusMinutes(1)
    }
    val proportion=if(total==0.0) 0.0 else paid/total
    return WorkTime(1,worked,paid,paid-overtime,overtime,night*proportion,weekend*proportion,holiday*proportion)
}
data class CostDetail(val cost: JobCost,val count: Int,val total: BigDecimal)
data class CostTotal(val category: String, val amount: BigDecimal)
data class JobAnalysis(val jobId: String, val name: String, val currency: String,
    val gross: BigDecimal, val costs: BigDecimal, val time: WorkTime, val categories: List<CostTotal>, val details: List<CostDetail> = emptyList(),
    val salaryBase: BigDecimal = BigDecimal.ZERO, val adjustments: List<MonthlyAdjustment> = emptyList()) {
    val monthlyBonuses: BigDecimal get()=adjustments.filter {it.type=="Bonus"}.fold(BigDecimal.ZERO){a,i->a+i.amount.toBigDecimal()}
    val monthlyDeductions: BigDecimal get()=adjustments.filter {it.type=="Deduction"}.fold(BigDecimal.ZERO){a,i->a+i.amount.toBigDecimal()}
    val adjusted: BigDecimal get()=gross+monthlyBonuses-monthlyDeductions
    val real: BigDecimal get()=adjusted-costs
    val grossHourly: BigDecimal? get()=hourlyValue(gross,time.worked)
    val realHourly: BigDecimal? get()=hourlyValue(real,time.worked)
}
data class CurrencyAnalysis(val currency: String, val jobs: List<JobAnalysis>) {
    val salaryBase: BigDecimal get()=jobs.fold(BigDecimal.ZERO){a,j->a+j.salaryBase}
    val monthlyBonuses: BigDecimal get()=jobs.fold(BigDecimal.ZERO){a,j->a+j.monthlyBonuses}
    val monthlyDeductions: BigDecimal get()=jobs.fold(BigDecimal.ZERO){a,j->a+j.monthlyDeductions}
    val adjusted: BigDecimal get()=gross+monthlyBonuses-monthlyDeductions
    val gross: BigDecimal get()=jobs.fold(BigDecimal.ZERO){a,j->a+j.gross}
    val costs: BigDecimal get()=jobs.fold(BigDecimal.ZERO){a,j->a+j.costs}
    val real: BigDecimal get()=adjusted-costs
    val time: WorkTime get()=jobs.fold(WorkTime()){a,j->a+j.time}
    val categories: List<CostTotal> get()=jobs.flatMap {it.categories}.groupBy {it.category}
        .map {(category,rows)->CostTotal(category,rows.fold(BigDecimal.ZERO){a,r->a+r.amount})}.sortedByDescending {it.amount}
    val grossHourly: BigDecimal? get()=hourlyValue(gross,time.worked)
    val realHourly: BigDecimal? get()=hourlyValue(real,time.worked)
}
data class WorkReport(val from: LocalDate, val until: LocalDate, val currencies: List<CurrencyAnalysis>)
fun hourlyValue(amount: BigDecimal, minutes: Double): BigDecimal? = if(minutes<=0.0) null else
    amount.multiply(BigDecimal(60)).divide(BigDecimal.valueOf(minutes),8,RoundingMode.HALF_UP)
fun costPercentage(amount: BigDecimal,total: BigDecimal): BigDecimal = if(total.signum()==0) BigDecimal.ZERO else
    amount.multiply(BigDecimal(100)).divide(total,1,RoundingMode.HALF_UP)
fun percentChange(now: BigDecimal, previous: BigDecimal): BigDecimal? = if(previous.signum()==0) null else
    (now-previous).multiply(BigDecimal(100)).divide(previous.abs(),1,RoundingMode.HALF_UP)

/** Workdays use shift start dates, including overnight shifts. Weekly costs are charged once
 * per ISO week with work, to its earliest workday (even across month/year boundaries).
 * Monthly costs are charged once per month with work, to its earliest workday.
 * Current job cost rules estimate historical costs; gross shift prices stay untouched. */
fun analytics(data: AppData, from: LocalDate, until: LocalDate): WorkReport {
    require(until>=from)
    val jobs=data.jobs.flatMap {job->
        val salary=salaryTotals(job,from,until)
        val adjustments=data.adjustments.filter {it.jobId==job.id && it.inPeriod(from,until)}
        val all=data.shifts.filter {it.jobId==job.id && it.kind=="Work"}.sortedBy {it.begins}
        val rows=all.filter {it.begins.toLocalDate() in from..until}
        val dates=all.map {it.begins.toLocalDate()}.distinct()
        val weekDates=dates.groupBy {it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))}.values.map {it.min()}
        val monthDates=dates.groupBy {YearMonth.from(it)}.values.map {it.min()}
        val details=job.costs.filter {it.enabled}.map {c->
            val count=when(c.frequency) {
                "Per shift"->rows.size
                "Per workday"->dates.count {it in from..until}
                "Weekly"->weekDates.count {it in from..until}
                "Monthly"->monthDates.count {it in from..until}
                else->error("Unsupported cost frequency")
            }
            CostDetail(c,count,c.amount.toBigDecimal().multiply(BigDecimal(count)))
        }.filter {it.count>0}
        val categoryAmounts=details.groupBy {it.cost.category}.map {(category,items)->CostTotal(category,items.fold(BigDecimal.ZERO){a,d->a+d.total})}.filter {it.amount.signum()!=0}
        val currencies=(rows.map {it.currency}+salary.keys+adjustments.map {it.currency}+if(categoryAmounts.isNotEmpty()) listOf(job.currency) else emptyList()).distinct()
        currencies.map {currency->
            val currencyRows=rows.filter {it.currency==currency}
            val categories=if(currency==job.currency) categoryAmounts else emptyList()
            JobAnalysis(job.id,job.name,currency,(salary[currency] ?: BigDecimal.ZERO)+currencyRows.fold(BigDecimal.ZERO){a,s->a+s.earnings()},
                categories.fold(BigDecimal.ZERO){a,c->a+c.amount},currencyRows.fold(WorkTime()){a,s->a+s.workTime()},categories,if(currency==job.currency) details else emptyList(), salary[currency] ?: BigDecimal.ZERO, adjustments.filter {it.currency==currency})
        }
    }
    return WorkReport(from,until,jobs.groupBy {it.currency}.toSortedMap().map {(currency,rows)->CurrencyAnalysis(currency,rows)})
}
fun monthlyAnalytics(data: AppData, month: YearMonth)=analytics(data,month.atDay(1),month.atEndOfMonth())
fun yearlyAnalytics(data: AppData,year: Int)=analytics(data,LocalDate.of(year,1,1),LocalDate.of(year,12,31))
fun yearlyTrend(data: AppData,year: Int)=(1..12).map {monthlyAnalytics(data,YearMonth.of(year,it))}

