package com.paytimeshift.pts.ui

import android.app.Activity
import android.content.Context
import android.net.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import com.google.android.gms.ads.*
import com.google.android.ump.*
import com.paytimeshift.pts.BuildConfig

val LocalPremium=staticCompositionLocalOf {false}
val LocalAdsResolved=staticCompositionLocalOf {false}

@Composable fun PtsBanner() {
    if(LocalPremium.current || !LocalAdsResolved.current) return
    val activity=LocalContext.current as? Activity ?: return
    var consentReady by remember(activity) {mutableStateOf(false)}
    DisposableEffect(activity) {
        var alive=true
        val consent=UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity,ConsentRequestParameters.Builder().build(),{
            if(alive) UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {if(alive) consentReady=consent.canRequestAds()}
        },{if(alive) consentReady=consent.canRequestAds()})
        onDispose {alive=false}
    }
    if(consentReady) BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width=maxWidth.value.toInt().coerceAtLeast(1)
        val size=remember(activity,width) {AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width)}
        val view=remember(activity,width) {AdView(activity).apply {
            adUnitId=if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-1171723950608276/4615840261"
            setAdSize(size)
        }}
        var visible by remember(view) {mutableStateOf(false)}
        DisposableEffect(view) {
            val recovery=BannerRecovery(activity,view) {visible=true}
            recovery.start()
            onDispose {recovery.close()}
        }
        // Keep the view attached for loading, but reserve no space before onAdLoaded.
        Box(Modifier.fillMaxWidth().height(if(visible) size.height.dp else 0.dp).clipToBounds()) {
            key(view) {AndroidView(factory={view},modifier=Modifier.fillMaxWidth().requiredHeight(size.height.dp))}
        }
    }
}

private class BannerRecovery(val activity:Activity,val view:AdView,val shown:()->Unit) {
    private val handler=Handler(Looper.getMainLooper())
    private val policy=BannerRetryPolicy()
    private val lifecycle=(activity as? LifecycleOwner)?.lifecycle
    private val network=activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var registered=false
    private var ready=false
    private var closed=false
    private var foreground=lifecycle?.currentState==Lifecycle.State.RESUMED
    private var validatedNetwork:Network?=null
    private val retry=Runnable {request()}
    private val observer=LifecycleEventObserver {_,event->when(event) {
        Lifecycle.Event.ON_RESUME->{foreground=true;view.resume();schedule()}
        Lifecycle.Event.ON_PAUSE->{foreground=false;handler.removeCallbacks(retry);view.pause()}
        else->Unit
    }}
    private val callback=object:ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(n:Network,c:NetworkCapabilities) {
            if(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) handler.post {
                if(!closed && validatedNetwork!=n) {validatedNetwork=n;policy.networkChanged(SystemClock.elapsedRealtime());schedule()}
            }
        }
        override fun onLost(n:Network) {handler.post {if(validatedNetwork==n) validatedNetwork=null}}
    }
    fun start() {
        view.adListener=object:AdListener() {
            override fun onAdLoaded() {if(!closed) {policy.success();handler.removeCallbacks(retry);shown()}}
            override fun onAdFailedToLoad(error:LoadAdError) {
                if(closed) return
                Log.w("PTSAds","Banner failed: domain=${error.domain}, code=${error.code}, message=${error.message}")
                policy.failed(SystemClock.elapsedRealtime());schedule()
            }
        }
        lifecycle?.addObserver(observer)
        registered=runCatching {network.registerDefaultNetworkCallback(callback);true}.getOrDefault(false)
        MobileAds.initialize(activity) {handler.post {if(!closed) {ready=true;schedule()}}}
    }
    private fun schedule() {
        handler.removeCallbacks(retry)
        if(closed || !ready || !foreground) return
        policy.delayUntilRequest(SystemClock.elapsedRealtime())?.let {handler.postDelayed(retry,it)}
    }
    private fun request() {
        if(!closed && ready && foreground && policy.begin(SystemClock.elapsedRealtime())) view.loadAd(AdRequest.Builder().build())
    }
    fun close() {
        closed=true;handler.removeCallbacksAndMessages(null);lifecycle?.removeObserver(observer)
        if(registered) runCatching {network.unregisterNetworkCallback(callback)}
        view.adListener=object:AdListener() {};view.destroy()
    }
}
