package com.paytimeshift.pts.premium

object StatusCachePolicy {
    const val TTL=5*60*1000L
    fun fresh(now:Long,checkedAt:Long,wallNow:Long,premium:Boolean,expiresAt:Long):Boolean =
        now>=checkedAt && now-checkedAt<TTL && (!premium || expiresAt>wallNow)
}
