@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.paytimeshift.pts.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.platform.*
import com.paytimeshift.pts.premium.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Teal = Color(0xFF00857C)
private val Navy = Color(0xFF071C43)
private val Pale = Color(0xFFFCFCFC)
private val Round = RoundedCornerShape(10.dp)
private enum class Tab(val title: String, val icon: ImageVector) {
    Today("Today", Icons.Outlined.Home), Calendar("Calendar", Icons.Outlined.CalendarMonth),
    Earnings("Earnings", Icons.Outlined.BarChart), Jobs("Jobs", Icons.Outlined.WorkOutline)
}

@Composable fun PtsApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf(AppData()) }
    var loaded by remember { mutableStateOf(false) }
    var writable by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val premiumRepo=remember {PremiumRepository(context)}
    var account by remember {mutableStateOf(AccountStatus(uid=premiumRepo.auth.currentUser?.uid))}
    var accountResolved by remember {mutableStateOf(premiumRepo.auth.currentUser==null)}
    var accountOpen by remember {mutableStateOf(false)}
    var analyticsOpen by remember {mutableStateOf(false)}
    var annualPrice by remember {mutableStateOf<String?>(null)}
    fun refreshAccount() {scope.launch {
        try {account=premiumRepo.status();accountResolved=true}
        catch(_:Exception) {accountResolved=premiumRepo.auth.currentUser==null}
    }}
    val billing=remember {PlayBilling(context as android.app.Activity,{premiumRepo.auth.currentUser?.uid},{token->scope.launch {
        try {premiumRepo.verifyPurchase(token);refreshAccount()}
        catch(e:Exception) {message=cloudError(e)}
    }},{message=it},{annualPrice=it})}
    DisposableEffect(Unit) {
        val listener=com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            account=AccountStatus(uid=it.currentUser?.uid,email=it.currentUser?.email ?: "");accountResolved=it.currentUser==null
            refreshAccount();billing.restore()
        }
        premiumRepo.auth.addAuthStateListener(listener);billing.start()
        onDispose {premiumRepo.auth.removeAuthStateListener(listener);billing.close()}
    }
    LaunchedEffect(account.uid,accountResolved,data.preferences.language) {
        if(account.uid!=null && accountResolved) try {premiumRepo.reportPreferences(account.monthlyEmail,account.yearlyEmail,data.preferences.language)} catch(_:Exception) { }
    }
    LaunchedEffect(account.premium,account.uid) {if(account.premium && premiumRepo.bound(account.uid!!)) queueCloudBackup(context,account.uid)}
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var reportMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var jobEditor by remember { mutableStateOf<Job?>(null) }
    var addJob by remember { mutableStateOf(false) }
    var shiftEditor by remember { mutableStateOf<Shift?>(null) }
    var addShift by remember { mutableStateOf(false) }
    var newShiftDate by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var patterns by remember {mutableStateOf(false)}
    var importer by remember {mutableStateOf(false)}
    var shareMonth by remember {mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {allowed->
        if(allowed) scope.launch {withContext(Dispatchers.IO){syncReminders(context,data,data)}}
        else message="Notifications need permission in phone settings."
    }
    var sampleConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try { data = withContext(Dispatchers.IO) { store.load() } }
        catch (e: Exception) { writable = false; message = "Could not read saved data. Your file has been kept. Close the app and contact support before making changes." }
        loaded = true
        if(writable) withContext(Dispatchers.IO){syncReminders(context,data,data);refreshWidgets(context)}
    }
    fun commit(next: AppData) {
        if (!loaded || !writable || saving) return
        saving = true
        scope.launch {
            try { withContext(Dispatchers.IO) { store.save(next);try {syncReminders(context,data,next);refreshWidgets(context)} catch(_:Exception) { } }; data = next;queueCloudBackup(context,premiumRepo.auth.currentUser?.uid)
                val wantsReminders=next.preferences.reminderMinutes>0 || next.jobs.any {it.reminderMinutes>0}
                if(wantsReminders && android.os.Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            catch (e: Exception) { message = "Changes could not be saved. Please try again." }
            finally { saving = false }
        }
    }
    val dark = data.preferences.appearance == "Dark" || (data.preferences.appearance == "System" && isSystemInDarkTheme())
    androidx.compose.runtime.CompositionLocalProvider(LocalLanguage provides data.preferences.language, LocalPremium provides account.premium, LocalAdsResolved provides accountResolved) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary=Color(0xFF69D6C6), secondary=Color(0xFF69D6C6))
        else lightColorScheme(primary=Teal, secondary=Teal, background=Pale, surface=Color.White, onSurface=Navy, onBackground=Navy, onSurfaceVariant=Color(0xFF4F5F7B), outlineVariant=Color(0xFFE5E8ED),surfaceVariant=Color(0xFFEBEDF1))) {
        BackHandler(settings || tab != Tab.Today) { if (settings) settings=false else tab=Tab.Today }
        Scaffold(
            topBar = { Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=16.dp,end=8.dp,top=3.dp,bottom=5.dp), verticalAlignment=Alignment.CenterVertically) {
                BrandLogo()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { UiText("PTS",fontSize=18.sp,fontWeight=FontWeight.Bold); UiText("Pay Time Shift",fontSize=9.sp) }
                IconButton(onClick={settings=!settings}) { Icon(if(settings) Icons.Outlined.Close else Icons.Outlined.Settings,if(settings) "Close settings" else "Settings") }
            } },
            bottomBar = { if (!settings) BrandNavigation(tab) {tab=it} },
            floatingActionButton = { if (!settings && tab in listOf(Tab.Today,Tab.Calendar) && loaded && writable)
                ShiftButton(if(data.jobs.none { !it.archived }) "Job" else "Shift") {if(data.jobs.none { !it.archived }) addJob=true else {if(tab==Tab.Today) newShiftDate=LocalDate.now().toString();addShift=true}}
            }

        ) { inset ->
            if (!loaded) Box(Modifier.fillMaxSize().padding(inset),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(Modifier.fillMaxSize().padding(inset),contentPadding=PaddingValues(16.dp,8.dp,16.dp,if(tab==Tab.Calendar && !settings) 76.dp else 14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(settings) item { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {SettingsScreen(data.preferences,{ commit(data.copy(preferences=it)) },{accountOpen=true},account.premium);FormSection("Local backup"){BackupControls(data,{commit(it)},{message=it})}} }
                else when(tab) {
                    Tab.Today -> item { TodayScreen(data,{shiftEditor=it},{addJob=true},{sampleConfirm=true},{reportMonthText=YearMonth.now().toString();tab=Tab.Earnings}) }
                    Tab.Calendar -> item { CalendarScreen(data,reportMonthText,{reportMonthText=it},{shiftEditor=it},{month->shareMonth=month},{importer=true},{date->commit(data.withHoliday(date))},{newShiftDate=it}) }
                    Tab.Earnings -> item { EarningsScreen(data,reportMonthText,{reportMonthText=it},{if(account.premium) analyticsOpen=true else accountOpen=true}) }
                    Tab.Jobs -> item { JobsScreen(data,{jobEditor=it},{addJob=true},{message=it},{patterns=true},{accountOpen=true}) }
                }
            }
        }
        if(accountOpen) AccountDialog(premiumRepo,account,annualPrice,{refreshAccount()},{billing.buy()},{billing.restore()},data,{next->
            check(loaded && writable && !saving);saving=true
            try {withContext(Dispatchers.IO){store.save(next);syncReminders(context,data,next);refreshWidgets(context)};data=next}
            finally {saving=false}
        },{accountOpen=false})
        if(analyticsOpen && account.premium) AnalyticsDialog(data,YearMonth.parse(reportMonthText)){analyticsOpen=false}
        if (addJob || jobEditor != null) JobDialog(jobEditor,data.preferences.currency,onClose={addJob=false;jobEditor=null}) { job, applyUpcoming ->
            commit(data.withJob(job,applyUpcoming,LocalDate.now())); addJob=false;jobEditor=null
        }
        if (addShift || shiftEditor != null) ShiftDialog(shiftEditor,data,newShiftDate,onClose={addShift=false;shiftEditor=null},onDelete={ id ->
            commit(data.copy(shifts=data.shifts.filterNot {it.id==id}));addShift=false;shiftEditor=null
        }) { shift -> commit(data.copy(shifts=data.shifts.filterNot {it.id==shift.id} + shift));addShift=false;shiftEditor=null }
        if(patterns) PatternDialog(data,{patterns=false}) {rows->commit(data.copy(shifts=data.shifts+rows));patterns=false}
        if(importer) ImportDialog(data,{importer=false}) {rows->commit(data.copy(shifts=data.shifts+rows));importer=false}
        shareMonth?.let {month->AlertDialog(onDismissRequest={shareMonth=null},title={ScreenHeading("Share schedule", Icons.Outlined.Share, "Export as PDF or image", compact=true)},text={Row {
            TextButton(onClick={scope.launch {try {shareSchedule(context,data,YearMonth.parse(month),true)} catch(_:Exception){message="File could not be saved."}};shareMonth=null}){UiText("PDF")}
            TextButton(onClick={scope.launch {try {shareSchedule(context,data,YearMonth.parse(month),false)} catch(_:Exception){message="File could not be saved."}};shareMonth=null}){UiText("Image")}
        }},confirmButton={TextButton(onClick={shareMonth=null}){UiText("Cancel")}})}
        if(sampleConfirm) AlertDialog(onDismissRequest={sampleConfirm=false},title={UiText("Load example schedule?")},text={UiText("Add Factory, Taxi and Restaurant with example shifts. You can edit them or start with your own jobs instead.")},confirmButton={TextButton(onClick={if(data.jobs.isEmpty()) commit(sampleData().copy(preferences=data.preferences)) else message="Examples are available only before adding jobs.";sampleConfirm=false}){UiText("Load examples")}},dismissButton={TextButton(onClick={sampleConfirm=false}){UiText("Cancel")}})
        if (saving) androidx.compose.ui.window.Dialog(onDismissRequest={}) { Surface(shape=Round) { Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); UiText("Saving…") } } }
        message?.let { text -> AlertDialog(onDismissRequest={message=null},title={UiText("PTS")},text={UiText(text)},confirmButton={TextButton(onClick={message=null}){UiText("OK")}}) }
    }
}
}

