package com.paytimeshift.pts.domain

import org.junit.Assert.*
import org.junit.Test

class InstallMeasurementTest {
    @Test fun noEventsWithoutConsentOrAfterDeclining() {
        var sends=0
        val session=InstallMeasurement {sends++;true}
        session.resume(0)
        session.consent(false,1)
        session.stop(2,false)
        session.resume(40_000)
        assertEquals(0,sends)
    }
    @Test fun consentingInForegroundSendsOnceAndRotationDoesNotDuplicate() {
        var sends=0
        val session=InstallMeasurement {sends++;true}
        session.resume(0)
        session.consent(true,1)
        session.consent(true,2)
        session.stop(3,true)
        session.resume(4)
        assertEquals(1,sends)
    }
    @Test fun shortPlayOrBillingVisitDoesNotCreateAnotherActivation() {
        var sends=0
        val session=InstallMeasurement {sends++;true}
        session.consent(true,0)
        session.resume(1)
        session.stop(2,false)
        session.resume(29_999)
        assertEquals(1,sends)
        session.stop(30_000,false)
        session.resume(60_000)
        assertEquals(2,sends)
    }
    @Test fun withdrawalStopsFutureActivationsAndRegrantWorks() {
        var sends=0
        val session=InstallMeasurement {sends++;true}
        session.consent(true,0);session.resume(1)
        session.consent(false,2)
        session.stop(3,false);session.resume(50_000)
        assertEquals(1,sends)
        session.consent(true,50_001)
        assertEquals(2,sends)
    }
    @Test fun failedSendCanRetryOnNextForegroundWithoutCrashing() {
        var attempts=0
        val session=InstallMeasurement {++attempts>1}
        session.consent(true,0);session.resume(1)
        session.stop(2,false);session.resume(3)
        assertEquals(2,attempts)
    }
}
