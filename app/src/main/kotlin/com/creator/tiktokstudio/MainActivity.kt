package com.creator.tiktokstudio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.creator.tiktokstudio.ui.theme.DeepDarkBackground
import com.creator.tiktokstudio.ui.theme.TikTokStudioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shared link from TikTok share menu if present
        handleIncomingShare(intent)

        setContent {
            TikTokStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepDarkBackground
                ) {
                    MainNavigation()
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
                // Shared text can be consumed by the Downloader module
                sharedTikTokUrl = sharedText
            }
        }
    }

    companion object {
        var sharedTikTokUrl: String? = null
    }
}
