package com.paytimeshift.pts.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.paytimeshift.pts.BuildConfig
import com.paytimeshift.pts.domain.UpdateCheckGate
import com.paytimeshift.pts.domain.shouldOfferRestart
import com.paytimeshift.pts.domain.shouldOfferUpdate
import com.paytimeshift.pts.domain.updatePrompt

/** Optional updates: Play supplies availability, with a Store route if flexible flow is unavailable. */
class PlayUpdates(private val activity:ComponentActivity):DefaultLifecycleObserver {
    private val manager=AppUpdateManagerFactory.create(activity)
    private val preferences=activity.getSharedPreferences("pts-play-updates",0)
    private val handler=Handler(Looper.getMainLooper())
    private val checks=UpdateCheckGate()
    private var resumed=false
    private var info:AppUpdateInfo?=null
    private var flowInFlight=false
    private var flowVersion=0
    private var downloadedVersion=0
    private var storeFallbackVersion=0
    var prompt by mutableStateOf<String?>(null)
        private set
    private val launcher=activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
        flowInFlight=false
        // The install listener may already have delivered Ready. Never erase it with a late result.
        if(prompt!="Ready") {
            when(result.resultCode) {
                Activity.RESULT_CANCELED -> {deferVersion(flowVersion);prompt=null}
                Activity.RESULT_OK -> prompt=null
                else -> {Log.w("PTSUpdates","Play update consent failed");storeFallbackVersion=flowVersion;prompt="Store"}
            }
        }
        check()
    }
    private val listener=InstallStateUpdatedListener {state->
        when(state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                downloadedVersion=info?.availableVersionCode() ?: flowVersion
                prompt="Ready"
            }
            InstallStatus.FAILED,InstallStatus.CANCELED -> {
                Log.w("PTSUpdates","Update install stopped: ${state.installStatus()}, error ${state.installErrorCode()}")
                if(prompt!="Ready") {storeFallbackVersion=info?.availableVersionCode() ?: flowVersion;prompt="Store"}
            }
        }
    }
    private val poll=object:Runnable {
        override fun run() {
            if(!resumed) return
            check()
            handler.postDelayed(this,30000L)
        }
    }
    init {activity.lifecycle.addObserver(this)}
    override fun onStart(owner:LifecycleOwner) {manager.registerListener(listener)}
    override fun onResume(owner:LifecycleOwner) {
        resumed=true;flowInFlight=false;check();handler.removeCallbacks(poll);handler.postDelayed(poll,15000L)
    }
    override fun onPause(owner:LifecycleOwner) {
        resumed=false;handler.removeCallbacks(poll);checks.cancel()
    }
    override fun onStop(owner:LifecycleOwner) {manager.unregisterListener(listener)}
    override fun onDestroy(owner:LifecycleOwner) {
        resumed=false;handler.removeCallbacks(poll);checks.cancel()
        manager.unregisterListener(listener);activity.lifecycle.removeObserver(this)
    }
    fun check() {
        if(!resumed || flowInFlight) return
        val ticket=checks.begin(SystemClock.elapsedRealtime()) ?: return
        // Activity-bound success callbacks can be detached onStop before the completion callback.
        // One unbound completion callback plus the session ticket handles both success and failure.
        manager.appUpdateInfo.addOnCompleteListener {task->
            if(!checks.complete(ticket) || !resumed) return@addOnCompleteListener
            if(!task.isSuccessful) {Log.w("PTSUpdates","Could not check Play update",task.exception);return@addOnCompleteListener}
            val value=task.result
            info=value
            val now=System.currentTimeMillis()
            val version=value.availableVersionCode()
            val status=value.installStatus()
            if(status==InstallStatus.DOWNLOADED) downloadedVersion=version
            val next=updatePrompt(
                value.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE,
                status==InstallStatus.DOWNLOADED,
                status==InstallStatus.PENDING || status==InstallStatus.DOWNLOADING || status==InstallStatus.INSTALLING,
                value.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && version!=storeFallbackVersion,
                shouldOfferUpdate(BuildConfig.VERSION_CODE,version,preferences.getInt("laterVersion",0),preferences.getLong("laterAt",0),now),
                shouldOfferRestart(version,preferences.getInt("restartLaterVersion",0),preferences.getLong("restartLaterAt",0),now))
            // A callback queried before DOWNLOADED must not erase the listener's newer Ready event.
            if(prompt!="Ready" || next=="Ready") prompt=next
        }
    }
    private fun deferVersion(version:Int) {
        if(version>0) preferences.edit().putInt("laterVersion",version).putLong("laterAt",System.currentTimeMillis()).apply()
    }
    fun later() {
        if(prompt=="Ready") preferences.edit().putInt("restartLaterVersion",downloadedVersion).putLong("restartLaterAt",System.currentTimeMillis()).apply()
        else deferVersion(info?.availableVersionCode() ?: flowVersion)
        prompt=null
    }
    fun download() {
        if(prompt=="Store") {openStore();return}
        val current=info ?: run {check();return}
        flowVersion=current.availableVersionCode()
        checks.cancel()
        info=null // A Play intent can only be used once, including failed launch attempts.
        prompt=null
        flowInFlight=true
        val started=runCatching {manager.startUpdateFlowForResult(current,launcher,AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())}
            .onFailure {Log.w("PTSUpdates","Could not start Play update",it)}.getOrDefault(false)
        if(!started) {flowInFlight=false;storeFallbackVersion=flowVersion;prompt="Store"}
        // Only an explicit Later/cancel action writes the 24-hour deferral.
    }
    private fun openStore() {
        prompt=null
        val opened=runCatching {activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")).setPackage("com.android.vending"))}
            .recoverCatching {activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")))}
        opened.onFailure {Log.w("PTSUpdates","Could not open Play Store",it);prompt="Store"}
    }
    fun restart() {
        prompt=null
        manager.completeUpdate().addOnFailureListener {Log.w("PTSUpdates","Could not complete update",it);prompt="Ready"}
    }
}
