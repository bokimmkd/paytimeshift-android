package com.paytimeshift.pts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.R

private val GuideShape = RoundedCornerShape(18.dp)

@Composable private fun GuideBrand() {
    Row(verticalAlignment=Alignment.CenterVertically) {
        BrandLogo()
        Spacer(Modifier.width(10.dp))
        Column {
            UiText("PTS",fontSize=18.sp,fontWeight=FontWeight.Bold)
            UiText("Pay Time Shift",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun HelpGuideScreen(close: () -> Unit, replay: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val topics = listOf(
        GuideTopic("Jobs & pay rules", Icons.Outlined.WorkOutline, listOf(
            "Open Jobs and tap Add job.",
            "Set the job color, currency and pay rules. Save the job.",
            "Edit a job to update its rules. Review whether to apply them to upcoming shifts.")),
        GuideTopic("Shifts & rotations", Icons.Outlined.Repeat, listOf(
            "Tap + Shift in Today or Calendar. Choose the job, date, hours and break.",
            "For a repeating schedule, open Jobs → Shift patterns and choose Rotation.",
            "Set the cycle and date range, review the preview, then save.")),
        GuideTopic("Earnings & costs", Icons.Outlined.BarChart, listOf(
            "Open Earnings and choose a month to see estimated earnings by job.",
            "Add bonuses or deductions to adjust the monthly total.",
            "With Premium, open Real earnings for costs and detailed work analytics.")),
        GuideTopic("Home screen widget", Icons.Outlined.Widgets, listOf(
            "Open Settings → Android widget → Activate widget.",
            "You can also hold an empty area on your home screen, open Widgets and choose PTS.",
            "Tap a shift in the widget to open it in PTS. Resize the widget for a wider view.")),
        GuideTopic("Premium & backup", Icons.Outlined.Cloud, listOf(
            "Open Account & Premium to view Premium or restore a purchase.",
            "For cloud backup, sign in, verify your email and activate Premium.",
            "Free local backups are in Settings → Local backup. Save a file before restoring."))
    )
    BackHandler(onBack=close)
    Scaffold(topBar={
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,translate("Back",LocalLanguage.current)) }
            GuideBrand()
        }
    }) { inset ->
        LazyColumn(Modifier.fillMaxSize().padding(inset),contentPadding=PaddingValues(16.dp,10.dp,16.dp,24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                ScreenHeading("Help & Guide",Icons.Outlined.HelpOutline,"A little guidance. More confidence.")
            }
            item {
                Surface(onClick=replay,shape=GuideShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            UiText("Watch introduction again",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onPrimaryContainer)
                            UiText("Four quick steps to get started",fontSize=12.sp,color=MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            topics.forEach { topic -> item(key=topic.title) {
                val open=expanded==topic.title
                val state=translate(if(open) "Expanded" else "Collapsed",LocalLanguage.current)
                Surface(shape=GuideShape,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),color=MaterialTheme.colorScheme.surface) {
                    Column {
                        Surface(onClick={expanded=if(open) null else topic.title},color=MaterialTheme.colorScheme.surface,
                            modifier=Modifier.fillMaxWidth().semantics { stateDescription=state }) {
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=.1f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                                    Icon(topic.icon,null,Modifier.size(21.dp),tint=MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                UiText(topic.title,modifier=Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                                Icon(if(open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)
                            }
                        }
                        AnimatedVisibility(open) {
                            Column(Modifier.padding(start=16.dp,end=16.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                                topic.steps.forEachIndexed { index, step ->
                                    Row(verticalAlignment=Alignment.Top) {
                                        Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                                            Text("${index+1}",fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        UiText(step,fontSize=13.sp,modifier=Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } }
            item {
                Row(Modifier.padding(horizontal=4.dp,vertical=6.dp),verticalAlignment=Alignment.Top) {
                    Icon(Icons.Outlined.Info,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    UiText("Earnings are estimates based on your saved rules. Restoring a backup replaces local data.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private data class GuideTopic(val title:String,val icon:ImageVector,val steps:List<String>)
private data class IntroPage(val title:String,val description:String,val image:Int)

/** finish(true) is Get started; finish(false) is Skip or closing a replay. */
@Composable fun IntroductionScreen(replay: Boolean, finish: (Boolean) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages=listOf(
        IntroPage("Add your jobs","Keep every job organized with its own color, currency and pay rules.",R.drawable.intro_jobs),
        IntroPage("Track your shifts and earnings","Plan your shifts, track your hours and see your estimated earnings.",R.drawable.intro_shifts),
        IntroPage("See your shifts at a glance","Add the PTS widget to your home screen for a clearer view of your schedule.",R.drawable.intro_widget),
        IntroPage("The full picture with PTS Premium","Explore earnings after costs, detailed reports, no ads and cloud backup.",R.drawable.intro_premium)
    )
    val current=pages[page]
    val pageLabel="${page+1} / ${pages.size}"
    BackHandler { if(page>0) page-- else finish(false) }
    Scaffold(topBar={
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=20.dp,end=8.dp,top=8.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
            GuideBrand()
            Spacer(Modifier.weight(1f))
            TextButton(onClick={finish(false)}) {UiText(if(replay) "Close" else "Skip")}
        }
    },bottomBar={
        Surface(color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.semantics {contentDescription=pageLabel}) {
                    repeat(pages.size) { index ->
                        Box(Modifier.size(if(index==page) 24.dp else 8.dp,8.dp).background(
                            if(index==page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,CircleShape))
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(page>0) OutlinedButton(onClick={page--},modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)) {UiText("Back")}
                    Button(onClick={if(page<pages.lastIndex) page++ else finish(true)},modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)) {
                        UiText(if(page<pages.lastIndex) "Next" else if(replay) "Done" else "Get started",fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(18.dp))
                    }
                }
            }
        }
    }) { inset ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(inset)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Image(painterResource(current.image),null,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxWidth().height((maxHeight*.55f).coerceIn(160.dp,330.dp)))
                UiText(current.title,fontSize=27.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                UiText(current.description,fontSize=15.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                if(page==pages.lastIndex) Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
                    UiText("Premium is optional. Start with the free features.",modifier=Modifier.padding(horizontal=14.dp,vertical=8.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onPrimaryContainer,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
