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

/** Play transfer state, independent of availability prompts and their Later preferences. */
enum class UpdateStage { Idle, Waiting, Downloading, Ready, Installing, Stopped, Installed }
data class UpdateTransfer(val version:Int=0,val stage:UpdateStage=UpdateStage.Idle,
                          val downloaded:Long=0,val total:Long=0,val completedVersion:Int=0) {
    val fraction:Float? get()=if(total>0) (downloaded.toDouble()/total).coerceIn(0.0,1.0).toFloat() else null
    fun observe(installed:Int,available:Int,next:UpdateStage,bytes:Long=0,totalBytes:Long=0):UpdateTransfer {
        if(available<=installed) return UpdateTransfer(completedVersion=completedVersion)
        if(available<=completedVersion) return this
        if(available<version || available==version && stage==UpdateStage.Installed) return this
        if(available==version && stage in listOf(UpdateStage.Waiting,UpdateStage.Downloading) && next==UpdateStage.Idle) return this
        if(available==version && stage in listOf(UpdateStage.Ready,UpdateStage.Installing) &&
            next in listOf(UpdateStage.Idle,UpdateStage.Waiting,UpdateStage.Downloading,UpdateStage.Ready)) return this
        return UpdateTransfer(available,next,bytes.coerceAtLeast(0),totalBytes.coerceAtLeast(0),if(next==UpdateStage.Installed) maxOf(completedVersion,available) else completedVersion)
    }
    fun beginInstall():UpdateTransfer = if(stage==UpdateStage.Ready) copy(stage=UpdateStage.Installing) else this
    fun installFailed():UpdateTransfer = if(stage==UpdateStage.Installing) copy(stage=UpdateStage.Ready) else this
}
