package com.creator.tiktokstudio.ui.main

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.AppLanguage
import com.creator.tiktokstudio.core.InstagramService
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.core.ThemeAndLocaleManager
import com.creator.tiktokstudio.data.StudioPlatform
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.screens.DownloaderScreen
import com.creator.tiktokstudio.ui.screens.InspectorScreen
import com.creator.tiktokstudio.ui.screens.InstagramStudioScreen
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val tiktokPagerState = rememberPagerState(pageCount = { 2 })

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurfaceElevated,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(DarkSurfaceElevated, DeepDarkBackground)
                            )
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // 1. Drawer Header (Glassmorphic 3D Emblem Card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TikTokDownloaderLogo(size = 40.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.drawerTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = strings.drawerSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3300F2FE))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = strings.drawerVersionBadge,
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. Main Platforms Section Title
                    Text(
                        text = strings.drawerSectionPlatforms,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TikTok Studio Platform Switcher
                    DrawerPlatformItem(
                        title = strings.drawerPlatformTikTok,
                        subtitle = strings.drawerPlatformTikTokDesc,
                        icon = { TikTokDownloaderLogo(size = 28.dp) },
                        isSelected = currentPlatform == StudioPlatform.TIKTOK,
                        activeBrush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonPink.copy(alpha = 0.25f))),
                        activeBorderColor = NeonCyan,
                        onClick = {
                            currentPlatform = StudioPlatform.TIKTOK
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instagram Studio Platform Switcher
                    DrawerPlatformItem(
                        title = strings.drawerPlatformInstagram,
                        subtitle = strings.drawerPlatformInstagramDesc,
                        icon = { RealInstagramIcon(size = 28.dp) },
                        isSelected = currentPlatform == StudioPlatform.INSTAGRAM,
                        activeBrush = Brush.horizontalGradient(listOf(Color(0x40833AB4), Color(0x40E1306C), Color(0x40FCB045))),
                        activeBorderColor = Color(0xFFE1306C),
                        onClick = {
                            currentPlatform = StudioPlatform.INSTAGRAM
                            coroutineScope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Divider
                    HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Quick Utilities Section Title
                    Text(
                        text = strings.drawerSectionUtilities,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Downloads History Item
                    DrawerUtilityItem(
                        title = strings.drawerDownloadsHistory,
                        icon = Icons.Default.History,
                        onClick = {
                            currentPlatform = StudioPlatform.TIKTOK
                            coroutineScope.launch {
                                tiktokPagerState.animateScrollToPage(0)
                                drawerState.close()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Clear Cache Item
                    DrawerUtilityItem(
                        title = strings.drawerClearCache,
                        icon = Icons.Default.DeleteSweep,
                        onClick = {
                            val freed = InstagramService.clearCache(context)
                            val freedMB = freed / (1024.0 * 1024.0)
                            Toast.makeText(context, "${strings.cacheClearedToast} (${String.format("%.1f", freedMB)} MB)", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DeepDarkBackground,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepDarkBackground)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Brand & Controls Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left Brand: Hamburger Menu, App Emblem & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            // Hamburger Menu Icon Button with Bounce Click
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .bounceClick {
                                        coroutineScope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = if (currentPlatform == StudioPlatform.TIKTOK) NeonCyan else Color(0xFFFCB045),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Dynamic Platform Logo
                            if (currentPlatform == StudioPlatform.TIKTOK) {
                                TikTokDownloaderLogo(size = 38.dp)
                            } else {
                                InstagramStudioLogo(size = 38.dp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentPlatform == StudioPlatform.TIKTOK) strings.appTitle else strings.igAppTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (currentPlatform == StudioPlatform.TIKTOK) strings.appSubtitle else strings.igAppSubtitle,
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
                                    .background(DarkSurfaceElevated)
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
                                        .background(DarkSurfaceElevated)
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
                }
            }
        }
    }
}

@Composable
fun FloatingGlassBottomDock(
    currentPlatform: StudioPlatform,
    onPlatformSelected: (StudioPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTikTok = currentPlatform == StudioPlatform.TIKTOK
    val targetFraction = if (isTikTok) 0f else 1f
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
            .padding(horizontal = 24.dp, vertical = 6.dp),
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
                                NeonCyan.copy(alpha = if (isTikTok) 0.6f else 0.2f),
                                Color(0xFFE1306C).copy(alpha = if (!isTikTok) 0.6f else 0.2f)
                            )
                        )
                    ),
                    RoundedCornerShape(30.dp)
                )
                .padding(4.dp)
        ) {
            val dockTabWidth = maxWidth / 2

            // Sliding Glowing Glass Capsule
            Box(
                modifier = Modifier
                    .offset(x = dockTabWidth * animatedFraction)
                    .width(dockTabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (isTikTok) {
                            Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.35f), NeonPink.copy(alpha = 0.35f)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0x55833AB4), Color(0x55E1306C), Color(0x55FCB045)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isTikTok) NeonCyan.copy(alpha = 0.8f) else Color(0xFFE1306C).copy(alpha = 0.8f),
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
                        TikTokDownloaderLogo(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TikTok Studio",
                            color = if (isTikTok) NeonCyan else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                        RealInstagramIcon(size = 22.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instagram",
                            color = if (!isTikTok) Color(0xFFE1306C) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerPlatformItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    activeBrush: Brush,
    activeBorderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) activeBrush else Brush.linearGradient(listOf(DarkSurfaceCard, DarkSurfaceCard)))
            .border(
                1.dp,
                if (isSelected) activeBorderColor else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .bounceClick(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(activeBorderColor)
                )
            }
        }
    }
}

@Composable
fun DrawerUtilityItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}



@Composable
fun NavTabItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background = if (isSelected) {
        Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OnAccentDark else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) OnAccentDark else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
