package com.paytimeshift.pts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.domain.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable internal fun MonthlyAdjustmentsDialog(job: Job, month: YearMonth, current: List<MonthlyAdjustment>,
    close: () -> Unit, save: (List<MonthlyAdjustment>) -> Unit) {
    var rows by remember { mutableStateOf(current.filter {it.jobId==job.id && it.month==month.toString()}) }
    var editing by remember { mutableStateOf<MonthlyAdjustment?>(null) }
    var amount by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun edit(item: MonthlyAdjustment) {
        editing=item;amount=if(item.amount=="0") "" else item.amount
        reason=if(item.type=="Deduction" && item.reason !in deductionReasons) "Custom reason" else item.reason
        custom=if(reason=="Custom reason") item.reason else "";note=item.note;error=null
    }
    BrandedEditor(close, title={ScreenHeading("Monthly adjustments",Icons.Outlined.Tune,
        job.name+" · "+month.format(DateTimeFormatter.ofPattern("MMMM yyyy",uiLocale())),compact=true)},
        error=error,heightFraction=.80f,text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            UiText("Bonuses and deductions affect only this job and month. Absences do not create automatic deductions.",fontSize=11.sp,lineHeight=15.sp)
            FormPair(first={OutlinedButton(onClick={edit(MonthlyAdjustment(jobId=job.id,month=month.toString(),type="Deduction",amount="0",currency=job.currency,reason=deductionReasons.first()))},modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(6.dp)) {Icon(Icons.Outlined.RemoveCircleOutline,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Add deduction",fontSize=11.sp)}},
                second={OutlinedButton(onClick={edit(MonthlyAdjustment(jobId=job.id,month=month.toString(),type="Bonus",amount="0",currency=job.currency,reason=""))},modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(6.dp)) {Icon(Icons.Outlined.AddCircleOutline,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Add monthly bonus",fontSize=11.sp)}})
            editing?.let { item -> FormSection(if(item.type=="Bonus") "Monthly bonus" else "Monthly deduction") {
                CompactField("Amount",amount,{amount=it},keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal,suffix=item.currency)
                if(item.type=="Deduction") {
                    SelectionField("Reason",reason,deductionReasons){reason=it}
                    if(reason=="Custom reason") CompactField("Description",custom,{custom=it.take(120)})
                } else CompactField("Description",reason,{reason=it.take(120)})
                CompactField("Note (optional)",note,{note=it.take(1000)},singleLine=false)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={
                        val value=amount.replace(',','.').toBigDecimalOrNull()
                        val label=if(reason=="Custom reason") custom.trim() else reason.trim()
                        if(value==null || value.signum()<=0 || amount.length>32 || label.isBlank()) error="Enter a positive amount and a reason."
                        else {rows=rows.filterNot {it.id==item.id}+item.copy(amount=value.toPlainString(),reason=label,note=note);editing=null;error=null}
                    },contentPadding=PaddingValues(8.dp)){Icon(Icons.Outlined.Check,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Add / update item",fontSize=11.sp)}
                    TextButton(onClick={editing=null;error=null}){UiText("Cancel",fontSize=11.sp)}
                }
            } }
            rows.forEach { item -> Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(9.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f)) {
                Row(Modifier.fillMaxWidth().padding(start=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(item.type=="Bonus") Icons.Outlined.AddCircleOutline else Icons.Outlined.RemoveCircleOutline,null,Modifier.size(19.dp),tint=if(item.type=="Bonus") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(7.dp));Column(Modifier.weight(1f)) {
                        UiText(item.reason,fontSize=12.sp,fontWeight=FontWeight.Medium)
                        UiText((if(item.type=="Bonus") "+ " else "− ")+displayMoney(item.amount.toBigDecimal(),item.currency),fontSize=12.sp)
                        if(item.note.isNotBlank()) Text(item.note,fontSize=10.sp,maxLines=2)
                    }
                    IconButton(onClick={edit(item)}){Icon(Icons.Outlined.Edit,translate("Edit",LocalLanguage.current),Modifier.size(17.dp))}
                    IconButton(onClick={rows=rows.filterNot {it.id==item.id};if(editing?.id==item.id) editing=null}){Icon(Icons.Outlined.DeleteOutline,translate("Delete",LocalLanguage.current),Modifier.size(17.dp),tint=MaterialTheme.colorScheme.error)}
                }
            } }
            if(rows.isEmpty() && editing==null) UiText("No monthly adjustments",fontSize=12.sp)
        }},confirmButton={Button(onClick={if(editing!=null) error="Add or cancel the item before saving." else save(rows)},contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Icon(Icons.Outlined.Save,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText("Save adjustments",fontSize=12.sp)}},
        dismissButton={TextButton(onClick=close){UiText("Cancel",fontSize=12.sp)}})
}
