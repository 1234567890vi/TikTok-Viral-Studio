package com.creator.tiktokstudio

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.creator.tiktokstudio.ui.main.MainScreen
import com.creator.tiktokstudio.ui.screens.SplashScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(SplashNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashNavKey> {
                SplashScreen(
                    onSplashFinished = {
                        backStack.clear()
                        backStack.add(MainNavKey)
                    }
                )
            }
            entry<MainNavKey> {
                MainScreen()
            }
        }
    )
}
