package com.paytimeshift.pts.domain

/** A refused version can be offered again after 24 hours, or immediately for a newer version. */
fun shouldOfferUpdate(installed: Int, available: Int, deferredVersion: Int, deferredAt: Long, now: Long): Boolean =
    available>installed && (available!=deferredVersion || now-deferredAt>=24*60*60*1000L)

/** Restart deferral belongs to one downloaded version, never to all future updates. */
fun shouldOfferRestart(available:Int, deferredVersion:Int, deferredAt:Long, now:Long):Boolean =
    available!=deferredVersion || now-deferredAt>=24*60*60*1000L

fun updatePrompt(available:Boolean, downloaded:Boolean, installing:Boolean, flexibleAllowed:Boolean,
                 offerVersion:Boolean, offerRestart:Boolean):String? = when {
    downloaded -> if(offerRestart) "Ready" else null
    installing -> null
    available && offerVersion -> if(flexibleAllowed) "Available" else "Store"
    else -> null
}

/** Bounds an unanswered Play request and rejects callbacks from an old foreground session. */
class UpdateCheckGate(private val timeoutMillis:Long=15000L) {
    private var sequence=0L
    private var active:Pair<Long,Long>?=null
    fun begin(now:Long):Long? {
        val pending=active
        if(pending!=null && now-pending.second<timeoutMillis) return null
        return (++sequence).also {active=it to now}
    }
    fun complete(ticket:Long):Boolean {
        if(active?.first!=ticket) return false
        active=null
        return true
    }
    fun cancel() {active=null}
}
