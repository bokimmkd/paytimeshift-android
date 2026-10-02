package com.paytimeshift.pts.platform

import android.app.*
import android.content.*
import android.os.Build
import com.paytimeshift.pts.MainActivity
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.ui.translate
import java.time.ZoneId
import java.util.concurrent.Executors

private fun alarmIntent(context:Context,id:String)=PendingIntent.getBroadcast(context,id.hashCode(),Intent(context,ReminderReceiver::class.java).apply {data=android.net.Uri.parse("pts://shift/$id");putExtra("shiftId",id)},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
fun syncReminders(context:Context,old:AppData,new:AppData) {
    val manager=context.getSystemService(AlarmManager::class.java)
    old.shifts.filter {s->val j=old.jobs.find {it.id==s.jobId};(j?.reminderMinutes?.takeIf {it>=0} ?: old.preferences.reminderMinutes)>0}.forEach {manager.cancel(alarmIntent(context,it.id))}
    if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) return
    new.shifts.filter {it.kind=="Work"}.sortedBy {it.begins}.mapNotNull {s->
        val job=new.jobs.find {it.id==s.jobId} ?: return@mapNotNull null
        if(job.archived) return@mapNotNull null
        val minutes=if(job.reminderMinutes>=0) job.reminderMinutes else new.preferences.reminderMinutes
        val instant=s.begins.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()-minutes*60000L
        if(minutes<=0 || instant<=System.currentTimeMillis()) null else s to instant
    }.take(30).forEach {(s,time)->manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,alarmIntent(context,s.id))}
}
class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val pending=goAsync()
        Executors.newSingleThreadExecutor().also {executor->executor.execute {
            try {
                val data=LocalStore(context).load()
                if(intent.action!=Intent.ACTION_BOOT_COMPLETED && intent.action!=Intent.ACTION_TIME_CHANGED && intent.action!=Intent.ACTION_TIMEZONE_CHANGED && intent.action!=Intent.ACTION_MY_PACKAGE_REPLACED) {
                    val s=data.shifts.find {it.id==intent.getStringExtra("shiftId")}
                    if(s!=null && s.kind=="Work") {
                        val job=data.jobs.find {it.id==s.jobId}
                        val manager=context.getSystemService(NotificationManager::class.java)
                        manager.createNotificationChannel(NotificationChannel("pts-shifts",translate("Reminders",data.preferences.language),NotificationManager.IMPORTANCE_DEFAULT))
                        if(Build.VERSION.SDK_INT<33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                            val notification=Notification.Builder(context,"pts-shifts").setSmallIcon(com.paytimeshift.pts.R.drawable.ic_pts).setContentTitle("PTS · ${job?.name ?: ""}").setContentText("${translate("Next shift",data.preferences.language)}: ${s.date} ${s.start}–${s.end}").setContentIntent(open).setAutoCancel(true).build()
                            manager.notify(s.id.hashCode(),notification)
                        }
                    }
                }
                syncReminders(context,data,data);refreshWidgets(context)
            } catch(_:Exception) {} finally {pending.finish();executor.shutdown()}
        }}
    }
}
