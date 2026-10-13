package com.paytimeshift.pts.data

import android.content.Context
import java.io.File

/** A separate flag keeps onboarding out of job data and backup restores. */
class OnboardingStore(context: Context) {
    private val preferences=context.getSharedPreferences("pts-onboarding",Context.MODE_PRIVATE)
    private val dataFile=File(context.filesDir,"pts-data-v1.json")
    fun shouldShow(): Boolean {
        if(preferences.getBoolean("completed-v1",false)) return false
        // Existing installations keep their normal launch; the guide offers replay.
        if(dataFile.exists() || File(dataFile.path+".bak").exists()) {
            complete()
            return false
        }
        return true
    }
    fun complete() { preferences.edit().putBoolean("completed-v1",true).apply() }
}