@Composable private fun Stack(content: @Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
}
@Composable private fun Heading(title: String, subtitle: String?=null) {
    val icon = when (title) {
        "Today" -> Icons.Outlined.Home
        "Your jobs" -> Icons.Outlined.WorkOutline
        else -> Icons.Outlined.Tune
    }
    val caption = subtitle ?: when (title) {
        "Today" -> "Shifts and earnings today"
        "Settings" -> "Display, reminders and backup"
        else -> null
    }
    ScreenHeading(title, icon, caption)
}
@Composable private fun Panel(modifier: Modifier=Modifier, color: Color=MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.()->Unit) {
    Surface(modifier.fillMaxWidth(),shape=Round,color=color,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=.6.dp) {
        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
    }
}
@Composable private fun BrandLogo() {
    val ink=MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(32.dp)) {
        val stroke=3.dp.toPx(); val center=Offset(size.width/2,size.height/2)
        drawCircle(ink,size.minDimension/2-stroke/2,center,style=Stroke(stroke))
        drawLine(ink,Offset(center.x,size.height*.22f),center,3.2.dp.toPx(),StrokeCap.Round)
        drawLine(ink,center,Offset(size.width*.72f,center.y),3.2.dp.toPx(),StrokeCap.Round)
        drawLine(Teal,Offset(size.width*.41f,size.height*.60f),Offset(size.width*.29f,size.height*.73f),3.2.dp.toPx(),StrokeCap.Round)
    }
}
private fun jobGlyph(job: Job): ImageVector = when {
    (job.name.contains("taxi",true) || job.name.contains("такси",true)) -> Icons.Filled.LocalTaxi
    (job.name.contains("restaurant",true) || job.name.contains("ресторан",true)) -> Icons.Filled.Restaurant
    else -> Icons.Filled.Factory
}
@Composable private fun JobIcon(job: Job, size: androidx.compose.ui.unit.Dp=48.dp) {
    Box(Modifier.size(size).background(Color(job.color).copy(alpha=.10f),CircleShape),contentAlignment=Alignment.Center) {
        Icon(jobGlyph(job),null,Modifier.size(size*.52f),tint=Color(job.color))
    }
}
@Composable private fun BrandNavigation(selected: Tab, choose: (Tab)->Unit) {
    Surface(color=MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(thickness=.7.dp,color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=6.dp,vertical=3.dp)) {
                Tab.entries.forEach { item ->
                    val active=item==selected
                    val icon=if(active) when(item) {Tab.Today->Icons.Filled.Home;Tab.Calendar->Icons.Filled.CalendarMonth;Tab.Earnings->Icons.Filled.BarChart;Tab.Jobs->Icons.Filled.Work} else item.icon
                    val tint=if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=androidx.compose.ui.semantics.Role.Tab){choose(item)}.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Icon(icon,null,Modifier.size(21.dp),tint=tint)
                        UiText(item.title,fontSize=10.sp,lineHeight=13.sp,fontWeight=if(active) FontWeight.Bold else FontWeight.Normal,color=tint)
                    }
                }
            }
        }
    }
}
@Composable private fun ShiftButton(label: String, click: ()->Unit) {
    Surface(onClick=click,shape=RoundedCornerShape(28.dp),color=Teal,contentColor=Color.White,shadowElevation=7.dp) {
        Row(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF009B8A),Color(0xFF007E77)))).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Add,null,Modifier.size(21.dp));UiText(label,fontSize=14.sp,fontWeight=FontWeight.Medium)
        }
    }
}
@Composable private fun displayMoney(value: java.math.BigDecimal,code: String): String {
    val currency=java.util.Currency.getInstance(code)
    val formatter=java.text.NumberFormat.getNumberInstance(Locale.US).apply {minimumFractionDigits=0;maximumFractionDigits=currency.defaultFractionDigits.coerceAtLeast(0)}
    val symbol=currency.getSymbol(Locale.US)
    return (if(symbol.length>3 || symbol==code) "$code " else symbol)+formatter.format(value)
}
@Composable private fun timeLabel(time: String,p: Preferences)=LocalTime.parse(time).format(DateTimeFormatter.ofPattern(if(p.time24) "HH:mm" else "h:mm a",uiLocale()))
private fun shiftLabel(shift: Shift): String {
    val hour=shift.begins.hour
    return when {shift.finishes.toLocalDate()>shift.begins.toLocalDate() || hour>=22 || hour<5 -> "Night";hour<12 -> "Morning";else -> "Afternoon"}
}
@Composable private fun SectionLabel(label: String) { UiText(label,fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=16.sp) }
@Composable private fun StripedCard(color: Color, click: ()->Unit, content: @Composable RowScope.()->Unit) {
    Surface(modifier=Modifier.fillMaxWidth(),onClick=click,shape=Round,color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=.6.dp) {
        Row(Modifier.height(IntrinsicSize.Min),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.width(7.dp).fillMaxHeight().background(color))
            Row(Modifier.weight(1f).padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,content=content)
        }
    }
}
@Composable private fun ShiftCard(shift: Shift,data: AppData,click: ()->Unit,compact: Boolean=false,badge: Boolean=false) {
    val job=data.jobs.firstOrNull {it.id==shift.jobId} ?: return
    StripedCard(Color(job.color),click) {
        JobIcon(job,if(compact) 36.dp else 40.dp);Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                androidx.compose.material3.Text(job.name,Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=if(compact) 14.sp else 15.sp,lineHeight=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(badge) Surface(shape=RoundedCornerShape(5.dp),color=Color(job.color).copy(alpha=.10f)) {
                    UiText(shiftLabel(shift),Modifier.padding(horizontal=7.dp,vertical=2.dp),fontSize=10.sp,lineHeight=13.sp)
                }
            }
            UiText(if(shift.kind!="Work") translate(shift.kind,LocalLanguage.current) else "${timeLabel(shift.start,data.preferences)} – ${timeLabel(shift.end,data.preferences)}${if(shift.finishes.toLocalDate()>shift.begins.toLocalDate()) " +1" else ""}",fontWeight=FontWeight.Medium,fontSize=15.sp,lineHeight=19.sp)
            UiText("${hours(shift.paidMinutes)} · ${displayMoney(shift.earnings(),shift.currency)} ${translate("estimated",LocalLanguage.current)}",fontSize=11.sp,lineHeight=15.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(4.dp));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(19.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun EmptyState(add: ()->Unit,sample: ()->Unit,showExample:Boolean) {Panel {
    UiText("One app for every job you work.",fontWeight=FontWeight.Bold)
    UiText("Add a job, set your pay and start planning your shifts.")
    Button(onClick=add){UiText("Add your first job")};if(showExample) TextButton(onClick=sample){UiText("Explore example schedule")}
}}
@Composable private fun MiniRow(icon: ImageVector,tint: Color,click: ()->Unit,content: @Composable RowScope.()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=42.dp).clickable(onClick=click).padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,Modifier.size(23.dp),tint=tint);Spacer(Modifier.width(10.dp));content();Spacer(Modifier.width(4.dp));Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun AdSlot() { PtsBanner() }
@Composable private fun TodayScreen(data: AppData,edit: (Shift)->Unit,add: ()->Unit,sample: ()->Unit,openEarnings: ()->Unit) {
    val today=LocalDate.now();val rows=data.shifts.filter {it.date==today.toString()}.sortedBy {it.begins}
    val month=data.shifts.filter {YearMonth.from(it.begins)==YearMonth.from(today)}
    val weekStart=today.minusDays((today.dayOfWeek.value-(if(data.preferences.mondayFirst) 1 else 7)+7L)%7)
    val payments=data.jobs.filter {j->data.shifts.any {it.jobId==j.id} && nextPayday(j,today)>=today}.sortedBy {nextPayday(it,today)}.take(2)
    val summaryInk=if(MaterialTheme.colorScheme.background.luminance()<.4f) MaterialTheme.colorScheme.primary else Color(0xFF004B55)
    Stack {
        Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
            UiText(today.format(DateTimeFormatter.ofPattern("EEE, MMM d",uiLocale())),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Heading("Today")
        }
        if(data.jobs.none { !it.archived }) EmptyState(add,sample,data.jobs.isEmpty())
        else {
            if(rows.isEmpty()) Panel {UiText("No shifts added today",fontSize=14.sp)}
            rows.forEachIndexed {index,s->ShiftCard(s,data,{edit(s)},badge=index==0)}
            Surface(shape=Round,color=if(MaterialTheme.colorScheme.background==Pale) Color(0xFFE2F5EF) else MaterialTheme.colorScheme.primary.copy(alpha=.15f)) {
                Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Teal.copy(alpha=.15f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Filled.AccountBalanceWallet,null,Modifier.size(26.dp),tint=summaryInk)}
                    Spacer(Modifier.width(12.dp));Column {
                        Row(verticalAlignment=Alignment.Bottom) {UiText(hours(rows.sumOf {it.paidMinutes}),fontSize=28.sp,lineHeight=31.sp,fontWeight=FontWeight.Bold,color=summaryInk);Spacer(Modifier.width(6.dp));UiText("today",fontSize=16.sp,fontWeight=FontWeight.Bold)}
                        totals(rows).forEach {(code,value)->Row(verticalAlignment=Alignment.CenterVertically) {UiText(displayMoney(value,code),fontSize=21.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(4.dp));UiText("estimated earnings",fontSize=12.sp)}}
                    }
                }
            }
            data.shifts.filter {it.kind=="Work" && it.begins>java.time.LocalDateTime.now()}.minByOrNull {it.begins}?.let {next->
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Next shift")
                    val job=data.jobs.first {it.id==next.jobId}
                    Surface(shape=Round,color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
                        MiniRow(jobGlyph(job),Color(job.color),{edit(next)}) {
                            UiText("${job.name} · ${if(next.date==today.toString()) translate("Today",LocalLanguage.current) else next.begins.format(DateTimeFormatter.ofPattern("MMM d",uiLocale()))} at ${timeLabel(next.start,data.preferences)}",Modifier.weight(1f),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                SmallStat(Modifier.weight(1f),Icons.Outlined.CalendarMonth,"This week",hours(data.shifts.filter {it.begins.toLocalDate()>=weekStart && it.begins.toLocalDate()<weekStart.plusDays(7)}.sumOf {it.paidMinutes}))
                SmallStat(Modifier.weight(1f),Icons.Filled.BarChart,"This month",hours(month.sumOf {it.paidMinutes}),"planned")
            }
            if(payments.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                SectionLabel("Next payday")
                Surface(shape=Round,color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
                    Column {payments.forEachIndexed {index,j->
                        if(index>0) HorizontalDivider(Modifier.padding(horizontal=12.dp),thickness=.5.dp,color=MaterialTheme.colorScheme.outlineVariant)
                        MiniRow(Icons.Outlined.CalendarMonth,if(index==0) Teal else Navy,openEarnings) {
                            UiText("${j.name} · ${nextPayday(j,today).format(DateTimeFormatter.ofPattern("MMM d",uiLocale()))}",Modifier.weight(1f),fontSize=12.sp)
                        }
                    }}
                }
            }
            Spacer(Modifier.height(4.dp));AdSlot()
        }
    }
}
@Composable private fun SmallStat(modifier: Modifier,icon: ImageVector,title: String,value: String,suffix: String="") {
    Panel(modifier.fillMaxHeight()) {
        Row(Modifier.heightIn(min=45.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(icon,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                UiText(title,fontSize=11.sp,color=MaterialTheme.colorScheme.primary,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    UiText(value,fontSize=18.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,maxLines=1)
                    if(suffix.isNotBlank()) UiText(suffix,Modifier.weight(1f),fontSize=8.sp,lineHeight=11.sp,
                        color=MaterialTheme.colorScheme.primary,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
        }
    }
}
@Composable private fun CalendarScreen(data: AppData,monthText:String,onMonthChange:(String)->Unit,edit: (Shift)->Unit,share:(String)->Unit,openImport:()->Unit,toggleHoliday:(String)->Unit,onSelected:(String)->Unit) {
    val month=YearMonth.parse(monthText)
    var selectedText by rememberSaveable(monthText) {mutableStateOf(if(month==YearMonth.now()) LocalDate.now().toString() else month.atDay(1).toString())};val selected=LocalDate.parse(selectedText);val locale=uiLocale();var menu by remember {mutableStateOf(false)}
    LaunchedEffect(selectedText) {onSelected(selectedText)}
    val selectedShifts=data.shifts.filter {it.date==selectedText}.sortedBy {it.begins}
    val offset=(month.atDay(1).dayOfWeek.value-(if(data.preferences.mondayFirst) 1 else 7)+7)%7
    Stack {
        Row(verticalAlignment=Alignment.CenterVertically) {
            ScreenHeading(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",uiLocale())), Icons.Outlined.CalendarMonth, "Plan shifts and mark holidays", modifier=Modifier.weight(1f))
            Box {
                IconButton(onClick={menu=true},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.MoreVert,translate("Calendar actions",LocalLanguage.current),Modifier.size(19.dp))}
                DropdownMenu(menu,{menu=false}) {
                    DropdownMenuItem(text={UiText(if(selectedText in data.holidays) "Unmark holiday" else "Mark as holiday")},leadingIcon={Icon(Icons.Outlined.Event,null)},onClick={menu=false;toggleHoliday(selectedText)})
                    DropdownMenuItem(text={UiText("Import roster")},onClick={menu=false;openImport()})
                    DropdownMenuItem(text={UiText("Share schedule")},onClick={menu=false;share(monthText)})
                }
            }
            IconButton(onClick={onMonthChange(month.minusMonths(1).toString())},modifier=Modifier.size(36.dp)){Icon(Icons.Outlined.ChevronLeft,translate("Previous month",LocalLanguage.current),Modifier.size(20.dp))}
            IconButton(onClick={onMonthChange(month.plusMonths(1).toString())},modifier=Modifier.size(36.dp)){Icon(Icons.Outlined.ChevronRight,translate("Next month",LocalLanguage.current),Modifier.size(20.dp))}
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(17.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            data.jobs.forEach {job->Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(14.dp).background(Color(job.color),CircleShape));Spacer(Modifier.width(7.dp));androidx.compose.material3.Text(job.name,fontSize=11.sp)}}
        }
        Surface(shape=Round,color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                val days=if(data.preferences.mondayFirst) (1..7).map {java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT,locale)} else (listOf(7)+ (1..6).toList()).map {java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT,locale)}
                Row(Modifier.height(28.dp),verticalAlignment=Alignment.CenterVertically) {days.forEach {UiText(it,Modifier.weight(1f),fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
                for(week in 0 until (offset+month.lengthOfMonth()+6)/7) Row {
                    for(col in 0..6) {
                        val day=week*7+col-offset+1
                        val base=Modifier.weight(1f).height(49.dp).border(.35.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
                        if(day !in 1..month.lengthOfMonth()) Box(base)
                        else {
                            val date=month.atDay(day);val dayRows=data.shifts.filter {it.date==date.toString()}
                            val holiday=date.toString() in data.holidays
                            val holidayBackground=if(holiday) MaterialTheme.colorScheme.primary.copy(alpha=.10f) else Color.Transparent
                            val selectedModifier=Modifier.padding(2.dp).background(holidayBackground,RoundedCornerShape(7.dp)).then(if(date==selected) Modifier.border(1.2.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(7.dp)) else Modifier)
                            Box(base.clickable {selectedText=date.toString()}.semantics {contentDescription="${date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d",locale))}, ${dayRows.size} shifts"}) {
                                Column(Modifier.fillMaxSize().then(selectedModifier).padding(top=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                    UiText(day.toString(),fontSize=12.sp,lineHeight=15.sp,fontWeight=if(date==selected) FontWeight.Bold else FontWeight.Normal)
                                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                        dayRows.map {it.jobId}.distinct().take(3).forEach {id->data.jobs.find {it.id==id}?.let {j->Box(Modifier.size(7.dp).background(Color(j.color),CircleShape))}}
                                        if(dayRows.map {it.jobId}.distinct().size>3) UiText("+",fontSize=10.sp,lineHeight=10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {SectionLabel("${selected.format(DateTimeFormatter.ofPattern("EEE, MMM d",uiLocale()))} · ${selectedShifts.size} shifts")}
            if(selectedText in data.holidays) Surface(shape=RoundedCornerShape(5.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.12f)){UiText("Holiday",Modifier.padding(horizontal=7.dp,vertical=3.dp),fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
        }
        if(selectedShifts.isEmpty()) UiText("No shifts added",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        selectedShifts.forEach {ShiftCard(it,data,{edit(it)},compact=true)}
        val nearby=data.shifts.filter {it.begins.toLocalDate()>=selected.minusDays(1) && it.begins.toLocalDate()<=selected.plusDays(1)}
        warnings(nearby,data.preferences.gapHours,data.jobs).filter {it.contains(selectedText)}.forEach {warning->
            val concise=if(warning.startsWith("Only ")) warning.substringAfter("Only ").substringBefore(" between")+" between shifts" else "Overlapping shifts"
            Surface(shape=RoundedCornerShape(8.dp),color=Color(0xFFFFF0D2)) {Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Outlined.Schedule,null,Modifier.size(21.dp),tint=Color(0xFF9B4A00));Spacer(Modifier.width(10.dp));UiText(concise,fontSize=12.sp,color=Color(0xFF9B4A00))}}
        }
    }
}
@Composable private fun EarningsScreen(data: AppData,monthText:String,onMonthChange:(String)->Unit,openAnalytics:()->Unit) {
    val month=YearMonth.parse(monthText)
    var picker by remember {mutableStateOf(false)};var period by rememberSaveable(monthText) {mutableStateOf("Month")}
    var paymentDetails by remember {mutableStateOf<Job?>(null)}
    val today=LocalDate.now();val weekStart=today.minusDays((today.dayOfWeek.value-(if(data.preferences.mondayFirst) 1 else 7)+7L)%7)
    val rows=if(period=="Month") shiftsForMonth(data.shifts,month) else data.shifts.filter {s->
        val date=s.begins.toLocalDate()
        when(period) {"Week"->date>=weekStart && date<weekStart.plusDays(7)
            "Pay period"->data.jobs.find {it.id==s.jobId}?.let {j->val (from,to)=payPeriod(j,nextPayday(j,today));date>=from && date<=to} ?: false
            else->YearMonth.from(s.begins)==month}
    }
    val payments=data.jobs.filter {j->data.shifts.any {it.jobId==j.id} && nextPayday(j,today)>=today}.sortedBy {nextPayday(it,today)}
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        ScreenHeading("Estimated earnings", Icons.Outlined.AccountBalanceWallet, "Hours, pay and next payments")
        Surface(onClick={picker=true},shape=RoundedCornerShape(7.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().heightIn(min=36.dp).padding(horizontal=13.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                UiText(if(period=="Week") "${weekStart.format(DateTimeFormatter.ofPattern("MMM d",uiLocale()))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d",uiLocale()))}" else if(period=="Pay period") translate("Current pay period",LocalLanguage.current) else month.format(DateTimeFormatter.ofPattern("MMMM yyyy",uiLocale())),Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.Medium)
                Icon(Icons.Outlined.KeyboardArrowDown,"Select month",Modifier.size(20.dp))
            }
        }
        Surface(shape=Round,color=Color(0xFF004A56)) {
            Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF005460),Color(0xFF00434F)))).padding(12.dp),verticalAlignment=Alignment.Top) {
                Box(Modifier.padding(top=2.dp).size(40.dp).background(Color.White.copy(alpha=.12f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Filled.AccountBalanceWallet,null,Modifier.size(26.dp),tint=Color.White)}
                Spacer(Modifier.width(12.dp));Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    if(rows.isEmpty()) UiText("No shifts added",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    totals(rows).forEach {(c,a)->UiText(displayMoney(a,c),fontSize=33.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                    UiText("${hours(rows.sumOf {it.paidMinutes})} scheduled ${if(period=="Week") "this week" else if(period=="Pay period") "in period" else "this month"}",fontSize=13.sp,color=Color.White)
                    UiText("Based on your pay rules",fontSize=11.sp,color=Color(0xFFB5D7DB))
                }
            }
        }
        WorkAnalyticsCard(openAnalytics,LocalPremium.current)
        data.jobs.forEach {job->
            val jobRows=rows.filter {it.jobId==job.id}
            if(jobRows.isNotEmpty()) EarningsBreakdown(job,jobRows)
        }
        Row(verticalAlignment=Alignment.Top) {Icon(Icons.Filled.Info,null,Modifier.size(15.dp),tint=Color(0xFF71859D));Spacer(Modifier.width(8.dp));UiText("Hourly prices applied. Unpaid breaks are deducted.",fontSize=10.sp,lineHeight=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(payments.isNotEmpty()) {
            SectionLabel("Next payments")
            Surface(shape=Round,color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Column {payments.forEachIndexed {index,j->
                    if(index>0) HorizontalDivider(Modifier.padding(horizontal=12.dp),thickness=.5.dp,color=MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().heightIn(min=43.dp).clickable {paymentDetails=j}.padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                        JobIcon(j,34.dp);Spacer(Modifier.width(9.dp));androidx.compose.material3.Text(j.name,Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Column(Modifier.weight(1.35f)) {UiText(nextPayday(j,today).format(DateTimeFormatter.ofPattern("MMM d",uiLocale())),fontSize=12.sp,fontWeight=FontWeight.Bold);UiText("${j.payCycle} pay",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }}
            }
        }
        Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(4.dp)) {listOf("Week","Month","Pay period").forEach {value->
                Box(Modifier.weight(1f).heightIn(min=30.dp).clip(RoundedCornerShape(10.dp)).background(if(period==value) Teal.copy(alpha=.17f) else Color.Transparent).clickable {period=value}.padding(vertical=6.dp),contentAlignment=Alignment.Center) {UiText(value,fontSize=11.sp,color=if(period==value) Color(0xFF005361) else MaterialTheme.colorScheme.onSurfaceVariant)}
            }}
        }
    }
    if(picker) ReportMonthDialog(month,{onMonthChange(it.toString());period="Month";picker=false},{picker=false})
    paymentDetails?.let {job->PaymentDetailsDialog(job,today){paymentDetails=null}}
}

@Composable private fun ReportMonthDialog(selected:YearMonth,onSelect:(YearMonth)->Unit,onClose:()->Unit) {
    val locale=uiLocale()
    var year by rememberSaveable {mutableStateOf(selected.year)}
    AlertDialog(onDismissRequest=onClose,containerColor=MaterialTheme.colorScheme.surface,
        title={ScreenHeading("Choose month",Icons.Outlined.CalendarMonth,"Choose an earnings period",compact=true)},
        text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={year--},enabled=year>1){Icon(Icons.Outlined.ChevronLeft,translate("Previous year",LocalLanguage.current))}
                UiText(year.toString(),Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick={year++},enabled=year<9999){Icon(Icons.Outlined.ChevronRight,translate("Next year",LocalLanguage.current))}
            }
            Column(Modifier.heightIn(max=340.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                monthsForYear(year).chunked(2).forEach {pair->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    pair.forEach {month->Surface(onClick={onSelect(month)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(7.dp),
                        color=if(month==selected) MaterialTheme.colorScheme.primary.copy(alpha=.13f) else MaterialTheme.colorScheme.surface,
                        border=BorderStroke(.7.dp,if(month==selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        UiText(month.format(DateTimeFormatter.ofPattern("MMMM",locale)),Modifier.semantics {contentDescription=month.format(DateTimeFormatter.ofPattern("MMMM yyyy",locale))}.padding(horizontal=8.dp,vertical=12.dp),fontSize=12.sp,
                            fontWeight=if(month==selected) FontWeight.Bold else FontWeight.Normal,color=if(month==selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }}
                }}
            }
        }},confirmButton={TextButton(onClick=onClose){Icon(Icons.Outlined.Close,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("Close",fontSize=12.sp)}})
}

@Composable private fun PaymentDetailsDialog(job:Job,today:LocalDate,onClose:()->Unit) {
    val payday=nextPayday(job,today);val (from,to)=payPeriod(job,payday)
    val dateFormat=DateTimeFormatter.ofPattern("d MMM yyyy",uiLocale())
    AlertDialog(onDismissRequest=onClose,containerColor=MaterialTheme.colorScheme.surface,
        title={ScreenHeading("Payment details",Icons.Outlined.Payments,"Payday and covered dates",compact=true)},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                JobIcon(job,36.dp);Spacer(Modifier.width(9.dp));Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(job.name,fontSize=15.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
                    UiText("${job.payCycle} pay",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                }
            }
            Panel(color=MaterialTheme.colorScheme.primary.copy(alpha=.08f)) {
                UiText("Next payday",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                UiText(payday.format(dateFormat),fontSize=17.sp,fontWeight=FontWeight.Bold)
            }
            UiText("Pay period",fontSize=11.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Medium)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {UiText("From",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);UiText(from.format(dateFormat),fontSize=12.sp,fontWeight=FontWeight.Medium)}
                Column(Modifier.weight(1f)) {UiText("To",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);UiText(to.format(dateFormat),fontSize=12.sp,fontWeight=FontWeight.Medium)}
            }
        }},confirmButton={Button(onClick=onClose,contentPadding=PaddingValues(horizontal=14.dp,vertical=7.dp)) {Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("OK",fontSize=12.sp)}})
}
@Composable private fun EarningsBreakdown(job: Job,rows: List<Shift>) {
    var expanded by rememberSaveable(job.id) {mutableStateOf(true)}
    Panel {
        Row(Modifier.fillMaxWidth().clickable {expanded=!expanded},verticalAlignment=Alignment.CenterVertically) {
            JobIcon(job,42.dp);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                androidx.compose.material3.Text(job.name,lineHeight=15.sp,fontSize=12.sp,fontWeight=FontWeight.Medium)
                UiText("${hours(rows.sumOf {it.paidMinutes})} · ${totals(rows).map {(c,a)->displayMoney(a,c)}.joinToString(" / ")}",fontSize=15.sp,fontWeight=FontWeight.Medium)
            };Icon(if(expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,"${if(expanded) "Collapse" else "Expand"} ${job.name}",Modifier.size(18.dp))
        }
        if(expanded) Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                rows.filter {it.fixedPay || !it.rules.useHourlyRates}.groupBy {Triple(it.currency,it.rate,it.fixedPay)}.forEach {(rule,group)->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        UiText(if(rule.third) "Fixed shifts" else "Regular base",Modifier.weight(1f),fontSize=10.sp)
                        UiText("${if(rule.third) group.count {it.kind=="Work"}.toString() else hours(group.sumOf {it.paidMinutes})} × ${displayMoney(java.math.BigDecimal(rule.second),rule.first)}",Modifier.weight(1f),fontSize=10.sp)
                        UiText(displayMoney(group.fold(java.math.BigDecimal.ZERO){a,s->a+s.baseEarnings()},rule.first),fontSize=11.sp)
                    }
                }
                rows.flatMap {it.hourlyLines()}.groupBy {Triple(it.label,it.currency,it.rate)}.forEach {(key,lines)->
                    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            UiText(key.first,Modifier.weight(1f),fontSize=11.sp)
                            UiText(displayMoney(lines.fold(java.math.BigDecimal.ZERO){a,line->a+line.amount},key.second),fontSize=11.sp,fontWeight=FontWeight.Medium)
                        }
                        UiText("${decimalHours(lines.sumOf {it.minutes})} × ${displayMoney(java.math.BigDecimal(key.third),key.second)}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                rows.flatMap {s->s.additions().map {(label,amount)->Triple(label,s.currency,amount)}}.groupBy {it.first to it.second}.forEach {(key,values)->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {UiText(key.first,fontSize=11.sp);UiText(displayMoney(values.fold(java.math.BigDecimal.ZERO){a,v->a+v.third},key.second),fontSize=11.sp)}
                }
                HorizontalDivider(thickness=.5.dp,color=MaterialTheme.colorScheme.outlineVariant)
                totals(rows).forEach {(c,a)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {UiText("Total",fontSize=11.sp,fontWeight=FontWeight.Bold);UiText(displayMoney(a,c),fontSize=11.sp,fontWeight=FontWeight.Bold)}}
            }
        }
    }
}
@Composable private fun jobPayLabel(job: Job): String = when(job.payCycle) {
    "Monthly" -> "Monthly · Payday ${LocalDate.parse(job.paydayAnchor).dayOfMonth}"
    "Weekly" -> "Weekly · ${LocalDate.parse(job.paydayAnchor).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL,uiLocale())}"
    "Biweekly" -> "Biweekly · ${LocalDate.parse(job.paydayAnchor).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL,uiLocale())}"
    else -> "Custom payday"
}
@Composable private fun JobsScreen(data: AppData,edit: (Job)->Unit,add: ()->Unit,info: (String)->Unit,patterns:()->Unit,premium:()->Unit) {
    Stack {
        Heading("Your jobs","Separate pay rules for every job.")
        Spacer(Modifier.height(2.dp))
        data.jobs.filterNot {it.archived}.forEach {job->StripedCard(Color(job.color),{edit(job)}) {
            JobIcon(job,42.dp);Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                androidx.compose.material3.Text(job.name,lineHeight=19.sp,fontSize=15.sp,fontWeight=FontWeight.Bold)
                UiText("${displayMoney(java.math.BigDecimal(job.rate),job.currency)} / ${if(job.fixedPay) "shift" else "hour"}",fontSize=16.sp,fontWeight=FontWeight.Medium)
                UiText(jobPayLabel(job),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            };Icon(Icons.Outlined.ChevronRight,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        Box(Modifier.fillMaxWidth().height(44.dp).clip(Round).drawBehind {
            drawRoundRect(color=Color(0xFF8EBFC3),cornerRadius=CornerRadius(9.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx()))))
        }.clickable(onClick=add),contentAlignment=Alignment.Center) {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(Icons.Outlined.Add,null,Modifier.size(22.dp),tint=Color(0xFF00535B));UiText("Add job",fontSize=15.sp,fontWeight=FontWeight.Medium,color=Color(0xFF00535B))}}
        if(data.jobs.any {it.archived}) {
            SectionLabel("Archived jobs")
            data.jobs.filter {it.archived}.forEach {j->TextButton(onClick={edit(j)}){androidx.compose.material3.Text(j.name)}}
        }
        Spacer(Modifier.height(1.dp))
        Panel(Modifier.clickable {patterns()}) {
            Row(verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Filled.Autorenew,null,Modifier.size(32.dp),tint=Color(0xFF00565B));Spacer(Modifier.width(15.dp));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {UiText("Shift patterns",fontSize=14.sp,fontWeight=FontWeight.Bold);UiText("Create your repeating schedule",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Outlined.ChevronRight,null,Modifier.size(20.dp))}
        }
        Surface(shape=Round,color=Color(0xFFFFF0DC),border=BorderStroke(.7.dp,Color(0xFFFFDDB0))) {
            Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.Top) {
                Box(Modifier.size(40.dp).background(Color(0xFFFFE3BA),CircleShape),contentAlignment=Alignment.Center){UiText("☕",fontSize=28.sp)}
                Spacer(Modifier.width(10.dp));Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    UiText("Buy us a coffee ☕",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Navy)
                    UiText("$1.99 / year",fontSize=15.sp,fontWeight=FontWeight.Medium,color=Navy)
                    UiText("No ads + automatic backup",fontSize=11.sp,color=Color(0xFF33425D))
                    UiText("Explore Premium →",Modifier.clickable {premium()}.padding(vertical=3.dp),fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF0087FF))
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Outlined.Storage,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(8.dp));UiText("Local storage · No login required",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable private fun SettingsScreen(p: Preferences, change: (Preferences)->Unit,premium:()->Unit,isPremium:Boolean) {
    var currencyOpen by remember {mutableStateOf(false)}
    val context=LocalContext.current
    Stack {
        Heading("Settings")
        FormSection("Display") {
            FormPair(first={SelectionField("Language",languageNames[p.language] ?: "English",languageNames.values.toList()){choice->change(p.copy(language=languageNames.entries.first {it.value==choice}.key))}},
                second={SelectionField("Appearance",p.appearance,listOf("Light","Dark","System")){change(p.copy(appearance=it))}})
            FormPair(first={CompactChoice("Default currency",p.currency,icon={Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(18.dp))}){currencyOpen=true}},
                second={Column {CompactToggle("24-hour time",p.time24){change(p.copy(time24=it))}}})
            CompactToggle("Start week on Monday",p.mondayFirst){change(p.copy(mondayFirst=it))}
            UiText("Used for new jobs. Each job keeps its own currency.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FormSection("Calendar warnings") {GapSetting(p,change)}
        FormSection("Reminders") {ReminderSetting(p,change)}
        FormSection("Premium & backup") {
            TextButton(onClick=premium){Icon(Icons.Outlined.PersonOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));UiText("Account & Premium",fontSize=12.sp)}
            UiText(if(isPremium) "Premium active" else "Free · Local storage",fontSize=11.sp)
        }
        FormSection("Ad privacy") {
            TextButton(onClick={com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(context as android.app.Activity){}}){UiText("Privacy choices",fontSize=11.sp)}
        }
        FormSection("Android widget") {UiText("Add a widget from your phone home screen.",fontSize=11.sp)}
        FormSection("PTS · Pay Time Shift") {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                UiText("0.2.0 · PTS Premium",Modifier.weight(1f),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://bokimmkd.github.io/paytimeshift-android/")))}) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(14.dp));Spacer(Modifier.width(4.dp));UiText("Website",fontSize=11.sp)
                }
            }
            TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://bokimmkd.github.io/paytimeshift-android/privacy.html")))}) {
                Icon(Icons.Outlined.PrivacyTip,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));UiText("Privacy policy",fontSize=12.sp)
            }
        }
    }
    if(currencyOpen) CurrencyDialog(p.currency,{currencyOpen=false}) {change(p.copy(currency=it));currencyOpen=false}
}
@Composable private fun CurrencyDialog(selected: String, close: ()->Unit, choose: (String)->Unit) {
    val currencies=remember {currencyCatalog()};var query by remember {mutableStateOf("")}
    val matching=currencies.filter {it.currencyCode.contains(query,true) || it.getDisplayName(uiLocale()).contains(query,true) || it.getDisplayName(Locale.getDefault()).contains(query,true)}
    AlertDialog(onDismissRequest=close,title={UiText("Choose currency")},text={Column {
        OutlinedTextField(query,{query=it},label={UiText("Search code or currency name")},singleLine=true,modifier=Modifier.fillMaxWidth())
        UiText("${matching.size} currencies",fontSize=12.sp,modifier=Modifier.padding(vertical=8.dp))
        LazyColumn(Modifier.heightIn(max=380.dp)) {items(matching,key={it.currencyCode}) {c->
            Row(Modifier.fillMaxWidth().clickable {choose(c.currencyCode)}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){UiText(c.currencyCode,fontWeight=FontWeight.Bold);UiText(c.getDisplayName(uiLocale()),fontSize=13.sp)}
                if(c.currencyCode==selected) Icon(Icons.Outlined.Check,null,tint=MaterialTheme.colorScheme.primary)
            }
        }}
    }},confirmButton={TextButton(onClick=close){UiText("Close")}})
}
@Composable fun DateControl(label: String, date: String, change: (String)->Unit) {
    val context=LocalContext.current;val value=LocalDate.parse(date)
    CompactChoice(label,value.format(DateTimeFormatter.ofPattern("d MMM yyyy",uiLocale())),
        icon={Icon(Icons.Outlined.CalendarMonth,null,Modifier.size(17.dp))}) {
        DatePickerDialog(context,{_,y,m,d->change(LocalDate.of(y,m+1,d).toString())},value.year,value.monthValue-1,value.dayOfMonth).show()
    }
}
@Composable fun TimeControl(label: String, time: String, modifier: Modifier=Modifier, change: (String)->Unit) {
    val context=LocalContext.current;val value=LocalTime.parse(time)
    CompactChoice(label,time,modifier,icon={Icon(Icons.Outlined.Schedule,null,Modifier.size(17.dp))}) {
        TimePickerDialog(context,{_,h,m->change(LocalTime.of(h,m).toString())},value.hour,value.minute,true).show()
    }
}
@Composable private fun JobDialog(original: Job?, defaultCurrency: String, onClose: ()->Unit, save: (Job,Boolean)->Unit) {
    val initialRules=original?.hourlyRules() ?: PayRules(useHourlyRates=true)
    var name by remember {mutableStateOf(original?.name ?: "")};var rate by remember {mutableStateOf(original?.rate ?: "")}
    var currency by remember {mutableStateOf(original?.currency ?: defaultCurrency)};var currencyOpen by remember {mutableStateOf(false)}
    var fixed by remember {mutableStateOf(original?.fixedPay ?: false)};var cycle by remember {mutableStateOf(original?.payCycle ?: "Monthly")}
    var anchor by remember {mutableStateOf(original?.paydayAnchor ?: LocalDate.now().withDayOfMonth(15).toString())}
    var color by remember {mutableStateOf(original?.color ?: 0xFF2488FF)}
    var shiftStart by remember {mutableStateOf(original?.defaultStart ?: "07:00")};var shiftEnd by remember {mutableStateOf(original?.defaultEnd ?: "15:00")}
    var pause by remember {mutableStateOf((original?.defaultBreakMinutes ?: 0).toString())};var paid by remember {mutableStateOf(original?.paidBreak ?: false)}
    var reminder by remember {mutableStateOf((original?.reminderMinutes?.takeIf {it>=0} ?: 30).toString())}
    var ownReminder by remember {mutableStateOf((original?.reminderMinutes ?: -1)>=0)}
    var rest by remember {mutableStateOf((original?.minGapHours?.takeIf {it>=0} ?: 8.0).toString())};var ownRest by remember {mutableStateOf((original?.minGapHours ?: -1.0)>=0)}
    var overtimeAfter by remember {mutableStateOf(initialRules.overtimeAfterHours.toString().removeSuffix(".0"))}
    var overtime by remember {mutableStateOf(initialRules.overtimeRate)};var saturday by remember {mutableStateOf(initialRules.saturdayRate)}
    var sunday by remember {mutableStateOf(initialRules.sundayRate)};var holiday by remember {mutableStateOf(initialRules.holidayRate)}
    var night by remember {mutableStateOf(initialRules.nightPercent.toString().removeSuffix(".0"))}
    var nightStart by remember {mutableStateOf(initialRules.nightStart)};var nightEnd by remember {mutableStateOf(initialRules.nightEnd)}
    var archived by remember {mutableStateOf(original?.archived ?: false)};var applyUpcoming by remember {mutableStateOf(true)}
    var costs by remember {mutableStateOf(original?.costs ?: emptyList<JobCost>())}
    var error by remember {mutableStateOf<String?>(null)}
    BrandedEditor(onDismissRequest=onClose,error=error,title={ScreenHeading(if(original==null) "Add job" else "Edit job", Icons.Outlined.WorkOutline, "Pay and shift rules", compact=true)},
        text={LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item {FormSection("Job & pay") {
            CompactField("Job name",name,{name=it})
            FormPair(first={if(fixed) NumberField("Amount per shift",rate){rate=it} else RateField("Regular hourly rate",rate,currency){rate=it}},
                second={CompactChoice("Currency",currency,icon={Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(18.dp))}){currencyOpen=true}})
            CompactToggle("Fixed amount per shift",fixed){fixed=it}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(0xFF2488FF,0xFFFFA000,0xFFA040C5,0xFF00857C,0xFFDE5353).forEach {c->
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).clickable {color=c},contentAlignment=Alignment.Center){if(color==c) Icon(Icons.Outlined.Check,translate("Selected color",LocalLanguage.current),Modifier.size(19.dp),tint=Color.White)}
            }}
        }}
        if(!fixed) item {FormSection("Hourly prices") {
            FormPair(first={NumberField("Overtime after (paid hours)",overtimeAfter){overtimeAfter=it}},
                second={RateField("Overtime hourly rate",overtime,currency){overtime=it}})
            FormPair(first={RateField("Saturday hourly rate",saturday,currency){saturday=it}},
                second={RateField("Sunday hourly rate",sunday,currency){sunday=it}})
            FormPair(first={RateField("Holiday hourly rate",holiday,currency){holiday=it}},
                second={Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    UiText("Blank = regular price",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    UiText("Holiday: Calendar → date → ⋮",fontSize=10.sp,color=MaterialTheme.colorScheme.primary)
                }})
            UiText("Holidays replace weekend prices. Overtime uses the higher price.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        item {FormSection("Payments") {
            FormPair(first={SelectionField("Payday frequency",cycle,listOf("Monthly","Weekly","Biweekly","Custom")){cycle=it}},
                second={DateControl(if(cycle=="Monthly") "Day of month" else "Next / anchor payday",anchor){anchor=it}})
        }}
        item {FormSection("Default shift & break") {
            FormPair(first={TimeControl("Starts",shiftStart){shiftStart=it}},second={TimeControl("Ends",shiftEnd){shiftEnd=it}})
            FormPair(first={NumberField("Break (minutes)",pause){pause=it}},second={CompactToggle("Paid break",paid){paid=it}})
            FormPair(first={Column {
                CompactToggle("Use global rest",!ownRest){ownRest=!it}
                if(ownRest) NumberField("Rest (hours, e.g. 1.5)",rest){rest=it}
            }},second={Column {
                CompactToggle("Use global reminder",!ownReminder){ownReminder=!it}
                if(ownReminder) NumberField("Reminder (minutes)",reminder){reminder=it}
            }})
        }}
        if(!fixed) item {FormSection("Night work") {
            NumberField("Night addition (%)",night){night=it}
            FormPair(first={TimeControl("Night starts",nightStart){nightStart=it}},second={TimeControl("Night ends",nightEnd){nightEnd=it}})
            UiText("Night additions use the regular hourly price.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        item {JobCostsEditor(costs,currency){costs=it}}
        if(original!=null) item {FormSection("Existing shifts") {
            CompactToggle("Apply prices to today's and future shifts",applyUpcoming){applyUpcoming=it}
            UiText("Past shifts keep their saved prices. Shift times and breaks stay as entered.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            CompactToggle(if(archived) "Restore job" else "Archive job",archived){archived=it}
        }}
    }},confirmButton={Button(contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp),onClick={
        val amount=rate.replace(',','.').toBigDecimalOrNull()
        val prices=listOf(overtime,saturday,sunday,holiday).map {it.trim().replace(',','.')}
        val hoursValue=overtimeAfter.replace(',','.').toDoubleOrNull();val nightValue=night.replace(',','.').toDoubleOrNull()
        val b=pause.toIntOrNull();val r=if(ownReminder) reminder.toIntOrNull() else -1;val gap=if(!ownRest) -1.0 else rest.replace(',','.').toDoubleOrNull()
        val duration=java.time.Duration.between(LocalTime.parse(shiftStart),LocalTime.parse(shiftEnd)).toMinutes().let {if(it<=0) it+1440 else it}
        when {
            name.isBlank() || amount==null || amount.signum()<0 -> error="Enter a job name and a valid rate (0 or more)."
            prices.any {it.isNotBlank() && (it.toBigDecimalOrNull()?.signum() ?: -1)<0} || hoursValue==null || !hoursValue.isFinite() || hoursValue !in 0.0..168.0 || nightValue==null || !nightValue.isFinite() || nightValue !in 0.0..10000.0 || b==null || b<0 || b>=duration || r==null || r !in -1..10080 || (ownReminder && r<0) || gap==null || !gap.isFinite() || (gap!=-1.0 && gap !in 0.0..168.0) || (ownRest && gap<0) || shiftStart==shiftEnd -> error="Numbers must be valid and non-negative."
            costs.any {it.amount.replace(',','.').toBigDecimalOrNull()?.signum()?.let {n->n<0} != false} -> error="Enter valid cost amounts (0 or more)."
            else -> save(Job(original?.id ?: java.util.UUID.randomUUID().toString(),name.trim(),color,currency,amount.toPlainString(),fixed,cycle,anchor,
                PayRules(overtimeAfterHours=hoursValue,nightPercent=nightValue,nightStart=nightStart,nightEnd=nightEnd,useHourlyRates=true,
                    overtimeRate=prices[0],saturdayRate=prices[1],sundayRate=prices[2],holidayRate=prices[3]),archived,shiftStart,shiftEnd,b,paid,r,gap,costs.map {it.copy(amount=it.amount.replace(',','.'))}),original!=null && applyUpcoming)
        }
    }){Icon(Icons.Outlined.Save,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("Save job",fontSize=12.sp)}},dismissButton={TextButton(onClick=onClose,contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)){Icon(Icons.Outlined.Close,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText("Cancel",fontSize=12.sp)}})
    if(currencyOpen) CurrencyDialog(currency,{currencyOpen=false}){currency=it;currencyOpen=false}
}
@Composable private fun ShiftDialog(original: Shift?,data: AppData,initialDate:String,onClose: ()->Unit,onDelete: (String)->Unit,save: (Shift)->Unit) {
    var jobId by remember {mutableStateOf(original?.jobId ?: data.jobs.first { !it.archived }.id)}
    var date by remember {mutableStateOf(original?.date ?: initialDate)}
    val initialJob=data.jobs.first {it.id==jobId}
    var start by remember {mutableStateOf(original?.start ?: initialJob.defaultStart)};var end by remember {mutableStateOf(original?.end ?: initialJob.defaultEnd)}
    var breakText by remember {mutableStateOf((original?.breakMinutes ?: initialJob.defaultBreakMinutes).toString())};var note by remember {mutableStateOf(original?.note ?: "")}
    var error by remember {mutableStateOf<String?>(null)};var deleting by remember {mutableStateOf(false)}
    val job=data.jobs.first {it.id==jobId}
    var paid by remember {mutableStateOf(original?.paidBreak ?: initialJob.paidBreak)}
    var bonus by remember {mutableStateOf(original?.bonus ?: "0")}
    var kind by remember {mutableStateOf(original?.kind ?: "Work")}
    var currentPrices by remember {mutableStateOf(original==null)}
    val keepPrices=original?.jobId==jobId && !currentPrices
    BrandedEditor(onDismissRequest=onClose,error=error,heightFraction=.82f,title={ScreenHeading(if(original==null) "Add shift" else "Edit shift", Icons.Outlined.Schedule, "Times, breaks and earnings", compact=true)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        JobPicker(data.jobs.filterNot {it.archived && it.id!=original?.jobId},jobId){id->
            val selectedJob=data.jobs.first {it.id==id}
            jobId=id;start=selectedJob.defaultStart;end=selectedJob.defaultEnd;breakText=selectedJob.defaultBreakMinutes.toString();paid=selectedJob.paidBreak;currentPrices=true
        }
        FormPair(first={DateControl("Date",date){date=it}},second={SelectionField("Day type",kind,listOf("Work","Off","Vacation","Sick")){kind=it}})
        if(kind!="Work") UiText("Days off, vacation and sick leave have no estimated pay.",fontSize=10.sp)
        if(date in data.holidays) UiText("Holiday",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
        FormPair(first={TimeControl("Starts",start){start=it}},second={TimeControl("Ends",end){end=it}})
        if(LocalTime.parse(end)<LocalTime.parse(start)) UiText("Ends the following day",fontSize=10.sp)
        FormPair(first={CompactField("Break (minutes)",breakText,{breakText=it},keyboardType=androidx.compose.ui.text.input.KeyboardType.Number)},
            second={NumberField("Bonus amount",bonus){bonus=it}})
        FormPair(first={CompactToggle("Paid break",paid){paid=it}},second={if(original!=null) CompactToggle("Current job prices",currentPrices){currentPrices=it}})
        val preview=runCatching {
            val minutes=breakText.toInt();require(minutes>=0)
            Shift(jobId=jobId,date=date,start=start,end=end,breakMinutes=minutes,rate=if(keepPrices) original!!.rate else job.rate,currency=if(keepPrices) original!!.currency else job.currency,
                fixedPay=if(keepPrices) original!!.fixedPay else job.fixedPay,rules=if(keepPrices) original!!.rules else job.hourlyRules(),bonus=bonus.replace(',','.').toBigDecimal().toPlainString(),kind=kind,paidBreak=paid)
        }.getOrNull()
        preview?.let {s->Surface(shape=Round,color=MaterialTheme.colorScheme.primary.copy(alpha=.10f)){
            Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AccountBalanceWallet,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)){UiText("Estimated daily earnings",fontSize=10.sp);UiText(displayMoney(s.earnings(),s.currency),fontSize=18.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
                UiText("${hours(s.paidMinutes)} paid",fontSize=10.sp)
            }
        }}
        CompactField("Note (optional)",note,{note=it},singleLine=false)
        if(original!=null) TextButton(onClick={deleting=true},contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
            Icon(Icons.Outlined.DeleteOutline,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(5.dp))
            UiText("Delete shift",fontSize=11.sp,color=MaterialTheme.colorScheme.error)
        }
    }},confirmButton={Button(contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp),onClick={
        val pause=breakText.toIntOrNull()
        val candidate=Shift(original?.id ?: java.util.UUID.randomUUID().toString(),jobId,date,start,end,pause ?: 0,note,
            if(keepPrices) original!!.rate else job.rate,if(keepPrices) original!!.currency else job.currency,if(keepPrices) original!!.fixedPay else job.fixedPay,
            if(keepPrices) original!!.rules else job.hourlyRules(),bonus.replace(',','.').toBigDecimalOrNull()?.toPlainString() ?: "0",kind,paid)
        when {
            bonus.replace(',','.').toBigDecimalOrNull()?.let {it.signum()<0} != false -> error="Numbers must be valid and non-negative."
            start==end -> error="Start and end must differ."
            pause==null || pause<0 || pause>=java.time.Duration.between(candidate.begins,candidate.finishes).toMinutes() -> error="Break must be shorter than the shift."
            data.shifts.any {it.id!=candidate.id && it.jobId==jobId && it.date==date && it.start==start && it.end==end} -> error="This shift already exists."
            else -> save(candidate)
        }
    }){Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("Save shift",fontSize=12.sp)}},dismissButton={TextButton(onClick=onClose,contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)){Icon(Icons.Outlined.Close,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText("Cancel",fontSize=12.sp)}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={UiText("Delete this shift?")},text={UiText("This removes the shift from your calendar and earnings.")},confirmButton={TextButton(onClick={onDelete(original!!.id)}){UiText("Delete")}},dismissButton={TextButton(onClick={deleting=false}){UiText("Cancel")}})
}
