@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.paytimeshift.pts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.platform.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@Composable fun WorkAnalyticsCard(open: ()->Unit,premium: Boolean) {
    Surface(onClick=open,shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.09f),border=BorderStroke(.7.dp,MaterialTheme.colorScheme.primary.copy(alpha=.2f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Outlined.Insights,null,Modifier.size(27.dp),tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {UiText("Real earnings",fontSize=15.sp,fontWeight=FontWeight.Bold);UiText("Advanced Work Analytics",fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
            Icon(if(premium) Icons.Outlined.ChevronRight else Icons.Outlined.Lock,null,Modifier.size(20.dp),tint=MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable fun AnalyticsDialog(data: AppData,month: YearMonth,close: ()->Unit) {
    var yearly by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf(month)}
    var loading by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val lines=remember(data,selected,yearly) {reportLines(data,selected,yearly)}
    BrandedEditor(onDismissRequest=close,title={ScreenHeading("Work Analytics",Icons.Outlined.Insights,"Gross, costs and real earnings",compact=true)},error=error,
        text={LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={selected=if(yearly) selected.minusYears(1) else selected.minusMonths(1)},enabled=selected.year>1){Icon(Icons.Outlined.ChevronLeft,"Previous period")}
                UiText(if(yearly) selected.year.toString() else selected.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",uiLocale())),Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=14.sp)
                IconButton(onClick={selected=if(yearly) selected.plusYears(1) else selected.plusMonths(1)},enabled=selected.year<9999){Icon(Icons.Outlined.ChevronRight,"Next period")}
            }}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!yearly,onClick={yearly=false},label={UiText("Monthly report",fontSize=11.sp)})
                FilterChip(selected=yearly,onClick={yearly=true},label={UiText("Yearly report",fontSize=11.sp)})
            }}
            items(lines) {line->
                Surface(shape=RoundedCornerShape(8.dp),color=if(line.heading) MaterialTheme.colorScheme.primary.copy(alpha=.08f) else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=if(line.heading) 9.dp else 5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(line.literal) Text(line.label,Modifier.weight(1f),fontSize=if(line.heading) 14.sp else 11.sp,fontWeight=if(line.heading) FontWeight.Bold else FontWeight.Normal)
                        else UiText(line.label,Modifier.weight(1f),fontSize=if(line.heading) 14.sp else 11.sp,fontWeight=if(line.heading) FontWeight.Bold else FontWeight.Normal,color=if(line.heading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if(line.value.isNotEmpty()) Text(line.value,fontSize=12.sp,fontWeight=FontWeight.Medium)
                    }
                }
            }
            item {FormSection("Report export") {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("View","PDF","Share","Print","Email").forEach {action->
                        OutlinedButton(enabled=!loading,onClick={loading=true;scope.launch {
                            try {val file=withContext(Dispatchers.Default){createWorkReportPdf(context,data,selected,yearly)};openWorkReport(context,file,if(action=="PDF") "View" else action,data.preferences.language)}
                            catch(_:Exception) {error="No app could open this report. Please install a PDF viewer or try Share."}
                            finally {loading=false}
                        }},contentPadding=PaddingValues(horizontal=8.dp,vertical=3.dp)) {
                            Icon(when(action){"Print"->Icons.Outlined.Print;"Email"->Icons.Outlined.Email;"Share"->Icons.Outlined.Share;else->Icons.Outlined.PictureAsPdf},null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));UiText(action,fontSize=11.sp)
                        }
                    }
                }
                if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }}
        }},confirmButton={TextButton(onClick=close){UiText("Close",fontSize=12.sp)}},dismissButton={})
}
