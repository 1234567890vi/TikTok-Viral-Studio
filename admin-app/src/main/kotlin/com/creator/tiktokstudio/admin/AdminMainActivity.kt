package com.creator.tiktokstudio.admin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.creator.tiktokstudio.admin.ui.theme.AdminAppTheme
import com.creator.tiktokstudio.admin.ui.theme.DeepDarkBackground

class AdminMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("admin_auth_prefs", Context.MODE_PRIVATE)
        val isAutoLoggedIn = prefs.getBoolean("is_logged_in", false) && prefs.getBoolean("remember_admin", false)

        setContent {
            AdminAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepDarkBackground
                ) {
                    var isLoggedIn by remember { mutableStateOf(isAutoLoggedIn) }

                    if (isLoggedIn) {
                        AdminDashboardScreen(
                            onLogout = {
                                prefs.edit().putBoolean("is_logged_in", false).apply()
                                isLoggedIn = false
                            }
                        )
                    } else {
                        AdminLoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
