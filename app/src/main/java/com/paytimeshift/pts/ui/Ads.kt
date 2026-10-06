package com.paytimeshift.pts.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.ump.*
import com.paytimeshift.pts.BuildConfig

val LocalPremium=staticCompositionLocalOf {false}
val LocalAdsResolved=staticCompositionLocalOf {false}
@Composable fun PtsBanner() {
    if(LocalPremium.current || !LocalAdsResolved.current) return
    val context=LocalContext.current;val activity=context as? Activity ?: return
    var consentReady by remember {mutableStateOf(false)}
    var ad by remember {mutableStateOf<AdView?>(null)}
    DisposableEffect(activity) {
        val consent=UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity,ConsentRequestParameters.Builder().build(),{
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {consentReady=consent.canRequestAds()}
        },{consentReady=consent.canRequestAds()})
        onDispose {ad?.destroy();ad=null}
    }
    val lifecycle=(activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle
    DisposableEffect(lifecycle,ad) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->when(event) {
            androidx.lifecycle.Lifecycle.Event.ON_RESUME->ad?.resume()
            androidx.lifecycle.Lifecycle.Event.ON_PAUSE->ad?.pause()
            else->Unit
        }}
        lifecycle?.addObserver(observer)
        onDispose {lifecycle?.removeObserver(observer)}
    }
    if(consentReady) BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width=maxWidth.value.toInt().coerceAtLeast(1)
        AndroidView(factory={
            AdView(activity).apply {
                adUnitId=if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-1171723950608276/4615840261"
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,width));ad=this
                MobileAds.initialize(activity) {activity.runOnUiThread {if(ad===this) loadAd(AdRequest.Builder().build())}}
            }
        },modifier=Modifier.fillMaxWidth().heightIn(min=50.dp))
    }
}
