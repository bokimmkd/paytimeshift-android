package com.paytimeshift.pts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Compact heading shared by tabs and secondary editors. */
@Composable fun ScreenHeading(title: String, icon: ImageVector, subtitle: String?,
    modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
            Box(Modifier.size(if (compact) 29.dp else 32.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(if (compact) 18.dp else 21.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            UiText(title, fontWeight = FontWeight.Bold, fontSize = if (compact) 18.sp else 22.sp,
                lineHeight = if (compact) 22.sp else 26.sp)
            subtitle?.let { UiText(it, fontSize = 10.sp, lineHeight = 13.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/** The same compact editor shell is used for jobs, shifts, patterns and roster import. */
@Composable fun BrandedEditor(onDismissRequest: () -> Unit, title: @Composable () -> Unit,
    text: @Composable () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit, error: String? = null, heightFraction: Float = .90f) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(heightFraction).imePadding(),
            shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.background) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { title() }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Outlined.Close, translate("Close", LocalLanguage.current), Modifier.size(21.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)) { text() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                error?.let { UiText(it, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    dismissButton(); Spacer(Modifier.width(8.dp)); confirmButton()
                }
            }
        }
    }
}

@Composable fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(.7.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val icon = when(title) {
                "Job & pay" -> Icons.Outlined.WorkOutline
                "Hourly prices", "Payments" -> Icons.Outlined.Payments
                "Default shift & break" -> Icons.Outlined.Schedule
                "Night work" -> Icons.Outlined.DarkMode
                "Existing shifts" -> Icons.Outlined.CalendarMonth
                "Language", "Display" -> Icons.Outlined.Tune
                "Calendar warnings" -> Icons.Outlined.WarningAmber
                "Reminders" -> Icons.Outlined.NotificationsNone
                "Local backup", "Premium & backup" -> Icons.Outlined.Backup
                else -> Icons.Outlined.Settings
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                UiText(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

/** Paired controls stay readable on narrow screens or with large accessibility fonts. */
@Composable fun FormPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 240.dp && fontScale <= 1.3f) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { first(); second() }
    }
}

@Composable fun CompactField(label: String, value: String, change: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text, suffix: String? = null, singleLine: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val translatedLabel = translate(label, LocalLanguage.current)
    BasicTextField(value, change, Modifier.fillMaxWidth().heightIn(min = 54.dp)
        .border(1.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        .padding(horizontal = 9.dp, vertical = 6.dp).semantics { contentDescription = translatedLabel },
        singleLine = singleLine, minLines = 1, maxLines = if (singleLine) 1 else 4,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), interactionSource = interaction,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { input ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                UiText(label, fontSize = 10.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).heightIn(min = 22.dp)) { input() }
                    suffix?.let { UiText(it, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        })
}

@Composable fun CompactChoice(label: String, value: String, modifier: Modifier = Modifier,
    translateValue: Boolean = true, icon: @Composable () -> Unit = {}, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            UiText(label, fontSize = 10.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                if (translateValue) UiText(value, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                else Text(value, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                icon()
            }
        }
    }
}

@Composable fun CompactToggle(label: String, checked: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = change)
        .padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Checkbox(checked, null, Modifier.size(24.dp), enabled = enabled)
        UiText(label, Modifier.weight(1f), fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable fun SelectionField(label: String, selected: String, options: List<String>, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
        Box {
            CompactChoice(label, selected, icon = { Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(18.dp)) }) { open = true }
            DropdownMenu(open, { open = false }) {
                options.forEach { value -> DropdownMenuItem(text = { UiText(value) },
                    onClick = { choose(value); open = false }) }
            }
        }
}

@Composable fun RateField(label: String, value: String, currency: String, change: (String) -> Unit) {
    CompactField(label, value, change, keyboardType = KeyboardType.Decimal, suffix = "$currency / hour")
}
