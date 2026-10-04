package com.paytimeshift.pts

import android.os.Bundle
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.paytimeshift.pts.platform.ShortcutAction
import com.paytimeshift.pts.platform.PlayUpdates
import com.paytimeshift.pts.platform.MetaMeasurement
import com.paytimeshift.pts.ui.PtsApp

class MainActivity : ComponentActivity() {
    private var shortcutAction by mutableStateOf<ShortcutAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) shortcutAction = ShortcutAction.fromIntent(intent)
        enableEdgeToEdge()
        val updates=PlayUpdates(this)
        setContent { PtsApp(shortcutAction, updates) { shortcutAction = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutAction = ShortcutAction.fromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        MetaMeasurement.get(this).resume()
    }

    override fun onStop() {
        MetaMeasurement.get(this).stop(isChangingConfigurations)
        super.onStop()
    }
}
