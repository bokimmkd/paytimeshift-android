package com.paytimeshift.pts.domain

/** Restoring work data must not change the appearance selected on this phone. */
fun AppData.withRestoredBackup(backup:AppData):AppData =
    backup.copy(preferences=backup.preferences.copy(appearance=preferences.appearance))
