package com.paytimeshift.pts

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.platform.PlayUpdateState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateLifecycleTest {
    @Test fun laterSurvivesRotationButReturnsAfterLeavingAndOpeningPts() {
        ActivityScenario.launch(MainActivity::class.java).use {scenario->
            lateinit var original:PlayUpdateState
            val available=UpdateTransfer(version=BuildConfig.VERSION_CODE+1)
            scenario.onActivity {activity->
                original=ViewModelProvider(activity)[PlayUpdateState::class.java]
                original.session.later(available.version)
                assertNull(original.session.prompt(BuildConfig.VERSION_CODE,available,true,true))
            }
            scenario.recreate()
            scenario.onActivity {activity->
                val current=ViewModelProvider(activity)[PlayUpdateState::class.java]
                assertSame(original,current)
                assertNull(current.session.prompt(BuildConfig.VERSION_CODE,available,true,true))
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {activity->
                val current=ViewModelProvider(activity)[PlayUpdateState::class.java]
                assertEquals("Available",current.session.prompt(BuildConfig.VERSION_CODE,available,true,true))
            }
        }
    }
    @Test fun playConsentSurvivesRotationWithoutRevivingUpdateOffer() {
        ActivityScenario.launch(MainActivity::class.java).use {scenario->
            val version=BuildConfig.VERSION_CODE+1
            scenario.onActivity {activity->
                ViewModelProvider(activity)[PlayUpdateState::class.java].session.beginConsent(version)
            }
            scenario.recreate()
            scenario.onActivity {activity->
                val current=ViewModelProvider(activity)[PlayUpdateState::class.java]
                assertEquals(version,current.session.consentVersion)
                assertNull(current.session.prompt(BuildConfig.VERSION_CODE,UpdateTransfer(version=version),true,true))
                current.session.endConsent()
            }
        }
    }
}
