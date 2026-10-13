package com.paytimeshift.pts.ui

/** Only recover initial failed loads. AdMob keeps ownership of refresh after success. */
class BannerRetryPolicy {
    private var attempts=0
    private var lastRequest:Long?=null
    private var nextRequest=0L
    var loading=false; private set
    var loaded=false; private set
    fun begin(now:Long):Boolean {
        if(loaded || loading || attempts>=5 || now<nextRequest) return false
        loading=true;attempts++;lastRequest=now;return true
    }
    fun failed(now:Long) {loading=false;nextRequest=now+minOf(300000L,30000L shl (attempts-1).coerceIn(0,4))}
    fun success() {loading=false;loaded=true}
    fun networkChanged(now:Long) {
        if(loaded || loading) return
        if(attempts>=5) attempts=0
        nextRequest=maxOf(now,(lastRequest ?: (now-30000))+30000)
    }
    fun delayUntilRequest(now:Long):Long? = if(loaded || loading || attempts>=5) null else (nextRequest-now).coerceAtLeast(0)
}
