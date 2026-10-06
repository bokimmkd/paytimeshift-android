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
                    val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                        ?.filter { it.width >= 1 && it.height >= 1 }?.distinct()?.take(16)
                    // Use the launcher's actual portrait/landscape sizes, including Samsung grids.
                    if (!sizes.isNullOrEmpty()) RemoteViews(sizes.associateWith {
                        widgetViews(context, data, it.height.toInt(), width = it.width.toInt())
                    }) else RemoteViews(mapOf(
                        SizeF(240f, 128f) to widgetViews(context, data, 128, width = 240),
                        SizeF(260f, 250f) to widgetViews(context, data, 250, width = 260),
                        SizeF(300f, 340f) to widgetViews(context, data, 340, width = 300)
                    ))
                } else {
                    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
                    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
                    widgetViews(context, data, height, width = width)
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
internal fun widgetViews(context: Context, data: AppData, height: Int, today: LocalDate = LocalDate.now(),
    width: Int = 350): RemoteViews {
    val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
    val days = widgetDays(data, today)
    val compact = height < (250 * fontScale).toInt() || width < 260
    val largeHeight = if (days.all { it.shifts.size <= 1 }) 250 else 340
    val large = !compact && height >= (largeHeight * fontScale).toInt() && width >= 300
    val capacity = 2
    val lang = data.preferences.language
    val locale = localeForLanguage(lang)
    val dark = data.preferences.appearance == "Dark" || (data.preferences.appearance == "System" &&
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    val ink = if (dark) 0xFFF3F7FB.toInt() else 0xFF071C43.toInt()
    val muted = if (dark) 0xFFB7C9D0.toInt() else 0xFF586877.toInt()
    val teal = if (dark) 0xFF73D4C9.toInt() else 0xFF00857C.toInt()
    val divider = if (dark) 0xFF304A53.toInt() else 0xFFE2E6EC.toInt()
    val views = RemoteViews(context.packageName, if (compact) R.layout.pts_widget_compact else R.layout.pts_widget)
    views.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_background_dark else R.drawable.widget_background)
    if (large) {
        val inset = (14 * context.resources.displayMetrics.density).toInt()
        views.setViewPadding(R.id.widget_root, inset, inset, inset, inset)
    }
    views.setImageViewResource(R.id.widget_logo, if (dark) R.drawable.widget_logo_dark else R.drawable.widget_logo)
    views.setTextColor(R.id.widget_brand, ink)
    views.setTextColor(R.id.widget_title, muted)
    views.setTextViewText(R.id.widget_title, translate("Your shifts", lang))
    for (id in listOf(R.id.widget_header_divider, R.id.widget_day_divider))
        views.setInt(id, "setColorFilter", divider)
    views.setInt(R.id.widget_calendar, "setColorFilter", muted)
    views.setContentDescription(R.id.widget_calendar, translate("Calendar", lang))
    views.setOnClickPendingIntent(R.id.widget_calendar, openDay(context, today))
    views.setOnClickPendingIntent(R.id.widget_root, openDay(context, today))
    views.setContentDescription(R.id.widget_root, "PTS · ${translate("Your shifts", lang)}")
    widgetDays(data, today).forEachIndexed { index, day ->
        val panel = if (index == 0) R.id.widget_today else R.id.widget_tomorrow
        val heading = if (index == 0) R.id.widget_today_label else R.id.widget_tomorrow_label
        val dateView = if (index == 0) R.id.widget_today_date else R.id.widget_tomorrow_date
        val body = if (index == 0) R.id.widget_today_rows else R.id.widget_tomorrow_rows
        val more = if (index == 0) R.id.widget_today_more else R.id.widget_tomorrow_more
        val label = translate(if (index == 0) "Today" else "Tomorrow", lang)
        val date = day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        views.setTextViewText(heading, label.uppercase(locale))
        views.setTextViewText(dateView, date)
        views.setTextColor(heading, teal)
        views.setTextColor(dateView, muted)
        if (large) {
            views.setTextViewTextSize(heading, android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            views.setTextViewTextSize(dateView, android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        }
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
            val row = RemoteViews(context.packageName, when {
                compact -> R.layout.pts_widget_row_compact
                large -> R.layout.pts_widget_row_large
                else -> R.layout.pts_widget_row
            })
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
            if (!compact) {
                row.setInt(R.id.widget_job_icon, "setColorFilter", ink)
                row.setInt(R.id.widget_job_circle, "setColorFilter", job.color.toInt())
                row.setInt(R.id.widget_badge_background, "setColorFilter", job.color.toInt())
                row.setTextViewText(R.id.widget_duration, if (shift.kind == "Work") translate(hours(shift.paidMinutes), lang) else "")
                row.setTextColor(R.id.widget_duration, ink)
                row.setViewVisibility(R.id.widget_duration, if (shift.kind == "Work") View.VISIBLE else View.GONE)
                row.setViewVisibility(R.id.widget_badge_background, if (shift.kind == "Work") View.VISIBLE else View.GONE)
                row.setViewVisibility(R.id.widget_duration_container, if (shift.kind == "Work") View.VISIBLE else View.GONE)
            }
            row.setContentDescription(R.id.widget_row, "${job.name}, $time")
            row.setOnClickPendingIntent(R.id.widget_row, openDay(context, day.date, shift))
            views.addView(body, row)
        }
        views.setTextViewText(more, "+$hidden")
        views.setContentDescription(more, "+$hidden ${translate("more shifts", lang)}")
        views.setTextColor(more, teal)
        views.setViewVisibility(more, if (hidden > 0) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(more, openDay(context, day.date))
    }
    return views
}
