@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.paytimeshift.pts.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.platform.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun NumberField(label:String,value:String,change:(String)->Unit) {
    CompactField(label,value,change,keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal)
}
@Composable fun JobPicker(jobs:List<Job>,selected:String,choose:(String)->Unit) {
    var open by remember {mutableStateOf(false)}
    Box {
        CompactChoice("Job",jobs.first {it.id==selected}.name,translateValue=false,
            icon={Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(18.dp))}){open=true}
        DropdownMenu(open,{open=false}) {
            jobs.forEach {job->DropdownMenuItem(text={Text(job.name,fontSize=13.sp)},onClick={choose(job.id);open=false})}
        }
    }
}
@Composable private fun ShiftReviewCard(shift:Shift,selected:Boolean,toggle:()->Unit) {
    Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color=if(selected) MaterialTheme.colorScheme.primary.copy(alpha=.06f) else MaterialTheme.colorScheme.surface,
        border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().heightIn(min=54.dp).toggleable(selected,role=Role.Checkbox){toggle()}.padding(6.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Checkbox(selected,null,Modifier.size(24.dp))
            Column {
                UiText(LocalDate.parse(shift.date).format(DateTimeFormatter.ofPattern("d MMM",uiLocale())),fontSize=10.sp)
                UiText("${shift.start}–${shift.end}",fontSize=12.sp,fontWeight=FontWeight.Medium)
            }
        }
    }
}
@Composable private fun ShiftReviewGrid(shifts:List<Shift>,selected:Set<String>,toggle:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        items(shifts.chunked(2),key={it.first().id}) {pair->
            FormPair(first={ShiftReviewCard(pair[0],pair[0].id in selected){toggle(pair[0].id)}},
                second={pair.getOrNull(1)?.let {s->ShiftReviewCard(s,s.id in selected){toggle(s.id)}}})
        }
    }
}
@Composable fun GapSetting(p:Preferences,change:(Preferences)->Unit) {
    var value by remember(p.gapHours) {mutableStateOf(p.gapHours.toString().removeSuffix(".0"))}
    val n=value.replace(',','.').toDoubleOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){NumberField("Rest between shifts (hours)",value){value=it}}
            Button(onClick={change(p.copy(gapHours=n!!))},enabled=n!=null && n.isFinite() && n in 0.0..168.0 && n!=p.gapHours){Icon(Icons.Outlined.Save,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText("Save",fontSize=12.sp)}
        }
        if(n==null || !n.isFinite() || n !in 0.0..168.0) UiText("Enter 0–168 hours.",color=MaterialTheme.colorScheme.error)
        UiText("Overlaps and short gaps appear in Calendar.",fontSize=11.sp)
    }
}
@Composable fun PatternDialog(data:AppData,close:()->Unit,save:(PatternChange)->Unit) {
    val jobs=data.jobs.filterNot {it.archived}
    if(jobs.isEmpty()) {AlertDialog(onDismissRequest=close,title={UiText("Shift patterns")},text={UiText("No active jobs.")},confirmButton={TextButton(onClick=close){UiText("Close")}});return}
    var jobId by remember {mutableStateOf(jobs.first().id)}
    val job=jobs.first {it.id==jobId};val templates=job.shiftTemplates()
    var from by remember {mutableStateOf(LocalDate.now().toString())};var until by remember {mutableStateOf(LocalDate.now().plusDays(30).toString())}
    var enabled by remember {mutableStateOf(setOf(0))}
    var rotation by remember {mutableStateOf(false)}
    var blocks by remember {mutableStateOf(listOf(RotationBlock(0,2),RotationBlock(null,1)))}
    var days by remember {mutableStateOf(setOf(1,2,3,4,5))}
    var preview by remember {mutableStateOf<List<Shift>?>(null)};var error by remember {mutableStateOf<String?>(null)}
    var selected by remember {mutableStateOf<Set<String>>(emptySet())}
    var replaceExisting by remember {mutableStateOf(false)}
    var removedIds by remember {mutableStateOf<Set<String>>(emptySet())}
    var confirmReplacement by remember {mutableStateOf(false)}
    val existing=runCatching {patternShiftsInPeriod(data.shifts,jobId,LocalDate.parse(from),LocalDate.parse(until))}.getOrDefault(emptyList())
    fun saveReviewed() {save(PatternChange(jobId,LocalDate.parse(from),LocalDate.parse(until),preview!!.filter {it.id in selected},removedIds))}
    val names=listOf("First shift","Second shift","Third shift")
    BrandedEditor(onDismissRequest=close,error=error,title={ScreenHeading("Shift patterns",Icons.Outlined.Repeat,"Repeat and review shifts",compact=true)},text={
        if(preview==null) Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            JobPicker(jobs,jobId){id->jobId=id;enabled=setOf(0);blocks=listOf(RotationBlock(0,2),RotationBlock(null,1));replaceExisting=false;removedIds=emptySet();error=null}
            FormPair(first={DateControl("From",from){from=it;replaceExisting=false}},second={DateControl("Repeat until",until){until=it;replaceExisting=false}})
            if(existing.isNotEmpty()) FormSection("Existing shifts in this period") {
                UiText("${existing.size}",fontSize=13.sp,fontWeight=FontWeight.Bold)
                CompactToggle("Replace existing shifts in this period",replaceExisting){replaceExisting=it}
                UiText("Removes all existing entries for this job in the selected period, including days off and leave, when you confirm the new schedule.",fontSize=10.sp)
            }
            templates.forEachIndexed {i,t->if(t.active) Row(verticalAlignment=Alignment.CenterVertically) {
                val shiftLabel=translate(names[i],LocalLanguage.current)
                Checkbox(i in enabled,{checked->enabled=if(checked) enabled+i else enabled-i;blocks=if(checked) blocks.filter {it.shift!=null}+RotationBlock(i,2)+blocks.filter {it.shift==null} else blocks.filter {it.shift==null || it.shift in enabled}},modifier=Modifier.semantics {contentDescription=shiftLabel})
                Column {UiText(names[i],fontSize=12.sp,fontWeight=FontWeight.Bold);UiText("${t.start}–${t.end} · ${t.breakMinutes} min",fontSize=11.sp)}
            }}
            CompactToggle("Rotation",rotation){rotation=it}
            if(rotation) {
                UiText("Repeat this cycle",fontSize=12.sp)
                blocks.forEachIndexed {index,block->
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.weight(1f)) {SelectionField("Shift",block.shift?.let {names[it]} ?: "Off",enabled.sorted().map {names[it]}+"Off"){value->blocks=blocks.toMutableList().apply {set(index,block.copy(shift=names.indexOf(value).takeIf {it>=0}))}}}
                        Box(Modifier.weight(.65f)) {CompactField("Days",block.days.toString(),{value->value.toIntOrNull()?.let {n->blocks=blocks.toMutableList().apply {set(index,block.copy(days=n))}}},keyboardType=androidx.compose.ui.text.input.KeyboardType.Number)}
                        IconButton(onClick={blocks=blocks.filterIndexed {i,_->i!=index}}){Icon(Icons.Outlined.DeleteOutline,null,Modifier.size(18.dp))}
                    }
                }
                OutlinedButton(onClick={blocks=blocks+RotationBlock(enabled.minOrNull(),2)},enabled=blocks.size<100){UiText("Add cycle step",fontSize=11.sp)}
            } else {
                UiText("Repeat on days")
                val locale=uiLocale()
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){(1..7).forEach {d->FilterChip(d in days,{days=if(d in days) days-d else days+d},label={UiText(java.time.DayOfWeek.of(d).getDisplayName(java.time.format.TextStyle.SHORT,locale))})}}
            }
        } else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            UiText("${selected.size} / ${preview!!.size} shifts",fontSize=12.sp)
            if(removedIds.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {UiText("Existing entries to remove",fontSize=11.sp);UiText(removedIds.size.toString(),fontSize=11.sp,fontWeight=FontWeight.Bold)}
            val alerts=warnings(data.shifts.filterNot {it.id in removedIds}+preview!!.filter {it.id in selected},data.preferences.gapHours,data.jobs)
            alerts.take(3).forEach {UiText(it,fontSize=10.sp,color=MaterialTheme.colorScheme.error)}
            Box(Modifier.weight(1f)){ShiftReviewGrid(preview!!,selected){id->selected=if(id in selected) selected-id else selected+id}}
        }
    },confirmButton={Button(contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp),enabled=preview==null || selected.isNotEmpty(),onClick={
        if(preview==null) try {
            require(enabled.isNotEmpty())
            val a=LocalDate.parse(from);val b=LocalDate.parse(until)
            require(b>=a && java.time.temporal.ChronoUnit.DAYS.between(a,b)<=366)
            val removing=if(replaceExisting) patternShiftsInPeriod(data.shifts,jobId,a,b).map {it.id}.toSet() else emptySet()
            val remaining=data.shifts.filterNot {it.id in removing}
            val rows=if(rotation) {require(blocks.all {it.shift==null || it.shift in enabled});generateRotation(job,a,b,blocks,remaining)} else {
                require(days.isNotEmpty())
                newScheduleRows(generateSequence(a){it.plusDays(1)}.takeWhile {it<=b}.filter {it.dayOfWeek.value in days}.flatMap {d->enabled.sorted().asSequence().map {i->templates[i].validate();job.templateShift(d,templates[i])}}.toList(),remaining)
            }
            preview=rows;selected=rows.map {it.id}.toSet();removedIds=removing;error=if(rows.isEmpty()) "No new shifts." else null
        } catch(_:Exception) {error="Select days and valid dates/times; maximum 366 days."}
        else if(removedIds.isNotEmpty()) confirmReplacement=true else saveReviewed()
    }){Icon(if(preview==null) Icons.Outlined.Visibility else Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText(if(preview==null) "Preview shifts" else "Add these shifts",fontSize=12.sp)}},dismissButton={TextButton(onClick={if(preview!=null) {preview=null;removedIds=emptySet()} else close()}){UiText(if(preview==null) "Cancel" else "Back",fontSize=12.sp)}})
    if(confirmReplacement) AlertDialog(onDismissRequest={confirmReplacement=false},
        title={ScreenHeading("Replace existing shifts?",Icons.Outlined.Repeat,"Review before saving",compact=true)},
        text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(job.name,fontWeight=FontWeight.Bold)
            Text("$from – $until",fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {UiText("Existing entries to remove");Text(removedIds.size.toString(),fontWeight=FontWeight.Bold)}
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {UiText("New shifts to add");Text(selected.size.toString(),fontWeight=FontWeight.Bold)}
            UiText("Other jobs and dates outside this period stay unchanged.",fontSize=11.sp)
        }},
        confirmButton={Button(onClick={confirmReplacement=false;saveReviewed()}) {Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("Replace shifts")}},
        dismissButton={TextButton(onClick={confirmReplacement=false}){UiText("Cancel")}})
}
@Composable fun ImportDialog(data:AppData,close:()->Unit,save:(List<Shift>)->Unit) {
    val jobs=data.jobs.filterNot {it.archived};val context=LocalContext.current;val scope=rememberCoroutineScope()
    if(jobs.isEmpty()) {AlertDialog(onDismissRequest=close,title={UiText("Import roster")},text={UiText("No active jobs.")},confirmButton={TextButton(onClick=close){UiText("Close")}});return}
    var jobId by remember {mutableStateOf(jobs.first().id)};var text by remember {mutableStateOf("")}
    var colleagueText by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
    var preview by remember {mutableStateOf<List<Shift>?>(null)};var selected by remember {mutableStateOf<Set<String>>(emptySet())}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) {busy=true;scope.launch {try {val raw=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.bufferedReader().use {reader->val chars=CharArray(4*1024*1024+1);var count=0;while(count<chars.size){val n=reader.read(chars,count,chars.size-count);if(n<0) break;count+=n};require(count<=4*1024*1024);String(chars,0,count)}}
                colleagueText=raw;text=""} catch(e:Exception){error="Could not read file. Try another image or enter text below."} finally{busy=false}}}}
    val rosterPick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) {busy=true;scope.launch {try {colleagueText=null;text=withContext(Dispatchers.IO){recognizeRoster(context,uri)}} catch(_:Exception){error="Could not read file. Try another image or enter text below."} finally{busy=false}}}}
    BrandedEditor(onDismissRequest={if(!busy) close()},error=error,title={ScreenHeading("Import roster",Icons.Outlined.FileUpload,"Review before saving",compact=true)},text={
        if(preview==null) Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            FlowRow {jobs.forEach {j->FilterChip(jobId==j.id,{jobId=j.id},label={androidx.compose.material3.Text(j.name)})}}
            OutlinedButton(onClick={rosterPick.launch(arrayOf("image/*","application/pdf"))},enabled=!busy){UiText("Choose image or PDF")}
            OutlinedButton(onClick={pick.launch(arrayOf("*/*"))},enabled=!busy){UiText("Import schedule from colleague")}
            if(colleagueText!=null) UiText("Schedule file loaded. Select your job and review shifts.",fontSize=11.sp)
            UiText("Image recognition supports Latin text. You can correct or enter Cyrillic text below.",fontSize=11.sp)
            OutlinedTextField(text,{text=it},label={UiText("Paste or edit recognized text")},modifier=Modifier.fillMaxWidth(),minLines=4)
            UiText("2026-10-02 07:00-15:00\n03.10.2026 18:00-23:00",fontSize=11.sp)
            if(busy) CircularProgressIndicator()
        } else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            UiText("Check dates and times before importing. Nothing is saved automatically.",fontSize=12.sp)
            warnings(data.shifts+preview!!.filter {it.id in selected},data.preferences.gapHours,data.jobs).take(3).forEach {UiText(it,fontSize=10.sp,color=MaterialTheme.colorScheme.error)}
            UiText("${selected.size} / ${preview!!.size} shifts",fontSize=12.sp)
            Box(Modifier.weight(1f)){ShiftReviewGrid(preview!!,selected){id->selected=if(id in selected) selected-id else selected+id}}
        }
    },confirmButton={Button(contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp),enabled=!busy,onClick={
        if(preview==null) {
            val rows=try {colleagueText?.let {decodeColleagueSchedule(it,jobs.first {j->j.id==jobId},data.shifts)} ?: parseRoster(text,jobs.first {it.id==jobId},data.shifts)} catch(_:Exception){error="Invalid schedule file.";emptyList()}
            if(rows.isEmpty()) error="No matching shifts. Use YYYY-MM-DD HH:mm-HH:mm, one shift per line."
            else {preview=rows;selected=rows.map {it.id}.toSet();error=null}
        } else if(selected.isNotEmpty()) save(preview!!.filter {it.id in selected})
    }){Icon(if(preview==null) Icons.Outlined.Visibility else Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText(if(preview==null) "Review shifts" else "Import shifts",fontSize=12.sp)}},dismissButton={TextButton(onClick={if(preview!=null) preview=null else if(!busy) close()}){Icon(if(preview==null) Icons.Outlined.Close else Icons.Outlined.ChevronLeft,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText(if(preview==null) "Cancel" else "Back",fontSize=12.sp)}})
}
@Composable fun BackupControls(data:AppData,restore:(AppData)->Unit,info:(String)->Unit) {
    val context=LocalContext.current;var pending by remember {mutableStateOf<AppData?>(null)};val scope=rememberCoroutineScope()
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null) scope.launch {try {withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use {it.write(LocalStore.encode(data))}};info("Backup saved.")} catch(e:Exception){info("File could not be saved.")}}}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null) scope.launch {try {pending=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.bufferedReader().use {reader-> val chars=CharArray(4*1024*1024+1);var count=0;while(count<chars.size){val n=reader.read(chars,count,chars.size-count);if(n<0) break;count+=n};require(count<=4*1024*1024);LocalStore.decode(String(chars,0,count))}}} catch(e:Exception){info("Invalid backup. Current data is unchanged.")}}}
    FormPair(first={OutlinedButton(onClick={create.launch("PTS-backup-${LocalDate.now()}.json")},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),contentPadding=PaddingValues(6.dp)){Icon(Icons.Outlined.SaveAlt,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));UiText("Save backup file",fontSize=11.sp)}},
        second={OutlinedButton(onClick={open.launch(arrayOf("application/json","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),contentPadding=PaddingValues(6.dp)){Icon(Icons.Outlined.Restore,null,Modifier.size(15.dp));Spacer(Modifier.width(5.dp));UiText("Restore backup file",fontSize=11.sp)}})
    if(pending!=null) AlertDialog(onDismissRequest={pending=null},title={UiText("Restore backup?")},text={UiText("This replaces your local jobs and shifts. Save a backup first.")},confirmButton={TextButton(onClick={restore(pending!!);pending=null}){UiText("Restore backup file")}},dismissButton={TextButton(onClick={pending=null}){UiText("Cancel")}})
}
@Composable fun ReminderSetting(p:Preferences,change:(Preferences)->Unit) {
    var value by remember(p.reminderMinutes) {mutableStateOf(p.reminderMinutes.toString())}
    val n=value.toIntOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){NumberField("Minutes before shift (0 = off)",value){value=it}}
            Button(onClick={change(p.copy(reminderMinutes=n!!))},enabled=n!=null && n in 0..10080 && n!=p.reminderMinutes){Icon(Icons.Outlined.Save,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText("Save",fontSize=12.sp)}
        }
        UiText("Notifications may be delayed by Android battery settings.",fontSize=11.sp)
    }
}

@Composable fun ColleagueExportDialog(data:AppData,month:java.time.YearMonth,close:()->Unit,share:(Job,LocalDate,LocalDate)->Unit) {
    if(data.jobs.isEmpty()) {close();return}
    var jobId by remember {mutableStateOf(data.jobs.first().id)}
    var from by remember {mutableStateOf(month.atDay(1).toString())};var until by remember {mutableStateOf(month.atEndOfMonth().toString())}
    var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=close,title={UiText("Export schedule for colleague")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        JobPicker(data.jobs,jobId){jobId=it}
        DateControl("From",from){from=it};DateControl("Repeat until",until){until=it}
        UiText("Dates, shifts and breaks only. Pay and private notes are excluded.",fontSize=11.sp)
        error?.let {UiText(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={val a=LocalDate.parse(from);val b=LocalDate.parse(until);if(b<a) error="Select days and valid dates/times; maximum 366 days." else share(data.jobs.first {it.id==jobId},a,b)}){UiText("Share")}},dismissButton={TextButton(onClick=close){UiText("Cancel")}})
}
