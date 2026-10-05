package com.paytimeshift.pts.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.paytimeshift.pts.MainActivity
import com.paytimeshift.pts.R
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.ui.localeForLanguage
import com.paytimeshift.pts.ui.translate
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val ROLLOVER = "com.paytimeshift.pts.WIDGET_DAY_CHANGED"
private const val WIDGET_DATE = "widgetDate"
private const val WIDGET_SHIFT = "widgetShift"

data class WidgetTarget(val date: String, val shiftId: String? = null) {
    companion object {
        fun fromIntent(intent: Intent?): WidgetTarget? {
            if (ShortcutAction.fromIntent(intent) != ShortcutAction.Calendar) return null
            val date = intent?.getStringExtra(WIDGET_DATE) ?: return null
            return runCatching { LocalDate.parse(date); WidgetTarget(date, intent?.getStringExtra(WIDGET_SHIFT)) }.getOrNull()
        }
    }
}

fun canActivatePtsWidget(context: Context): Boolean =
    AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

fun activatePtsWidget(context: Context): Boolean = runCatching {
    val manager = AppWidgetManager.getInstance(context)
    manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(
        ComponentName(context, PtsWidget::class.java), null, null)
}.getOrDefault(false)

fun refreshWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    PtsWidget().onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, PtsWidget::class.java)))
}

class PtsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        runCatching {
            val data = LocalStore(context).load()
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val views = if (Build.VERSION.SDK_INT >= 31) {
                    RemoteViews(mapOf(
                        SizeF(240f, 128f) to widgetViews(context, data, 128),
                        SizeF(260f, 250f) to widgetViews(context, data, 250),
                        SizeF(260f, 350f) to widgetViews(context, data, 350)
                    ))
                } else {
                    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
                    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
                    widgetViews(context, data, if (width < 260) 128 else height)
                }
                manager.updateAppWidget(id, views)
            }
            scheduleRollover(context)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(ROLLOVER, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) refreshWidgets(context)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(rolloverIntent(context))
    }
}

private fun rolloverIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context, 2701, Intent(context, PtsWidget::class.java).setAction(ROLLOVER),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

private fun scheduleRollover(context: Context) {
    val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    // No exact-alarm permission. Android may defer this while the device is asleep.
    context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, rolloverIntent(context))
}

internal fun widgetIntent(context: Context, date: LocalDate, shift: Shift? = null): Intent =
    ShortcutAction.Calendar.intent(context).setData(Uri.parse("pts://widget/$date/${shift?.id ?: "day"}"))
        .putExtra(WIDGET_DATE, date.toString()).apply { shift?.let { putExtra(WIDGET_SHIFT, it.id) } }

private fun openDay(context: Context, date: LocalDate, shift: Shift? = null): PendingIntent =
    PendingIntent.getActivity(context, 0, widgetIntent(context, date, shift),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

/** Shared by the real launcher renderer and instrumented rendering checks. */
internal fun widgetViews(context: Context, data: AppData, height: Int, today: LocalDate = LocalDate.now()): RemoteViews {
    val compact = height < 250
    val capacity = if (height >= 350) 2 else 1
    val lang = data.preferences.language
    val locale = localeForLanguage(lang)
    val dark = data.preferences.appearance == "Dark" || (data.preferences.appearance == "System" &&
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    val ink = if (dark) 0xFFF3F7FB.toInt() else 0xFF071C43.toInt()
    val muted = if (dark) 0xFFB7C9D0.toInt() else 0xFF586877.toInt()
    val teal = if (dark) 0xFF73D4C9.toInt() else 0xFF00857C.toInt()
    val views = RemoteViews(context.packageName, if (compact) R.layout.pts_widget_compact else R.layout.pts_widget)
    views.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_background_dark else R.drawable.widget_background)
    views.setImageViewResource(R.id.widget_logo, if (dark) R.drawable.widget_logo_dark else R.drawable.widget_logo)
    views.setTextColor(R.id.widget_brand, ink)
    views.setTextColor(R.id.widget_title, muted)
    views.setTextViewText(R.id.widget_title, translate("Your shifts", lang))
    views.setOnClickPendingIntent(R.id.widget_root, openDay(context, today))
    views.setContentDescription(R.id.widget_root, "PTS · ${translate("Your shifts", lang)}")
    widgetDays(data, today).forEachIndexed { index, day ->
        val panel = if (index == 0) R.id.widget_today else R.id.widget_tomorrow
        val heading = if (index == 0) R.id.widget_today_label else R.id.widget_tomorrow_label
        val body = if (index == 0) R.id.widget_today_rows else R.id.widget_tomorrow_rows
        val more = if (index == 0) R.id.widget_today_more else R.id.widget_tomorrow_more
        val label = translate(if (index == 0) "Today" else "Tomorrow", lang)
        val date = day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        views.setInt(panel, "setBackgroundResource", if (dark) R.drawable.widget_panel_dark else R.drawable.widget_panel)
        views.setTextViewText(heading, if (compact) label else "$label · $date")
        views.setTextColor(heading, teal)
        views.setOnClickPendingIntent(panel, openDay(context, day.date))
        views.removeAllViews(body)
        val displayed = day.shifts.take(capacity)
        val hidden = (day.shifts.size - capacity).coerceAtLeast(0)
        if (displayed.isEmpty()) {
            val row = RemoteViews(context.packageName, R.layout.pts_widget_empty)
            row.setTextViewText(R.id.widget_empty, translate("No shifts added", lang))
            row.setTextColor(R.id.widget_empty, muted)
            views.addView(body, row)
        }
        displayed.forEach { shift ->
            val job = data.jobs.first { it.id == shift.jobId }
            val row = RemoteViews(context.packageName, if (compact) R.layout.pts_widget_row_compact else R.layout.pts_widget_row)
            val timeFormat = DateTimeFormatter.ofPattern(if (data.preferences.time24) "HH:mm" else "h:mm a", locale)
            val time = if (shift.kind == "Work") {
                "${LocalTime.parse(shift.start).format(timeFormat)}–${LocalTime.parse(shift.end).format(timeFormat)}" +
                    if (shift.finishes.toLocalDate() > day.date) " +1" else ""
            } else translate(shift.kind, lang)
            row.setTextViewText(R.id.widget_job, job.name)
            row.setTextViewText(R.id.widget_time, time)
            row.setTextColor(R.id.widget_job, ink)
            row.setTextColor(R.id.widget_time, ink)
            row.setInt(R.id.widget_marker, "setColorFilter", job.color.toInt())
            if (compact) {
                row.setTextViewText(R.id.widget_count, if (hidden > 0) "+$hidden" else "")
                row.setTextColor(R.id.widget_count, teal)
                row.setOnClickPendingIntent(R.id.widget_count, openDay(context, day.date))
            } else {
                row.setTextViewText(R.id.widget_duration, if (shift.kind == "Work") translate(hours(shift.paidMinutes), lang) else "")
                row.setTextColor(R.id.widget_duration, teal)
                row.setViewVisibility(R.id.widget_duration, if (shift.kind == "Work") View.VISIBLE else View.GONE)
            }
            row.setContentDescription(R.id.widget_row, "${job.name}, $time")
            row.setOnClickPendingIntent(R.id.widget_row, openDay(context, day.date, shift))
            views.addView(body, row)
        }
        if (!compact) {
            views.setTextViewText(more, "+$hidden ${translate("more shifts", lang)}")
            views.setTextColor(more, teal)
            views.setViewVisibility(more, if (hidden > 0) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(more, openDay(context, day.date))
        }
    }
    return views
}
