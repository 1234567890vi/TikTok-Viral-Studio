package com.creator.tiktokstudio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.creator.tiktokstudio.core.RemoteUpdateManager
import com.creator.tiktokstudio.ui.main.MainScreen
import com.creator.tiktokstudio.ui.screens.ForceUpdateScreen
import com.creator.tiktokstudio.ui.screens.SplashScreen

@Composable
fun MainNavigation() {
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
