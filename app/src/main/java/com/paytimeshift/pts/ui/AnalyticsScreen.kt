@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.paytimeshift.pts.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var savedPdf by remember {mutableStateOf<java.io.File?>(null)}
    val savePdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {uri->
        if(uri!=null) scope.launch {try {withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri)?.use {out->savedPdf?.inputStream()?.use {it.copyTo(out)}} ?: error("Could not save")}}
            catch(_:Exception) {error="File could not be saved."}}
    }
    val report=remember(data,selected,yearly) {if(yearly) yearlyAnalytics(data,selected.year) else monthlyAnalytics(data,selected)}
    val lines=remember(data,selected,yearly) {compactReportBlocks(reportLines(data,selected,yearly).filterNot {it.label in listOf("Annual Work & Earnings Report","Monthly Work & Earnings Report","Period","Estimated real earnings after work-related costs.","Taxes and government deductions are not calculated. Only entered adjustments are included.")})}
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
            items(report.currencies) {currency->
                Surface(shape=RoundedCornerShape(12.dp),color=androidx.compose.ui.graphics.Color(0xFF034C57)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Outlined.AccountBalanceWallet,null,tint=androidx.compose.ui.graphics.Color.White,modifier=Modifier.size(22.dp))
                            UiText("Real estimated earnings",color=androidx.compose.ui.graphics.Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        }
                        Text(reportMoney(currency.real,currency.currency,data.preferences.language),fontSize=24.sp,fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {UiText("Gross estimated earnings",fontSize=10.sp,color=androidx.compose.ui.graphics.Color.White.copy(alpha=.75f));Text(reportMoney(currency.gross,currency.currency,data.preferences.language),fontSize=13.sp,color=androidx.compose.ui.graphics.Color.White)}
                            Column(Modifier.weight(1f)) {UiText("Work-related costs",fontSize=10.sp,color=androidx.compose.ui.graphics.Color.White.copy(alpha=.75f));Text("− "+reportMoney(currency.costs,currency.currency,data.preferences.language),fontSize=13.sp,color=androidx.compose.ui.graphics.Color.White)}
                        }
                        UiText("Estimated real earnings after work-related costs.",fontSize=10.sp,color=androidx.compose.ui.graphics.Color.White.copy(alpha=.75f))
                    }
                }
            }
            items(lines) {block->
                if(block.grid) {
                    block.lines.chunked(2).forEach {pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        pair.forEach {line->Surface(Modifier.weight(1f),shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.06f)) {
                            Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {UiText(line.label,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2);Text(line.value,fontSize=14.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
                        }}
                    };Spacer(Modifier.height(6.dp))}
                } else if(block.trend) {
                    block.lines.chunked(8).forEach {pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        pair.chunked(4).forEach {monthLines->Surface(Modifier.weight(1f),shape=RoundedCornerShape(8.dp),border=BorderStroke(.7.dp,MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(monthLines[0].label,fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                                Text(monthLines[0].value,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                monthLines.drop(1).forEach {line->UiText(line.label,fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(line.value,fontSize=11.sp,fontWeight=if(line.label=="Real estimated earnings") FontWeight.Bold else FontWeight.Normal)}
                            }
                        }}
                    };Spacer(Modifier.height(6.dp))}
                } else {
                    val line=block.lines.single()
                    Surface(shape=RoundedCornerShape(8.dp),color=if(line.heading) MaterialTheme.colorScheme.primary.copy(alpha=.08f) else MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=if(line.heading) 9.dp else 5.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(line.literal) Text(line.label,Modifier.weight(1f),fontSize=if(line.heading) 14.sp else 11.sp,fontWeight=if(line.heading) FontWeight.Bold else FontWeight.Normal)
                                else UiText(line.label,Modifier.weight(1f),fontSize=if(line.heading) 14.sp else 11.sp,fontWeight=if(line.heading) FontWeight.Bold else FontWeight.Normal,color=if(line.heading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if(line.value.isNotEmpty()) Text(line.value,Modifier.weight(1f,false),fontSize=12.sp,fontWeight=FontWeight.Medium,textAlign=androidx.compose.ui.text.style.TextAlign.End)
                            }
                            if(line.label in costCategories) {
                                val percent=Regex("([0-9.,]+)%$").find(line.value)?.groupValues?.get(1)?.replace(',','.')?.toFloatOrNull() ?: 0f
                                Spacer(Modifier.height(5.dp));LinearProgressIndicator(progress={percent.coerceIn(0f,100f)/100f},modifier=Modifier.fillMaxWidth().height(3.dp))
                            }
                        }
                    }
                }
            }
            item {UiText("Taxes and government deductions are not calculated. Only entered adjustments are included.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            item {FormSection("Report export") {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("View","PDF","Share","Print","Email").forEach {action->
                        OutlinedButton(enabled=!loading,onClick={loading=true;scope.launch {
                            try {val file=withContext(Dispatchers.Default){createWorkReportPdf(context,data,selected,yearly)};if(action=="PDF") {savedPdf=file;savePdf.launch(file.name)} else openWorkReport(context,file,action,data.preferences.language)}
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

private fun reportMoney(value: java.math.BigDecimal,code: String,language: String): String = code+" "+java.text.NumberFormat.getNumberInstance(localeForLanguage(language)).apply {minimumFractionDigits=0;maximumFractionDigits=java.util.Currency.getInstance(code).defaultFractionDigits.coerceAtLeast(0)}.format(value)

private data class AnalyticsBlock(val lines: List<ReportLine>,val grid: Boolean=false,val trend: Boolean=false)
private fun compactReportBlocks(lines: List<ReportLine>): List<AnalyticsBlock> {
    val result=mutableListOf<AnalyticsBlock>();var i=0
    while(i<lines.size) {
        when {
            lines[i].label=="Total shifts" && i+8<=lines.size->{result+=AnalyticsBlock(lines.subList(i,i+8),grid=true);i+=8}
            lines[i].label=="Monthly trend"->{result+=AnalyticsBlock(listOf(lines[i++]));val start=i;while(i<lines.size && lines[i].label!="Highlights") i++;result+=AnalyticsBlock(lines.subList(start,i),trend=true)}
            else->result+=AnalyticsBlock(listOf(lines[i++]))
        }
    }
    return result
}

