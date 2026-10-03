package com.paytimeshift.pts.domain

/** A refused version can be offered again after 24 hours, or immediately for a newer version. */
fun shouldOfferUpdate(installed: Int, available: Int, deferredVersion: Int, deferredAt: Long, now: Long): Boolean =
    available>installed && (available!=deferredVersion || now-deferredAt>=24*60*60*1000L)
