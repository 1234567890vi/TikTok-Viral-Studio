package com.creator.tiktokstudio.ui.main

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.R
import com.creator.tiktokstudio.core.AppLanguage
import com.creator.tiktokstudio.core.InstagramService
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.core.ThemeAndLocaleManager
import com.creator.tiktokstudio.data.StudioPlatform
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.screens.DownloaderScreen
import com.creator.tiktokstudio.ui.screens.InspectorScreen
import com.creator.tiktokstudio.ui.screens.InstagramStudioScreen
import com.creator.tiktokstudio.ui.screens.SnapchatStudioScreen
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = Localization.current
    val isDark by ThemeAndLocaleManager.isDarkMode.collectAsState()
    val currentLang by ThemeAndLocaleManager.currentLanguage.collectAsState()

    var currentPlatform by rememberSaveable { mutableStateOf(StudioPlatform.TIKTOK) }
    val coroutineScope = rememberCoroutineScope()

    val tiktokPagerState = rememberPagerState(pageCount = { 2 })

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic Full Wallpaper with Glass Blur (28.dp) & Dark Dimming Overlay Layer (55% Opacity)
        Crossfade(
            targetState = currentPlatform,
            label = "WallpaperCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { platform ->
            PlatformBlurredBackground(platform = platform)
        }

        // 2. Foreground Glassmorphic Interface Layer with Transparent Container
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Brand & Controls Bar (No Drawer Menu Button)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left Brand: App Emblem & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            // Dynamic Platform Logo
                            when (currentPlatform) {
                                StudioPlatform.TIKTOK -> TikTokDownloaderLogo(size = 40.dp)
                                StudioPlatform.INSTAGRAM -> InstagramStudioLogo(size = 40.dp)
                                StudioPlatform.SNAPCHAT -> SnapchatStudioLogo(size = 40.dp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val titleText = when (currentPlatform) {
                                    StudioPlatform.TIKTOK -> strings.appTitle
                                    StudioPlatform.INSTAGRAM -> strings.igAppTitle
                                    StudioPlatform.SNAPCHAT -> strings.snapAppTitle
                                }
                                val subtitleText = when (currentPlatform) {
                                    StudioPlatform.TIKTOK -> strings.appSubtitle
                                    StudioPlatform.INSTAGRAM -> strings.igAppSubtitle
                                    StudioPlatform.SNAPCHAT -> strings.snapAppSubtitle
                                }
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Controls: Theme Toggle + Language Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick Theme Toggle Button (Sun / Moon) with Bounce Click
                            IconButton(
                                onClick = { ThemeAndLocaleManager.toggleTheme(context) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .bounceClick { ThemeAndLocaleManager.toggleTheme(context) }
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDark) strings.themeLight else strings.themeDark,
                                    tint = if (isDark) GoldVip else NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Language Selection Dropdown
                            Box {
                                var showLangMenu by remember { mutableStateOf(false) }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .bounceClick { showLangMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "${currentLang.flag} ${currentLang.code.uppercase()}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = showLangMenu,
                                    onDismissRequest = { showLangMenu = false },
                                    modifier = Modifier
                                        .background(DarkSurfaceElevated)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                ) {
                                    AppLanguage.values().forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(lang.flag, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = lang.title,
                                                        color = if (lang == currentLang) NeonCyan else TextPrimary,
                                                        fontWeight = if (lang == currentLang) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                ThemeAndLocaleManager.setLanguage(context, lang)
                                                showLangMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Tabs for TikTok (Downloader, Inspector) with Fluid Spring Pill
                    if (currentPlatform == StudioPlatform.TIKTOK) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SpringPillTabBar(
                            selectedTab = tiktokPagerState.currentPage,
                            onTabSelected = { page ->
                                coroutineScope.launch {
                                    tiktokPagerState.animateScrollToPage(page)
                                }
                            },
                            tabs = listOf(
                                strings.tabDownloader to Icons.Default.Download,
                                strings.tabInspector to Icons.Default.Search
                            ),
                            activeGradient = Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
                        )
                    }
                }
            },
            bottomBar = {
                FloatingGlassBottomDock(
                    currentPlatform = currentPlatform,
                    onPlatformSelected = { currentPlatform = it }
                )
            }
        ) { innerPadding ->
            Crossfade(
                targetState = currentPlatform,
                label = "PlatformCrossfade",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { platform ->
                when (platform) {
                    StudioPlatform.TIKTOK -> {
                        HorizontalPager(
                            state = tiktokPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> DownloaderScreen(modifier = Modifier.fillMaxSize())
                                1 -> InspectorScreen(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    StudioPlatform.INSTAGRAM -> {
                        InstagramStudioScreen(modifier = Modifier.fillMaxSize())
                    }
                    StudioPlatform.SNAPCHAT -> {
                        SnapchatStudioScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Platform Blurred Icon Backgrounds (PlatformBlurredBackground)
 * Large platform icon in depth with glowing auras and heavy smooth blur (Modifier.blur(70.dp))
 * + dark glassmorphic layer for all 3 platforms (TikTok neon cyan/pink, Instagram sunset, Snapchat yellow/amber).
 */
@Composable
fun PlatformBlurredBackground(
    platform: StudioPlatform,
    modifier: Modifier = Modifier
) {
    when (platform) {
        StudioPlatform.TIKTOK -> TikTokWallpaperBackground(modifier)
        StudioPlatform.INSTAGRAM -> InstagramWallpaperBackground(modifier)
        StudioPlatform.SNAPCHAT -> SnapchatWallpaperBackground(modifier)
    }
}

/**
 * TikTok Official Full Wallpaper with 70.dp Ambient Blur & 55% Dark Dimming Layer
 */
@Composable
fun TikTokWallpaperBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.bg_tiktok_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )
        // Glowing Neon Cyan/Pink Aura with 70.dp blur
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.35f),
                            NeonPink.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(70.dp),
            contentAlignment = Alignment.Center
        ) {
            TikTokDownloaderLogo(size = 220.dp)
        }
        // 55% Dark Dimming Overlay Layer to ensure clean contrast over the blurred wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )
    }
}

/**
 * Instagram Official Full Wallpaper with 70.dp Ambient Blur & 55% Dark Dimming Layer
 */
@Composable
fun InstagramWallpaperBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.bg_instagram_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )
        // Glowing Sunset Aura with 70.dp blur
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE1306C).copy(alpha = 0.40f),
                            Color(0xFF833AB4).copy(alpha = 0.30f),
                            Color(0xFFFCB045).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(70.dp),
            contentAlignment = Alignment.Center
        ) {
            RealInstagramIcon(size = 220.dp)
        }
        // 55% Dark Dimming Overlay Layer to ensure clean contrast over the blurred wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )
    }
}

