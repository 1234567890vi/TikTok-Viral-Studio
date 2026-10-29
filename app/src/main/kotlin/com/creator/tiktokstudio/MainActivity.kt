package com.creator.tiktokstudio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.creator.tiktokstudio.core.ThemeAndLocaleManager
import com.creator.tiktokstudio.ui.theme.DeepDarkBackground
import com.creator.tiktokstudio.ui.theme.TikTokStudioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Theme and Locale settings
        ThemeAndLocaleManager.init(this)

        // Handle shared link from TikTok share menu if present
        handleIncomingShare(intent)

        setContent {
            val isDark by ThemeAndLocaleManager.isDarkMode.collectAsState()
            val currentLang by ThemeAndLocaleManager.currentLanguage.collectAsState()
            val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                TikTokStudioTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DeepDarkBackground
                    ) {
                        MainNavigation()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank() && sharedText.contains("tiktok.com")) {
                sharedTikTokUrl = sharedText
            }
        }
    }

    companion object {
        var sharedTikTokUrl: String? = null
    }
}
