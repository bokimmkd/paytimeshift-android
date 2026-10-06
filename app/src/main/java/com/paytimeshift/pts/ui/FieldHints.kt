package com.paytimeshift.pts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.paytimeshift.pts.domain.Preferences

internal val fieldHints = mapOf(
    "Pay basis" to "Choose hourly pay, a fixed amount per shift, or a fixed monthly salary.",
    "Monthly salary" to "Counted once per calendar month, regardless of shifts or month length. Absences only reduce it when you enter a deduction.",
    "Effective from" to "Salary changes apply from this date. Earlier salary periods stay in your reports. Partial periods use calendar days.",
    "Employment ends" to "No monthly salary is estimated after this date. Leave this off while the job continues.",
    "Reference hourly rate" to "Used only for percentage night extras. It is not calculated from your monthly salary.",
    "Overtime hourly rate" to "Enter the price per overtime hour. For monthly salary, enter only the additional hourly payment.",
    "Overtime after (paid hours)" to "Paid hours above this limit in one shift count as overtime.",
    "Saturday hourly rate" to "Price per Saturday hour. For monthly salary, this is an extra payment. Blank adds no extra.",
    "Sunday hourly rate" to "Price per Sunday hour. For monthly salary, this is an extra payment. Blank adds no extra.",
    "Holiday hourly rate" to "Mark holidays in Calendar. Holiday prices replace weekend prices; overtime uses the higher price.",
    "Paid break" to "A paid break counts toward paid hours. An unpaid break is deducted from paid hours.",
    "Break (minutes)" to "Enter break length in minutes. Worked hours exclude breaks; paid hours include paid breaks.",
    "Job costs" to "Optional costs are subtracted after pay and monthly adjustments. They never change saved shift prices.",
    "Rest (hours, e.g. 1.5)" to "Minimum rest between shifts. You can enter decimals, such as 1.5 hours. Shorter rest shows a warning.",
    "Rest between shifts (hours)" to "Minimum rest between shifts. You can enter decimals, such as 1.5 hours. Shorter rest shows a warning.",
    "Day type" to "Off, vacation, sick and non-working day replace only this job's shifts on this date, after confirmation.",
    "Monthly adjustments" to "Add a bonus or deduction for one job and month. These are separate from shift pay and work costs.",
    "Replace existing shifts in this period" to "Review and replace only the selected job's entries in this date range. Other jobs stay unchanged.",
    "Work costs" to "Optional costs are subtracted after pay and monthly adjustments. They never change saved shift prices.",
    "Frequency" to "Per shift counts every shift; per workday counts each date once. Weekly and monthly costs are charged once when you work.",
    "Amount" to "Enter a positive amount in this job's currency. Different currencies are kept separate.",
    "Payday frequency" to "Choose when you receive payment. This does not change the salary month or hourly calculation.",
    "Night addition (%)" to "Additional percentage of the regular or reference hourly rate during the configured night hours."
)

internal class HintState {
    var active by mutableStateOf<Any?>(null)
    var dismissed by mutableStateOf(setOf<String>())
}
internal data class HintContext(val state: HintState, val preferences: Preferences, val change: (Preferences) -> Unit)
internal val LocalHints = staticCompositionLocalOf<HintContext?> { null }

@Composable internal fun HintProvider(preferences: Preferences, change: (Preferences) -> Unit, content: @Composable () -> Unit) {
    val state = remember { HintState() }
    LaunchedEffect(preferences.showHints) { if (!preferences.showHints) state.active = null }
    CompositionLocalProvider(LocalHints provides HintContext(state, preferences, change), content = content)
}

/** One non-focusable bubble: keyboard, pickers and dropdowns retain their normal interaction. */
@Composable internal fun HintAnchor(label: String, content: @Composable (show: () -> Unit, close: () -> Unit) -> Unit) {
    val context = LocalHints.current
    val token = remember { Any() }
    val id = "field:" + label
    val explanation = fieldHints[label]
    fun close() { if (context?.state?.active === token) context.state.active = null }
    val enabled = context != null && explanation != null && context.preferences.showHints &&
        id !in context.preferences.hiddenHintIds && id !in context.state.dismissed
    DisposableEffect(Unit) { onDispose { close() } }
    Box(Modifier.fillMaxWidth()) {
        content({ if (enabled) context!!.state.active = token else context?.state?.let { it.active = null } }, ::close)
        if (enabled && context!!.state.active === token) {
            val position = remember { object : PopupPositionProvider {
                override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                    val margin = 12
                    val left = anchorBounds.left.coerceIn(margin, maxOf(margin, windowSize.width-popupContentSize.width-margin))
                    val top = if (anchorBounds.top >= popupContentSize.height+margin) anchorBounds.top-popupContentSize.height-margin else anchorBounds.bottom+margin
                    return IntOffset(left, top.coerceIn(margin, maxOf(margin, windowSize.height-popupContentSize.height-margin)))
                }
            } }
            Popup(popupPositionProvider = position, onDismissRequest = ::close,
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)) {
                Surface(Modifier.widthIn(max = 300.dp), shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 4.dp, shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=.25f))) {
                    Row(Modifier.padding(start=10.dp,top=4.dp,bottom=6.dp), verticalAlignment=Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, Modifier.padding(top=8.dp).size(16.dp), tint=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(7.dp))
                        UiText(explanation!!, Modifier.weight(1f).padding(top=7.dp), fontSize=11.sp, lineHeight=15.sp, color=MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = {
                            close(); context.state.dismissed += id
                            context.change(context.preferences.copy(hiddenHintIds=(context.preferences.hiddenHintIds+id).distinct()))
                        }, modifier=Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Close, translate("Hide this hint", LocalLanguage.current), Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}
