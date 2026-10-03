package com.paytimeshift.pts.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

data class ShiftTemplate(val start: String, val end: String, val breakMinutes: Int = 0,
    val paidBreak: Boolean = false, val active: Boolean = false) {
    fun validate() {
        val minutes=Duration.between(LocalTime.parse(start),LocalTime.parse(end)).toMinutes().let {if(it<=0) it+1440 else it}
        require(start!=end && breakMinutes>=0 && breakMinutes<minutes)
    }
}
fun Job.shiftTemplates(): List<ShiftTemplate> = listOf(ShiftTemplate(defaultStart,defaultEnd,defaultBreakMinutes,paidBreak,true)) +
    (0..1).map {extraShifts.getOrNull(it) ?: if(it==0) ShiftTemplate("15:00","23:00") else ShiftTemplate("23:00","07:00")}
data class RotationBlock(val shift: Int?, val days: Int = 2)
fun Job.templateShift(date: LocalDate, template: ShiftTemplate) = Shift(jobId=id,date=date.toString(),start=template.start,end=template.end,
    breakMinutes=template.breakMinutes,paidBreak=template.paidBreak,rate=rate,currency=currency,fixedPay=fixedPay,rules=hourlyRules(),monthlyPay=monthlyPay || monthlySalaryOn(date)!=null)
fun newScheduleRows(rows: List<Shift>,existing: List<Shift>): List<Shift> {
    fun key(s:Shift)=listOf(s.jobId,s.date,s.start,s.end,s.kind)
    val occupied=existing.map(::key).toMutableSet()
    return rows.filter {occupied.add(key(it))}
}
fun generateRotation(job: Job,from: LocalDate,until: LocalDate,blocks: List<RotationBlock>,existing: List<Shift>): List<Shift> {
    require(until>=from && java.time.temporal.ChronoUnit.DAYS.between(from,until)<=366)
    require(blocks.isNotEmpty() && blocks.size<=100 && blocks.all {it.days in 1..366})
    val templates=job.shiftTemplates()
    blocks.forEach {b->b.shift?.let {require(it in templates.indices && templates[it].active);templates[it].validate()}}
    val cycle=blocks.flatMap {b->List(b.days){b.shift}}
    require(cycle.any {it!=null})
    val rows=generateSequence(from){it.plusDays(1)}.takeWhile {it<=until}.mapIndexedNotNull {i,d->
        cycle[i%cycle.size]?.let {job.templateShift(d,templates[it])}
    }.toList()
    return newScheduleRows(rows,existing)
}

/** Reports select shifts by their start date; replacement uses the same boundary. */
fun patternShiftsInPeriod(shifts: List<Shift>,jobId: String,from: LocalDate,until: LocalDate): List<Shift> {
    require(until>=from)
    return shifts.filter {it.jobId==jobId && it.begins.toLocalDate() in from..until}
}

data class PatternChange(val jobId: String,val from: LocalDate,val until: LocalDate,
    val added: List<Shift>,val removedIds: Set<String> = emptySet())

/** Preview/cancel never changes data. One validated save replaces only reviewed IDs. */
fun AppData.withPatternChange(change: PatternChange): AppData {
    require(change.added.isNotEmpty())
    val allowedIds=patternShiftsInPeriod(shifts,change.jobId,change.from,change.until).map {it.id}.toSet()
    require(allowedIds.containsAll(change.removedIds))
    require(change.added.all {it.jobId==change.jobId && it.begins.toLocalDate() in change.from..change.until})
    require(change.added.map {it.id}.distinct().size==change.added.size)
    val remaining=shifts.filterNot {it.id in change.removedIds}
    require(change.added.none {added->remaining.any {it.id==added.id}})
    require(newScheduleRows(change.added,remaining).size==change.added.size)
    return copy(shifts=remaining+change.added)
}

