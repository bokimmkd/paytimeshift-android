package com.paytimeshift.pts.domain

/** Consent is a phone setting, deliberately separate from account and restored backups. */
class InstallMeasurement(private val sendActivation: () -> Boolean) {
    private var allowed = false
    private var foreground = false
    private var lastActivation: Long? = null
    private var backgroundAt: Long? = null

    fun consent(allow: Boolean, now: Long) {
        allowed = allow
        if (!allow) lastActivation = null
        if (allow && foreground) activate(now)
    }

    fun resume(now: Long) {
        if (foreground) return
        foreground = true
        if (allowed && (lastActivation == null || backgroundAt?.let { now - it >= 30_000L } == true)) activate(now)
    }

    fun stop(now: Long, changingConfiguration: Boolean) {
        if (changingConfiguration) return
        foreground = false
        backgroundAt = now
    }

    private fun activate(now: Long) {
        if (lastActivation == null || !foreground || backgroundAt?.let { now - it >= 30_000L } == true) {
            if (sendActivation()) {
                lastActivation = now
                backgroundAt = null
            }
        }
    }
}
