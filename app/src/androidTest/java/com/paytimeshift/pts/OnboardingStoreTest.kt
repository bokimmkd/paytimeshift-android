package com.paytimeshift.pts

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.data.OnboardingStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class OnboardingStoreTest {
    private val base=InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs="pts-onboarding-test"
    private val folder=File(base.cacheDir,"onboarding-test")
    private val context=object:ContextWrapper(base) {
        override fun getFilesDir()=folder
        override fun getSharedPreferences(name:String,mode:Int)=base.getSharedPreferences(prefs,mode)
    }
    @Before fun reset() {folder.deleteRecursively();folder.mkdirs();base.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit()}
    @After fun clean() {folder.deleteRecursively();base.getSharedPreferences(prefs,Context.MODE_PRIVATE).edit().clear().commit()}
    @Test fun freshInstallShowsOnceAfterCompletion() {
        assertTrue(OnboardingStore(context).shouldShow())
        OnboardingStore(context).complete()
        assertFalse(OnboardingStore(context).shouldShow())
    }
    @Test fun existingDataSkipsIntroductionWithoutModifyingTheFile() {
        val file=File(folder,"pts-data-v1.json").apply {writeText("existing-data")}
        assertFalse(OnboardingStore(context).shouldShow())
        assertEquals("existing-data",file.readText())
    }
    @Test fun recoverableAtomicBackupCountsAsExistingData() {
        File(folder,"pts-data-v1.json.bak").writeText("recoverable")
        assertFalse(OnboardingStore(context).shouldShow())
    }
}
