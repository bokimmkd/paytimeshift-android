package com.paytimeshift.pts.platform
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.paytimeshift.pts.MainActivity
import com.paytimeshift.pts.R
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.ui.translate
import java.time.LocalDate

fun refreshWidgets(context:Context) {
    val manager=AppWidgetManager.getInstance(context)
    PtsWidget().onUpdate(context,manager,manager.getAppWidgetIds(ComponentName(context,PtsWidget::class.java)))
}
class PtsWidget:AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {
        if(ids.isEmpty()) return
        try {
            val data=LocalStore(context).load();val lang=data.preferences.language
            val rows=data.shifts.filter {it.date==LocalDate.now().toString()}.sortedBy {it.begins}
            val text=rows.take(3).joinToString("\n") {s->"${data.jobs.find {it.id==s.jobId}?.name ?: ""} · ${if(s.kind=="Work") "${s.start}–${s.end}" else translate(s.kind,lang)}"}.ifEmpty {translate("No shifts added today",lang)}
            ids.forEach {id->
                val views=RemoteViews(context.packageName,R.layout.pts_widget)
                views.setTextViewText(R.id.widget_title,"PTS · ${translate("Today",lang)}")
                views.setTextViewText(R.id.widget_shifts,text)
                views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id,views)
            }
        } catch(_:Exception) {}
    }
}
