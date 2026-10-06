package com.paytimeshift.pts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable internal fun ScheduleWarningDetails(warning:ScheduleWarning,jobs:List<Job>) {
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        UiText(if(warning.shortRest) hours(warning.gapMinutes)+" between shifts" else "Overlapping shifts",fontSize=11.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.error)
        Text(shiftIntervalLabel(warning.first,jobs,uiLocale()),fontSize=10.sp)
        Text(shiftIntervalLabel(warning.next,jobs,uiLocale()),fontSize=10.sp)
    }
}
@Composable internal fun ScheduleWarningsList(warnings:List<ScheduleWarning>,jobs:List<Job>) {
    if(warnings.isNotEmpty()) LazyColumn(Modifier.heightIn(max=160.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        items(warnings) {ScheduleWarningDetails(it,jobs)}
    }
}

@Composable internal fun DeleteShiftsDialog(data:AppData,selectedDate:String,close:()->Unit,save:(ShiftDeletion)->Unit) {
    val selected=LocalDate.parse(selectedDate);val month=YearMonth.from(selected)
    var jobId by remember {mutableStateOf(data.shifts.firstOrNull {it.date==selectedDate}?.jobId ?: data.jobs.firstOrNull()?.id)}
    var from by remember {mutableStateOf(month.atDay(1).toString())}
    var until by remember {mutableStateOf(month.atEndOfMonth().toString())}
    var jobMenu by remember {mutableStateOf(false)}
    var reviewed by remember {mutableStateOf<ShiftDeletion?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    val validRange=runCatching {LocalDate.parse(until)>=LocalDate.parse(from)}.getOrDefault(false)
    val entries=runCatching {shiftsForDeletion(data.shifts,jobId,LocalDate.parse(from),LocalDate.parse(until))}.getOrDefault(emptyList())
    BrandedEditor(onDismissRequest=close,error=error,title={ScreenHeading("Delete shifts in a period",Icons.Outlined.DeleteOutline,"Review before saving",compact=true)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Box {
                OutlinedButton(onClick={jobMenu=true},modifier=Modifier.fillMaxWidth()) {Text(data.jobs.find {it.id==jobId}?.name ?: translate("All jobs",LocalLanguage.current))}
                DropdownMenu(jobMenu,{jobMenu=false}) {
                    data.jobs.forEach {job->DropdownMenuItem(text={Text(job.name)},onClick={jobId=job.id;jobMenu=false;error=null})}
                    DropdownMenuItem(text={UiText("All jobs")},onClick={jobId=null;jobMenu=false;error=null})
                }
            }
            FormPair(first={DateControl("From",from){from=it;error=null}},second={DateControl("To",until){until=it;error=null}})
            if(!validRange) UiText("Select valid From and To dates.",fontSize=11.sp,color=MaterialTheme.colorScheme.error)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {UiText("Entries to delete");Text(entries.size.toString(),fontWeight=FontWeight.Bold)}
            UiText("Includes days off and leave. Dates refer to when each shift starts.",fontSize=11.sp)
        }
    },confirmButton={Button(enabled=entries.isNotEmpty(),onClick={
        try {reviewed=ShiftDeletion(jobId,LocalDate.parse(from),LocalDate.parse(until),entries.map {it.id}.toSet())}
        catch(_:Exception) {error="Select valid From and To dates."}
    },contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)){UiText("Review deletion",fontSize=12.sp)}},dismissButton={TextButton(onClick=close){UiText("Cancel",fontSize=12.sp)}})
    reviewed?.let {change->AlertDialog(onDismissRequest={reviewed=null},title={UiText("Delete these entries?")},text={
        Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(data.jobs.find {it.id==change.jobId}?.name ?: translate("All jobs",LocalLanguage.current),fontWeight=FontWeight.Bold)
            val format=DateTimeFormatter.ofPattern("d MMM yyyy",uiLocale())
            Text("${change.from.format(format)} – ${change.until.format(format)}",fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {UiText("Entries to delete");Text(change.reviewedIds.size.toString(),fontWeight=FontWeight.Bold)}
            UiText("Includes days off and leave. Dates refer to when each shift starts.",fontSize=11.sp)
            UiText("Entries outside this selection and job settings stay unchanged.",fontSize=11.sp)
        }
    },confirmButton={TextButton(onClick={
        if(runCatching {data.withShiftDeletion(change)}.isSuccess) {reviewed=null;save(change)}
        else {reviewed=null;error="Schedule changed. Review the shifts again."}
    }) {UiText("Delete",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={reviewed=null}){UiText("Cancel")}})}
}
