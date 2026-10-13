package com.paytimeshift.pts.domain

import com.paytimeshift.pts.data.LocalStore
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {
    @Test fun everyRestoreKeepsThisPhonesSelectedAppearance() {
        val backup=AppData(jobs=listOf(Job(id="saved",name="Saved job")),preferences=Preferences(appearance="Dark",language="mk",gapHours=12.0))
        for(appearance in listOf("Light","Dark","System")) {
            val phone=AppData(preferences=Preferences(appearance=appearance))
            val restored=phone.withRestoredBackup(backup)
            assertEquals(appearance,restored.preferences.appearance)
            assertEquals(appearance,restored.withRestoredBackup(backup).preferences.appearance)
            assertEquals(backup.jobs,restored.jobs)
            assertEquals("mk",restored.preferences.language)
            assertEquals(12.0,restored.preferences.gapHours,0.0)
            assertEquals(restored,LocalStore.decode(LocalStore.encode(restored)))
        }
    }
    @Test fun restoringALightBackupDoesNotChangeDarkOrSystemTheme() {
        val backup=AppData(preferences=Preferences(appearance="Light"))
        for(appearance in listOf("Dark","System")) {
            assertEquals(appearance,AppData(preferences=Preferences(appearance=appearance)).withRestoredBackup(backup).preferences.appearance)
        }
    }
}
