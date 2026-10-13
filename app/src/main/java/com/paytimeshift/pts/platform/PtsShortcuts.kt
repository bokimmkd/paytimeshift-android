package com.paytimeshift.pts.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.paytimeshift.pts.MainActivity
import com.paytimeshift.pts.R
import com.paytimeshift.pts.ui.translate

enum class ShortcutAction(val id: String, val label: String, val icon: Int) {
    AddShift("add_shift", "Add shift", R.drawable.ic_shortcut_add_shift),
    Calendar("calendar", "Calendar", R.drawable.ic_shortcut_calendar),
    Earnings("earnings", "Earnings", R.drawable.ic_shortcut_earnings),
    Jobs("jobs", "Jobs", R.drawable.ic_shortcut_jobs);

    val intentAction: String get() = "com.paytimeshift.pts.shortcut.$id"

    fun intent(context: Context): Intent = Intent(context, MainActivity::class.java)
        .setAction(intentAction)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    companion object {
        fun fromIntent(intent: Intent?): ShortcutAction? = entries.firstOrNull { it.intentAction == intent?.action }
    }
}

/** Labels follow PTS's chosen language, including its English default. */
fun publishPtsShortcuts(context: Context, language: String) {
    val manager = context.getSystemService(ShortcutManager::class.java) ?: return
    if (manager.isRateLimitingActive) return
    val shortcuts = ShortcutAction.entries.take(manager.maxShortcutCountPerActivity).mapIndexed { rank, action ->
        val label = translate(action.label, language)
        ShortcutInfo.Builder(context, action.id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(context, action.icon))
            .setIntent(action.intent(context))
            .setRank(rank)
            .build()
    }
    val current = manager.dynamicShortcuts
    if (current.size == shortcuts.size && shortcuts.all { next ->
        current.any { it.id == next.id && it.shortLabel.toString() == next.shortLabel.toString() && it.intent?.action == next.intent?.action }
    }) return
    // A launcher restriction must not prevent opening or using the app.
    runCatching {
        manager.updateShortcuts(shortcuts)
        manager.dynamicShortcuts = shortcuts
    }
}
