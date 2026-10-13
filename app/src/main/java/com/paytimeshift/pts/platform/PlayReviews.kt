package com.paytimeshift.pts.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.review.ReviewManagerFactory
import com.paytimeshift.pts.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

fun openPtsStore(context:Context):Boolean {
    val id=BuildConfig.APPLICATION_ID
    return runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=$id")).setPackage("com.android.vending"));true}
        .getOrElse {runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$id")));true}.getOrDefault(false)}
}

@Composable fun AutomaticPtsReview(activity:Activity,allowed:Boolean,hasShifts:Boolean) {
    // These timestamps never travel with shift data, cloud or local backup/restore.
    val prefs=remember(activity) {activity.getSharedPreferences("pts-review",Context.MODE_PRIVATE)}
    val firstUse=remember(prefs) {prefs.getLong("first-use",0).takeIf {it>0} ?: System.currentTimeMillis().also {prefs.edit().putLong("first-use",it).commit()}}
    val latestAllowed=rememberUpdatedState(allowed)
    val lifecycle=(activity as? LifecycleOwner)?.lifecycle
    var resumed by remember {mutableStateOf(lifecycle?.currentState==Lifecycle.State.RESUMED)}
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,_->resumed=lifecycle?.currentState==Lifecycle.State.RESUMED}
        lifecycle?.addObserver(observer)
        onDispose {lifecycle?.removeObserver(observer)}
    }
    LaunchedEffect(allowed,hasShifts,resumed) {
        if(!allowed || !resumed || !ReviewPolicy.eligible(System.currentTimeMillis(),firstUse,prefs.getLong("last-attempt",0),hasShifts)) return@LaunchedEffect
        delay(5000) // Wait for a quiet main screen; cancelled by any editor/dialog/update.
        if(!latestAllowed.value || !activity.hasWindowFocus() || activity.isFinishing || activity.isDestroyed) return@LaunchedEffect
        @Suppress("DEPRECATION")
        val playInstall=runCatching {activity.packageManager.getInstallerPackageName(activity.packageName)=="com.android.vending"}.getOrDefault(false)
        if(!playInstall) return@LaunchedEffect
        val now=System.currentTimeMillis()
        if(!ReviewPolicy.eligible(now,firstUse,prefs.getLong("last-attempt",0),hasShifts)) return@LaunchedEffect
        if(!prefs.edit().putLong("last-attempt",now).commit()) return@LaunchedEffect
        try {
            val manager=ReviewManagerFactory.create(activity)
            val info=withTimeoutOrNull(15000) {manager.requestReviewFlow().await()} ?: return@LaunchedEffect
            if(latestAllowed.value && activity.hasWindowFocus() && lifecycle?.currentState==Lifecycle.State.RESUMED && !activity.isFinishing && !activity.isDestroyed) manager.launchReviewFlow(activity,info).await()
        } catch(e:kotlinx.coroutines.CancellationException) {throw e}
        catch(_:Exception) { /* No popup, store redirect or "rated" flag for an unavailable/quota-limited flow. */ }
    }
}
