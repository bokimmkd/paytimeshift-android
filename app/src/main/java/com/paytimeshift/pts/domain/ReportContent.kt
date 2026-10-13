package com.paytimeshift.pts.domain

import java.math.BigDecimal
import java.time.YearMonth

data class ReportLine(val label: String, val value: String = "", val heading: Boolean = false, val literal: Boolean = false)
fun reportLines(data: AppData, month: YearMonth, yearly: Boolean): List<ReportLine> {
    val locale=com.paytimeshift.pts.ui.localeForLanguage(data.preferences.language)
    val unit=com.paytimeshift.pts.ui.translate("h",data.preferences.language)
    fun number(value: BigDecimal,places: Int=2)=java.text.NumberFormat.getNumberInstance(locale).apply {maximumFractionDigits=places;minimumFractionDigits=0}.format(value)
    fun money(value: BigDecimal,code: String)=code+" "+number(value,java.util.Currency.getInstance(code).defaultFractionDigits.coerceAtLeast(0))
    fun decimalHours(minutes: Double)=number(BigDecimal.valueOf(minutes).divide(BigDecimal(60),2,java.math.RoundingMode.HALF_UP))+" "+unit
    val result=mutableListOf<ReportLine>()
    val report=if(yearly) yearlyAnalytics(data,month.year) else monthlyAnalytics(data,month)
    result+=ReportLine(if(yearly) "Annual Work & Earnings Report" else "Monthly Work & Earnings Report",heading=true)
    result+=ReportLine("Period","${report.from} – ${report.until}")
    result+=ReportLine("Estimated real earnings after work-related costs.")
    result+=ReportLine("Taxes and government deductions are not calculated. Only entered adjustments are included.")
    if(report.currencies.isEmpty()) result+=ReportLine("No shifts added")
    fun time(t: WorkTime) {
        result+=listOf(ReportLine("Total shifts",t.shifts.toString()),ReportLine("Worked hours",decimalHours(t.worked)),
            ReportLine("Paid hours",decimalHours(t.paid)),ReportLine("Regular hours",decimalHours(t.regular)),ReportLine("Overtime",decimalHours(t.overtime)),
            ReportLine("Night hours",decimalHours(t.night)),ReportLine("Weekend hours",decimalHours(t.weekend)),ReportLine("Holiday hours",decimalHours(t.holiday)))
    }
    report.currencies.forEach {c->
        result+=ReportLine(c.currency,heading=true,literal=true)
        time(c.time)
        result+=listOf(ReportLine("Monthly salary",money(c.salaryBase,c.currency)),ReportLine("Gross estimated earnings",money(c.gross,c.currency)),ReportLine("Monthly bonuses",money(c.monthlyBonuses,c.currency)),ReportLine("Monthly deductions",money(c.monthlyDeductions,c.currency)),ReportLine("Adjusted estimated earnings",money(c.adjusted,c.currency)))
        result+=listOf(ReportLine("Work-related costs",money(c.costs,c.currency)),
            ReportLine("Real estimated earnings",money(c.real,c.currency)),ReportLine("Gross hourly value",c.grossHourly?.let {money(it,c.currency)+" / "+unit} ?: "—"),
            ReportLine("Real hourly value",c.realHourly?.let {money(it,c.currency)+" / "+unit} ?: "—"))
        result+=ReportLine("Cost breakdown",heading=true)
        c.categories.forEach {result+=ReportLine(it.category,money(it.amount,c.currency)+" · "+number(costPercentage(it.amount,c.costs),1)+"%")}
        result+=ReportLine("Breakdown by job",heading=true)
        c.jobs.forEach {j->
            result+=ReportLine(j.name,heading=true,literal=true)
            time(j.time)
            result+=listOf(ReportLine("Monthly salary",money(j.salaryBase,c.currency)),ReportLine("Gross estimated earnings",money(j.gross,c.currency)),ReportLine("Monthly bonuses",money(j.monthlyBonuses,c.currency)),ReportLine("Monthly deductions",money(j.monthlyDeductions,c.currency)),ReportLine("Adjusted estimated earnings",money(j.adjusted,c.currency)))
            if(j.adjustments.isNotEmpty()) result+=ReportLine("Monthly adjustments",heading=true)
            j.adjustments.sortedWith(compareBy({it.month},{it.type},{it.id})).forEach {a->
                val reason=com.paytimeshift.pts.ui.translate(a.reason,data.preferences.language)
                result+=ReportLine(a.month+" · "+reason,(if(a.type=="Bonus") "+ " else "- ")+money(a.amount.toBigDecimal(),a.currency)+(if(a.note.isBlank()) "" else " · "+a.note),literal=true)
            }
            result+=listOf(ReportLine("Work-related costs",money(j.costs,c.currency)),
                ReportLine("Real estimated earnings",money(j.real,c.currency)),
                ReportLine("Gross hourly value",j.grossHourly?.let {money(it,c.currency)+" / "+unit} ?: "—"),
                ReportLine("Real hourly value",j.realHourly?.let {money(it,c.currency)+" / "+unit} ?: "—"))
            if(j.details.isNotEmpty()) result+=ReportLine("Cost items",heading=true)
            j.details.forEach {d->
                result+=ReportLine(d.cost.name.ifBlank {com.paytimeshift.pts.ui.translate(d.cost.category,data.preferences.language)},
                    money(d.cost.amount.toBigDecimal(),c.currency)+" × "+d.count+" · "+com.paytimeshift.pts.ui.translate(d.cost.frequency,data.preferences.language)+" = "+money(d.total,c.currency),literal=true)
            }
        }
        if(yearly) {
            val trend=yearlyTrend(data,month.year).map {it.from to it.currencies.find {r->r.currency==c.currency}}
            result+=ReportLine("Average monthly real earnings",money(c.real.divide(BigDecimal(12),8,java.math.RoundingMode.HALF_UP),c.currency))
            result+=ReportLine("Monthly trend",heading=true)
            trend.forEach {(date,entry)->
                result+=ReportLine(YearMonth.from(date).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",locale)),decimalHours(entry?.time?.worked ?: 0.0),heading=true,literal=true)
                result+=ReportLine("Gross estimated earnings",money(entry?.gross ?: BigDecimal.ZERO,c.currency))
                result+=ReportLine("Monthly bonuses",money(entry?.monthlyBonuses ?: BigDecimal.ZERO,c.currency))
                result+=ReportLine("Monthly deductions",money(entry?.monthlyDeductions ?: BigDecimal.ZERO,c.currency))
                result+=ReportLine("Adjusted estimated earnings",money(entry?.adjusted ?: BigDecimal.ZERO,c.currency))
                result+=ReportLine("Work-related costs",money(entry?.costs ?: BigDecimal.ZERO,c.currency))
                result+=ReportLine("Real estimated earnings",money(entry?.real ?: BigDecimal.ZERO,c.currency))
            }
            val active=trend.filter {it.second!=null}
            result+=ReportLine("Highlights",heading=true)
            fun monthHighlight(label: String,selector: (CurrencyAnalysis)->BigDecimal,highest: Boolean=true) {
                val chosen=if(highest) active.maxByOrNull {selector(it.second!!)} else active.minByOrNull {selector(it.second!!)}
                chosen?.let {result+=ReportLine(label,YearMonth.from(it.first).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",locale)))}
            }
            monthHighlight("Highest earning month",{it.real});monthHighlight("Lowest earning month",{it.real},false)
            monthHighlight("Month with highest work costs",{it.costs});monthHighlight("Month with most overtime",{BigDecimal.valueOf(it.time.overtime)})
            c.jobs.maxByOrNull {it.gross}?.let {result+=ReportLine("Job with highest gross earnings",it.name)}
            c.jobs.maxByOrNull {it.real}?.let {result+=ReportLine("Job with highest real earnings",it.name)}
            c.jobs.filter {it.realHourly!=null}.maxByOrNull {it.realHourly!!}?.let {result+=ReportLine("Job with highest real hourly value",it.name)}
        } else {
            val previous=monthlyAnalytics(data,month.minusMonths(1)).currencies.find {it.currency==c.currency}
            result+=ReportLine("Compared with previous month",heading=true)
            fun diff(label: String,now: BigDecimal,old: BigDecimal?) {
                val p=old?.let {percentChange(now,it)}
                result+=ReportLine(label,if(p==null) "—" else (if(p.signum()>0) "↑ +" else if(p.signum()<0) "↓ " else "")+number(p,1)+"%")
            }
            diff("Worked hours",BigDecimal.valueOf(c.time.worked),previous?.time?.worked?.let {BigDecimal.valueOf(it)})
            diff("Gross estimated earnings",c.gross,previous?.gross);diff("Work-related costs",c.costs,previous?.costs)
            diff("Real estimated earnings",c.real,previous?.real)
            if(c.realHourly!=null) diff("Real hourly value",c.realHourly!!,previous?.realHourly)
        }
    }
    result+=ReportLine("Night, weekend and holiday hours overlap other hours.")
    result+=ReportLine("Weekly: once per workweek, on its first workday. Monthly: once per working month. Current cost rules apply to report history.")
    result+=ReportLine("Different currencies are shown separately.")
    return result
}

