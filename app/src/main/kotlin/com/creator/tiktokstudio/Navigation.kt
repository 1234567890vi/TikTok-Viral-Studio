package com.creator.tiktokstudio

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.creator.tiktokstudio.core.AppControlState
import com.creator.tiktokstudio.core.RemoteUpdateManager
import com.creator.tiktokstudio.core.TelemetryAndCommandService
import com.creator.tiktokstudio.ui.main.MainScreen
import com.creator.tiktokstudio.ui.screens.*

@Composable
fun MainNavigation() {
    val isBanned by TelemetryAndCommandService.isDeviceBanned.collectAsState()
    val controlState by TelemetryAndCommandService.controlState.collectAsState()
    val controlMessage by TelemetryAndCommandService.controlMessage.collectAsState()
    val pendingAlert by TelemetryAndCommandService.pendingAlertMessage.collectAsState()

    // 1. If device is banned by admin, block everything
    if (isBanned) {
        DeviceBannedScreen()
        return
    }

    // 2. If app is stopped by admin kill-switch
    if (controlState == AppControlState.STOPPED) {
        AppStoppedScreen(message = controlMessage)
        return
    }

    // 3. If app is in maintenance mode
    if (controlState == AppControlState.MAINTENANCE) {
        AppMaintenanceScreen(message = controlMessage)
        return
    }

    // 4. Instant Admin Alert Popup Dialog
    pendingAlert?.let { alertText ->
        AlertDialog(
            onDismissRequest = { TelemetryAndCommandService.clearAlert() },
            title = { Text("📢 رسالة من إدارة التطبيق") },
            text = { Text(alertText) },
            confirmButton = {
                Button(onClick = { TelemetryAndCommandService.clearAlert() }) {
                    Text("حسناً")
                }
            }
        )
    }

    val backStack = rememberNavBackStack(SplashNavKey)
    val isUpdateRequired by RemoteUpdateManager.isUpdateRequired.collectAsState()
    val remoteConfig by RemoteUpdateManager.remoteConfig.collectAsState()

    NavDisplay(
        backStack = backStack,
        onBack = {
            // Strictly forbid closing or going back if force update screen is active
            if (backStack.lastOrNull() != ForceUpdateNavKey && !isUpdateRequired) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<SplashNavKey> {
                SplashScreen(
                    onSplashFinished = { isRequired ->
                        backStack.clear()
                        if (isRequired || isUpdateRequired) {
                            backStack.add(ForceUpdateNavKey)
                        } else {
                            backStack.add(MainNavKey)
                        }
                    }
                )
            }
            entry<ForceUpdateNavKey> {
                ForceUpdateScreen(config = remoteConfig)
            }
            entry<MainNavKey> {
                if (isUpdateRequired) {
                    ForceUpdateScreen(config = remoteConfig)
                } else {
                    MainScreen()
                }
            }
        }
    )
}
