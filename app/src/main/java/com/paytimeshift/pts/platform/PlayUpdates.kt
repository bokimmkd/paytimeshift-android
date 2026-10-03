package com.paytimeshift.pts.platform

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
import com.paytimeshift.pts.domain.shouldOfferUpdate

/** Flexible updates never block local work. Play remains the source of available-version truth. */
class PlayUpdates(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    private val manager=AppUpdateManagerFactory.create(activity)
    private val preferences=activity.getSharedPreferences("pts-play-updates",0)
    private var info:AppUpdateInfo?=null
    private var checking=false
    var prompt by mutableStateOf<String?>(null)
        private set
    private val launcher=activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        // Cancellation/failure is optional. The next resume query checks downloaded state.
        prompt=null
    }
    private val listener=InstallStateUpdatedListener {state->
        if(state.installStatus()==InstallStatus.DOWNLOADED) prompt="Ready"
        else if(state.installStatus()==InstallStatus.FAILED || state.installStatus()==InstallStatus.CANCELED) prompt=null
    }
    init { activity.lifecycle.addObserver(this) }
    override fun onStart(owner:LifecycleOwner) {manager.registerListener(listener)}
    override fun onStop(owner:LifecycleOwner) {manager.unregisterListener(listener)}
    override fun onResume(owner:LifecycleOwner) { check() }
    override fun onDestroy(owner:LifecycleOwner) {manager.unregisterListener(listener);activity.lifecycle.removeObserver(this)}
    fun check() {
        if(checking) return
        checking=true
        manager.appUpdateInfo.addOnSuccessListener(activity) {value->
            info=value
            if(value.installStatus()==InstallStatus.DOWNLOADED) {
                if(System.currentTimeMillis()-preferences.getLong("restartLaterAt",0)>=24*60*60*1000L) prompt="Ready"
            } else if(value.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE && value.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                shouldOfferUpdate(BuildConfig.VERSION_CODE,value.availableVersionCode(),preferences.getInt("laterVersion",0),preferences.getLong("laterAt",0),System.currentTimeMillis())) prompt="Available"
        }.addOnCompleteListener {checking=false}
    }
    fun later() {
        if(prompt=="Ready") preferences.edit().putLong("restartLaterAt",System.currentTimeMillis()).apply()
        else info?.let {preferences.edit().putInt("laterVersion",it.availableVersionCode()).putLong("laterAt",System.currentTimeMillis()).apply()}
        prompt=null
    }
    fun download() {
        val current=info ?: return
        later()
        runCatching {manager.startUpdateFlowForResult(current,launcher,AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())}
        info=null // A Play intent can only be used once. Resume obtains a fresh instance.
    }
    fun restart() {
        prompt=null
        manager.completeUpdate().addOnFailureListener {prompt="Ready"}
    }
}
