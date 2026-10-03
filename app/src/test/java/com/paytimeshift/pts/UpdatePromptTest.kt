package com.paytimeshift.pts

import com.paytimeshift.pts.domain.shouldOfferUpdate
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
}
