package com.paytimeshift.pts.platform

object ReviewPolicy {
    const val DAY=86400000L
    fun eligible(now:Long,firstUse:Long,lastAttempt:Long,hasShifts:Boolean):Boolean =
        hasShifts && firstUse>0 && now>=firstUse && now-firstUse>=7*DAY &&
        (lastAttempt==0L || (now>=lastAttempt && now-lastAttempt>=30*DAY))
}