/**
 * Snapchat Official Full Wallpaper with 70.dp Ambient Blur & 55% Dark Dimming Layer
 */
@Composable
fun SnapchatWallpaperBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.bg_snapchat_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )
        // Glowing Electric Yellow/Gold Aura with 70.dp blur
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFC00).copy(alpha = 0.45f),
                            Color(0xFFFFB800).copy(alpha = 0.30f),
                            Color(0xFFF59E0B).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .blur(70.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_snapchat_official),
                contentDescription = null,
                modifier = Modifier.size(240.dp)
            )
        }
        // 55% Dark Dimming Overlay Layer to ensure clean contrast over the blurred wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )
    }
}

@Composable
fun FloatingGlassBottomDock(
    currentPlatform: StudioPlatform,
    onPlatformSelected: (StudioPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetFraction = when (currentPlatform) {
        StudioPlatform.TIKTOK -> 0f
        StudioPlatform.INSTAGRAM -> 1f
        StudioPlatform.SNAPCHAT -> 2f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dockSlide"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(DarkSurfaceCard.copy(alpha = 0.90f))
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = if (currentPlatform == StudioPlatform.TIKTOK) 0.6f else 0.2f),
                                Color(0xFFE1306C).copy(alpha = if (currentPlatform == StudioPlatform.INSTAGRAM) 0.6f else 0.2f),
                                Color(0xFFFFFC00).copy(alpha = if (currentPlatform == StudioPlatform.SNAPCHAT) 0.6f else 0.2f)
                            )
                        )
                    ),
                    RoundedCornerShape(30.dp)
                )
                .padding(4.dp)
        ) {
            val dockTabWidth = maxWidth / 3

            // Sliding Glowing Glass Capsule
            Box(
                modifier = Modifier
                    .offset(x = dockTabWidth * animatedFraction)
                    .width(dockTabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        when (currentPlatform) {
                            StudioPlatform.TIKTOK -> Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.35f), NeonPink.copy(alpha = 0.35f)))
                            StudioPlatform.INSTAGRAM -> Brush.horizontalGradient(listOf(Color(0x55833AB4), Color(0x55E1306C), Color(0x55FCB045)))
                            StudioPlatform.SNAPCHAT -> Brush.horizontalGradient(listOf(Color(0x55FFFC00), Color(0x55FFB800), Color(0x55F59E0B)))
                        }
                    )
                    .border(
                        1.dp,
                        when (currentPlatform) {
                            StudioPlatform.TIKTOK -> NeonCyan.copy(alpha = 0.8f)
                            StudioPlatform.INSTAGRAM -> Color(0xFFE1306C).copy(alpha = 0.8f)
                            StudioPlatform.SNAPCHAT -> Color(0xFFFFFC00).copy(alpha = 0.9f)
                        },
                        RoundedCornerShape(26.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // TikTok Studio Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .bounceClick { onPlatformSelected(StudioPlatform.TIKTOK) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TikTokDownloaderLogo(size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TikTok",
                            color = if (currentPlatform == StudioPlatform.TIKTOK) NeonCyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Instagram Studio Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .bounceClick { onPlatformSelected(StudioPlatform.INSTAGRAM) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RealInstagramIcon(size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Instagram",
                            color = if (currentPlatform == StudioPlatform.INSTAGRAM) Color(0xFFE1306C) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Snapchat Studio Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .bounceClick { onPlatformSelected(StudioPlatform.SNAPCHAT) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RealSnapchatIcon(size = 20.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snapchat",
                            color = if (currentPlatform == StudioPlatform.SNAPCHAT) Color(0xFFFFFC00) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
