package com.paytimeshift.pts

import com.paytimeshift.pts.platform.ReviewPolicy
import com.paytimeshift.pts.premium.StatusCachePolicy
import com.paytimeshift.pts.ui.BannerRetryPolicy
import org.junit.Assert.*
import org.junit.Test

class CloudAndReviewPolicyTest {
    @Test fun reviewNeedsSevenDaysOfActualUseAndShiftData() {
        val start=1000L
        assertFalse(ReviewPolicy.eligible(start+7*ReviewPolicy.DAY-1,start,0,true))
        assertTrue(ReviewPolicy.eligible(start+7*ReviewPolicy.DAY,start,0,true))
        assertFalse(ReviewPolicy.eligible(start+40*ReviewPolicy.DAY,start,0,false))
    }
    @Test fun reviewCooldownStartsAtAttemptAndClockRollbackCannotBypassIt() {
        val start=1000L;val attempt=start+9*ReviewPolicy.DAY
        assertFalse(ReviewPolicy.eligible(start+30*ReviewPolicy.DAY,start,attempt,true))
        assertFalse(ReviewPolicy.eligible(attempt-1,start,attempt,true))
        assertTrue(ReviewPolicy.eligible(attempt+30*ReviewPolicy.DAY,start,attempt,true))
    }
    @Test fun cachedPremiumNeverSurvivesExpiryOrFreshnessLimit() {
        assertTrue(StatusCachePolicy.fresh(100,0,1000,true,1001))
        assertFalse(StatusCachePolicy.fresh(100,0,1000,true,1000))
        assertFalse(StatusCachePolicy.fresh(StatusCachePolicy.TTL,0,1000,false,0))
        assertFalse(StatusCachePolicy.fresh(50,100,1000,false,0))
    }
    @Test fun bannerBackoffCoalescesRequestsAndNetworkRecoveryRespectsSpacing() {
        val p=BannerRetryPolicy();assertTrue(p.begin(1000));assertFalse(p.begin(1001))
        p.failed(2000);assertEquals(30000L,p.delayUntilRequest(2000));assertFalse(p.begin(31999))
        p.networkChanged(5000);assertFalse(p.begin(30999));assertTrue(p.begin(31000))
        p.failed(32000);assertEquals(60000L,p.delayUntilRequest(32000))
    }
    @Test fun bannerStopsAfterFiveAttemptsAndNewNetworkCanRecover() {
        val p=BannerRetryPolicy();var now=0L
        repeat(5) {assertTrue(p.begin(now));p.failed(now);now+=p.delayUntilRequest(now) ?: 300000L}
        assertNull(p.delayUntilRequest(now));assertFalse(p.begin(now))
        p.networkChanged(now);assertTrue(p.begin(now))
    }
    @Test fun successfulBannerLeavesRefreshToAdMobEvenAfterNetworkChanges() {
        val p=BannerRetryPolicy();assertTrue(p.begin(0));p.success();p.networkChanged(60000);p.failed(60000)
        assertFalse(p.begin(600000));assertNull(p.delayUntilRequest(600000));assertTrue(p.loaded)
    }
}
