package com.creator.tiktokstudio.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.NativeEngine
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.screens.CreatorStudioScreen
import com.creator.tiktokstudio.ui.screens.DownloaderScreen
import com.creator.tiktokstudio.ui.screens.VipLoungeScreen
import com.creator.tiktokstudio.ui.theme.*

data class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavigationTab("التحميل الخارق", Icons.Default.Download, NeonCyan),
        NavigationTab("استوديو المحتوى", Icons.Default.AutoAwesome, NeonPink),
        NavigationTab("جناح VIP", Icons.Default.WorkspacePremium, GoldVip)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepDarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepDarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, NeonPink))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TS", color = DeepDarkBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Creator & TikTok Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "محرك النواة الهجين v3.4",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                        }
                    }

                    NeonBadge(
                        text = if (NativeEngine.isNativeLoaded()) "NDK C++ ⚡" else "Hybrid Engine",
                        textColor = NeonCyan,
                        borderColor = NeonCyan
                    )
                }
            }
        },
        bottomBar = {
            // Floating Glass Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.4f), NeonPink.copy(alpha = 0.4f)))
                            ),
                            RoundedCornerShape(34.dp)
                        ),
                    color = Color(0xEE161622),
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            val itemColor = if (isSelected) tab.accentColor else TextMuted

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        if (isSelected) tab.accentColor.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = itemColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            color = tab.accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 250),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tabCrossfade"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> DownloaderScreen()
                1 -> CreatorStudioScreen()
                2 -> VipLoungeScreen()
            }
        }
    }
}
