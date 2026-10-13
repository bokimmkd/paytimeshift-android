package com.paytimeshift.pts

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.FacebookSdk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetaConfigurationTest {
    @Test fun metaUsesPtsAppAndHasNoAutomaticInitProvider() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val info=context.packageManager.getApplicationInfo(context.packageName,PackageManager.GET_META_DATA)
        assertEquals("1899290267897887",info.metaData.getString("com.facebook.sdk.ApplicationId"))
        assertEquals(32,info.metaData.getString("com.facebook.sdk.ClientToken")!!.length)
        assertFalse(info.metaData.getBoolean("com.facebook.sdk.AutoLogAppEventsEnabled"))
        assertFalse(info.metaData.getBoolean("com.facebook.sdk.AdvertiserIDCollectionEnabled"))
        val providers=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_PROVIDERS).providers.orEmpty()
        assertFalse(providers.any {it.name=="com.facebook.internal.FacebookInitProvider"})
    }

    @Test fun launchingWithoutOptInDoesNotInitializeMeta() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pts_install_measurement",Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use {scenario->
            scenario.onActivity {assertFalse(FacebookSdk.isInitialized())}
            scenario.recreate()
            scenario.onActivity {assertFalse(FacebookSdk.isInitialized())}
        }
    }
}
