package com.paytimeshift.pts.ui

import androidx.compose.foundation.layout.*
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

@Composable fun JobCostsEditor(costs: List<JobCost>,currency: String,change: (List<JobCost>)->Unit) {
    var open by remember {mutableStateOf(false)}
    HintAnchor("Job costs") {show,_ ->
    FormSection("Job costs") {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            UiText("Costs related to this job",Modifier.weight(1f),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick={show();open=!open},modifier=Modifier.size(30.dp)) {Icon(if(open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,"Job costs",Modifier.size(20.dp))}
        }
        if(costs.isNotEmpty() && !open) UiText("${costs.count {it.enabled}} · ${translate("Enabled",LocalLanguage.current)}",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
        if(open) {
            UiText("Optional. Gross pay stays unchanged. Analytics requires Premium.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            costs.forEach {cost->key(cost.id) {
                fun update(next: JobCost)=change(costs.map {if(it.id==cost.id) next else it})
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {CompactToggle("Enabled",cost.enabled){update(cost.copy(enabled=it))}}
                    IconButton(onClick={change(costs.filterNot {it.id==cost.id})},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.DeleteOutline,"Remove cost",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.error)}
                }
                SelectionField("Category",cost.category,costCategories){update(cost.copy(category=it))}
                CompactField("Cost name (optional)",cost.name,{update(cost.copy(name=it.take(120)))})
                FormPair(first={CompactField("Amount",cost.amount,{update(cost.copy(amount=it.take(32)))},androidx.compose.ui.text.input.KeyboardType.Decimal,currency)},
                    second={SelectionField("Frequency",cost.frequency,costFrequencies){update(cost.copy(frequency=it))}})
            }}
            if(costs.size<100) {
                var addMenu by remember {mutableStateOf(false)}
                Box {
                    TextButton(onClick={addMenu=true},contentPadding=PaddingValues(4.dp)) {Icon(Icons.Outlined.Add,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Add cost",fontSize=12.sp,fontWeight=FontWeight.Bold)}
                    DropdownMenu(expanded=addMenu,onDismissRequest={addMenu=false}) {
                        costCategories.forEach {category->DropdownMenuItem(text={UiText(category,fontSize=12.sp)},onClick={change(costs+JobCost(category=category));addMenu=false})}
                        DropdownMenuItem(text={UiText("Add custom cost",fontSize=12.sp)},onClick={change(costs+JobCost(category="Other"));addMenu=false})
                    }
                }
            }
            UiText("Weekly: once per workweek, on its first workday. Monthly: once per working month. Current cost rules apply to report history.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
}
