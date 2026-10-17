package com.englishpartner.hub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishpartner.hub.data.settings.AppSettings
import com.englishpartner.hub.ui.AppNavigation
import com.englishpartner.hub.ui.theme.EnglishPartnerTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as EnglishPartnerApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle the launch intent once; after process recreation it was already handled.
        if (savedInstanceState == null) container.shareInbox.accept(intent)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            EnglishPartnerTheme(themeMode = settings.themeMode) {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        container.shareInbox.accept(intent)
    }
}
