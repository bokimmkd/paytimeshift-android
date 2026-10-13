package com.paytimeshift.pts.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ShiftDeletion(val jobId:String?,val from:LocalDate,val until:LocalDate,val reviewedIds:Set<String>)

/** Inclusive start-date scope includes work and non-working entries; null means explicitly all jobs. */
fun shiftsForDeletion(shifts:List<Shift>,jobId:String?,from:LocalDate,until:LocalDate):List<Shift> {
    require(until>=from)
    return shifts.filter {(jobId==null || it.jobId==jobId) && it.begins.toLocalDate() in from..until}
}
fun AppData.withShiftDeletion(change:ShiftDeletion):AppData {
    val current=shiftsForDeletion(shifts,change.jobId,change.from,change.until).map {it.id}.toSet()
    require(current==change.reviewedIds) {"Schedule changed. Review the shifts again."}
    return copy(shifts=shifts.filterNot {it.id in change.reviewedIds})
}

/** User job names remain literal; overnight intervals include their actual end date. */
fun shiftIntervalLabel(shift:Shift,jobs:List<Job>,locale:Locale):String {
    val date=DateTimeFormatter.ofPattern("d MMM yyyy",locale)
    val clock=DateTimeFormatter.ofPattern("HH:mm",locale)
    val first=shift.begins;val last=shift.finishes
    val end=if(first.toLocalDate()==last.toLocalDate()) last.format(clock) else last.format(date)+" "+last.format(clock)
    return (jobs.find {it.id==shift.jobId}?.name ?: shift.jobId)+" · "+first.format(date)+" "+first.format(clock)+" – "+end
}
