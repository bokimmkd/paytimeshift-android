package com.paytimeshift.pts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.platform.MetaMeasurement

@Composable fun MeasurementChoice(settings: Boolean = false) {
    val context = LocalContext.current
    val measurement = remember { MetaMeasurement.get(context) }
    var chosen by remember { mutableStateOf(measurement.chosen) }
    var allowed by remember { mutableStateOf(measurement.allowed) }
    fun choose(value: Boolean) {
        measurement.choose(value)
        chosen = true
        allowed = value
    }
    if (!settings && chosen) return
    Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(.7.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UiText("App measurement", fontSize = 14.sp)
            UiText("Allow Meta to measure PTS installs and app opens? Device/app details and network information are shared. Your shifts, pay and account data are not shared.", fontSize = 11.sp)
            UiText("Optional. You can change this in Settings. Advertising ID collection is off.", fontSize = 11.sp)
            if (settings && chosen) CompactToggle("Allow app measurement", allowed) { choose(it) }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { choose(false) }) { UiText("Not now") }
                TextButton(onClick = { choose(true) }) { UiText("Allow") }
            }
        }
    }
}
