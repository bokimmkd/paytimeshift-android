package com.paytimeshift.pts.platform

import android.content.Context
import android.os.SystemClock
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.paytimeshift.pts.domain.InstallMeasurement

/** Only install/activation measurement. No account, schedule, pay, purchase or screen parameters. */
class MetaMeasurement private constructor(context: Context) {
    private val context = context.applicationContext
    private val preferences = context.getSharedPreferences("pts_install_measurement", Context.MODE_PRIVATE)
    private val session = InstallMeasurement { sendActivation() }
    val allowed: Boolean get() = preferences.getBoolean("allowed", false)
    val chosen: Boolean get() = preferences.contains("allowed")

    init { session.consent(allowed, SystemClock.elapsedRealtime()) }

    fun choose(allow: Boolean) {
        preferences.edit().putBoolean("allowed", allow).apply()
        session.consent(allow, SystemClock.elapsedRealtime())
    }

    fun resume() = session.resume(SystemClock.elapsedRealtime())
    fun stop(changingConfiguration: Boolean) = session.stop(SystemClock.elapsedRealtime(), changingConfiguration)

    @Suppress("DEPRECATION")
    private fun sendActivation(): Boolean = runCatching {
        // The SDK's init provider is removed: declining never initializes Meta.
        // Keep automatic purchase/codeless logging and advertising ID collection off.
        FacebookSdk.setAutoLogAppEventsEnabled(false)
        FacebookSdk.setAdvertiserIDCollectionEnabled(false)
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)
        if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(context)
        FacebookSdk.fullyInitialize()
        FacebookSdk.setLimitEventAndDataUsage(context, true)
        FacebookSdk.publishInstallAsync(context, FacebookSdk.getApplicationId())
        AppEventsLogger.newLogger(context).apply {
            logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP)
            flush()
        }
        true
    }.getOrDefault(false)

    companion object {
        @Volatile private var instance: MetaMeasurement? = null
        fun get(context: Context): MetaMeasurement = instance ?: synchronized(this) {
            instance ?: MetaMeasurement(context).also { instance = it }
        }
    }
}
