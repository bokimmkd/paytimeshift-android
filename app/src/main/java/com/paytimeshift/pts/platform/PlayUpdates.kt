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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.paytimeshift.pts.BuildConfig
import com.paytimeshift.pts.domain.*

/** Keep the app visit and active transfer intact during rotation, including Play consent. */
class PlayUpdateState:ViewModel() {
    val session=UpdateOfferSession()
    var transfer by mutableStateOf(UpdateTransfer())
    var installFailed by mutableStateOf(false)
    var prompt by mutableStateOf<String?>(null)
    var flowVersion=0
    var storeFallbackVersion=0
}

/** Optional flexible updates. Play owns consent, download, installation and process restart. */
class PlayUpdates(private val activity:ComponentActivity):DefaultLifecycleObserver {
    private val manager=AppUpdateManagerFactory.create(activity)
    private val state=ViewModelProvider(activity)[PlayUpdateState::class.java]
    private val session get()=state.session
    private val handler=Handler(Looper.getMainLooper())
    private val checks=UpdateCheckGate()
    private var resumed=false
    private var info:AppUpdateInfo?=null
    private val flowInFlight get()=session.consentVersion>0
    private var flowVersion:Int
        get()=state.flowVersion
        set(value) {state.flowVersion=value}
    private var storeFallbackVersion:Int
        get()=state.storeFallbackVersion
        set(value) {state.storeFallbackVersion=value}
    private var observation=0L
    var transfer:UpdateTransfer
        get()=state.transfer
        private set(value) {state.transfer=value}
    var installFailed:Boolean
        get()=state.installFailed
        private set(value) {state.installFailed=value}
    var prompt:String?
        get()=state.prompt
        private set(value) {state.prompt=value}
    private fun refreshPrompt() {
        prompt=session.prompt(BuildConfig.VERSION_CODE,transfer,
            info?.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE,
            info?.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)==true && transfer.version!=storeFallbackVersion)
    }
    private fun stage(status:Int)=when(status) {
        InstallStatus.PENDING -> UpdateStage.Waiting
        InstallStatus.DOWNLOADING -> UpdateStage.Downloading
        InstallStatus.DOWNLOADED -> UpdateStage.Ready
        InstallStatus.INSTALLING -> UpdateStage.Installing
        InstallStatus.INSTALLED -> UpdateStage.Installed
        InstallStatus.FAILED,InstallStatus.CANCELED -> UpdateStage.Stopped
        else -> UpdateStage.Idle
    }
    private fun observe(version:Int,status:Int,bytes:Long=0,total:Long=0) {
        transfer=transfer.observe(BuildConfig.VERSION_CODE,version,stage(status),bytes,total)
        if(transfer.stage==UpdateStage.Stopped) storeFallbackVersion=transfer.version
        refreshPrompt()
    }

    private val launcher=activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
        session.endConsent()
        // Consent can finish after a download event. Never replace a newer listener state.
        if(transfer.stage !in listOf(UpdateStage.Ready,UpdateStage.Downloading,UpdateStage.Installing,UpdateStage.Installed,UpdateStage.Stopped)) {
            observation++
            when(result.resultCode) {
                Activity.RESULT_CANCELED -> {session.later(flowVersion);transfer=UpdateTransfer(completedVersion=transfer.completedVersion);prompt=null}
                Activity.RESULT_OK -> observe(flowVersion,InstallStatus.PENDING)
                else -> {Log.w("PTSUpdates","Play update consent failed");observe(flowVersion,InstallStatus.FAILED)}
            }
        }
        refreshPrompt();check()
    }
    private val listener=InstallStateUpdatedListener {state->
        observation++
        observe(maxOf(flowVersion,info?.availableVersionCode() ?: 0),
            state.installStatus(),state.bytesDownloaded(),state.totalBytesToDownload())
        if(state.installStatus()==InstallStatus.FAILED || state.installStatus()==InstallStatus.CANCELED)
            Log.w("PTSUpdates","Update stopped: ${state.installStatus()}, error ${state.installErrorCode()}")
    }
    private val poll=object:Runnable {
        override fun run() {
            if(!resumed) return
            check();handler.postDelayed(this,30000L)
        }
    }
    init {activity.lifecycle.addObserver(this)}
    override fun onStart(owner:LifecycleOwner) {session.startForeground();manager.registerListener(listener)}
    override fun onResume(owner:LifecycleOwner) {
        resumed=true;check();handler.removeCallbacks(poll);handler.postDelayed(poll,15000L)
    }
    override fun onPause(owner:LifecycleOwner) {resumed=false;handler.removeCallbacks(poll);checks.cancel()}
    override fun onStop(owner:LifecycleOwner) {session.stopForeground(activity.isChangingConfigurations);manager.unregisterListener(listener)}
    override fun onDestroy(owner:LifecycleOwner) {
        resumed=false;handler.removeCallbacks(poll);checks.cancel()
        manager.unregisterListener(listener);activity.lifecycle.removeObserver(this)
    }
    fun check() {
        if(!resumed || flowInFlight) return
        val ticket=checks.begin(SystemClock.elapsedRealtime()) ?: return
        val queriedAt=observation
        manager.appUpdateInfo.addOnCompleteListener {task->
            if(!checks.complete(ticket) || !resumed || queriedAt!=observation) return@addOnCompleteListener
            if(!task.isSuccessful) {Log.w("PTSUpdates","Could not check Play update",task.exception);return@addOnCompleteListener}
            val value=task.result
            info=value
            val version=value.availableVersionCode()
            observe(version,value.installStatus(),value.bytesDownloaded(),value.totalBytesToDownload())
            if(version<=maxOf(BuildConfig.VERSION_CODE,transfer.completedVersion) || transfer.stage==UpdateStage.Installed) {prompt=null;return@addOnCompleteListener}
        }
    }
    fun later() {
        observation++
        session.later(transfer.version,prompt=="Ready")
        prompt=null
    }
    fun download() {
        if(prompt=="Store") {openStore();return}
        if(prompt!="Available" || flowInFlight) return
        val current=info ?: run {check();return}
        flowVersion=current.availableVersionCode()
        checks.cancel();observation++;installFailed=false
        info=null // Play intents are single-use, including failed launch attempts.
        prompt=null;session.beginConsent(flowVersion)
        val started=runCatching {manager.startUpdateFlowForResult(current,launcher,AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())}
            .onFailure {Log.w("PTSUpdates","Could not start Play update",it)}.getOrDefault(false)
        if(!started) {session.endConsent();storeFallbackVersion=flowVersion;prompt="Store"}
    }
    private fun openStore() {
        prompt=null
        val opened=runCatching {activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")).setPackage("com.android.vending"))}
            .recoverCatching {activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")))}
        opened.onFailure {Log.w("PTSUpdates","Could not open Play Store",it);prompt="Store"}
    }
    fun restart() {
        if(prompt!="Ready" || transfer.stage!=UpdateStage.Ready) return
        checks.cancel();observation++;installFailed=false
        transfer=transfer.beginInstall();prompt=null
        manager.completeUpdate().addOnFailureListener {
            Log.w("PTSUpdates","Could not complete update",it)
            if(transfer.stage==UpdateStage.Installing) {observation++;transfer=transfer.installFailed();installFailed=true;prompt="Ready"}
        }
    }
}
