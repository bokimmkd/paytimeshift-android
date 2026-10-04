package com.paytimeshift.pts

import com.paytimeshift.pts.domain.shouldOfferUpdate
import com.paytimeshift.pts.domain.shouldOfferRestart
import com.paytimeshift.pts.domain.updatePrompt
import com.paytimeshift.pts.domain.UpdateCheckGate
import org.junit.Assert.*
import org.junit.Test

class UpdatePromptTest {
    @Test fun onlyNewVersionsAreOfferedAndRefusalDoesNotLoop() {
        val now=100000000L
        assertFalse(shouldOfferUpdate(14,14,0,0,now))
        assertFalse(shouldOfferUpdate(14,13,0,0,now))
        assertTrue(shouldOfferUpdate(14,15,0,0,now))
        assertFalse(shouldOfferUpdate(14,15,15,now,now+1000))
        assertTrue(shouldOfferUpdate(14,15,15,now,now+86400000))
        assertTrue(shouldOfferUpdate(14,16,15,now,now+1000))
    }
    @Test fun storeRouteDoesNotHideAnAvailableUpdateWhenFlexibleIsUnavailable() {
        assertEquals("Store",updatePrompt(true,false,false,false,true,true))
        assertEquals("Available",updatePrompt(true,false,false,true,true,true))
        assertNull(updatePrompt(false,false,false,false,true,true))
    }
    @Test fun activeDownloadDoesNotOfferASecondDownload() {
        assertNull(updatePrompt(true,false,true,true,true,true))
        assertNull(updatePrompt(true,false,true,false,true,true))
    }
    @Test fun downloadedUpdateOffersRestartEvenWithoutAnAvailableDownload() {
        assertEquals("Ready",updatePrompt(false,true,false,false,false,true))
        assertNull(updatePrompt(true,true,false,true,true,false))
    }
    @Test fun restartDeferralOfOneVersionCannotHideTheNextVersion() {
        val now=100000000L
        assertFalse(shouldOfferRestart(16,16,now,now+1000))
        assertTrue(shouldOfferRestart(17,16,now,now+1000))
        assertTrue(shouldOfferRestart(16,16,now,now+86400000))
    }
    @Test fun explicitLaterSuppressesOnlyThatAvailableVersion() {
        val now=100000000L
        val deferred=shouldOfferUpdate(15,16,16,now,now+1000)
        assertNull(updatePrompt(true,false,false,true,deferred,true))
        val newer=shouldOfferUpdate(15,17,16,now,now+1000)
        assertEquals("Available",updatePrompt(true,false,false,true,newer,true))
    }
    @Test fun concurrentChecksAreCoalescedUntilTheRequestCompletes() {
        val gate=UpdateCheckGate()
        val first=gate.begin(0)!!
        assertNull(gate.begin(1000))
        assertTrue(gate.complete(first))
        assertNotNull(gate.begin(1001))
    }
    @Test fun completedFailureDoesNotPermanentlyBlockTheNextResumeCheck() {
        val gate=UpdateCheckGate()
        assertTrue(gate.complete(gate.begin(0)!!))
        assertNotNull(gate.begin(1))
    }
    @Test fun returningToTheForegroundRejectsTheOldSessionsResponse() {
        val gate=UpdateCheckGate()
        val old=gate.begin(0)!!
        gate.cancel()
        val fresh=gate.begin(1)!!
        assertFalse(gate.complete(old))
        assertNull(gate.begin(2))
        assertTrue(gate.complete(fresh))
    }
    @Test fun unansweredRequestExpiresAndCannotEraseANewerReply() {
        val gate=UpdateCheckGate()
        val old=gate.begin(0)!!
        assertNull(gate.begin(14999))
        val fresh=gate.begin(15000)!!
        assertFalse(gate.complete(old))
        assertTrue(gate.complete(fresh))
        assertFalse(gate.complete(old))
    }
}
