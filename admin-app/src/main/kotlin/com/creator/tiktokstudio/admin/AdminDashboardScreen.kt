package com.creator.tiktokstudio.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.admin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var devices by remember { mutableStateOf<List<DeviceTelemetry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentGlobalMode by remember { mutableStateOf("ACTIVE") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ONLINE, BANNED

    // OTA Version Updater State
    val isUpdateAvailable by AdminUpdateManager.isUpdateAvailable.collectAsState()
    val adminConfig by AdminUpdateManager.adminRemoteConfig.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Dialogs state
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceMsgInput by remember { mutableStateOf("نقوم حالياً بإجراء تحديثات وتحسينات دورية على الخوادم. سنعود قريباً!") }

    var showStopDialog by remember { mutableStateOf(false) }
    var stopMsgInput by remember { mutableStateOf("تم إيقاف تشغيل التطبيق مؤقتاً من قِبل إدارة النظام.") }

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastMsgInput by remember { mutableStateOf("") }

    var showAlertDialog by remember { mutableStateOf(false) }
    var alertTargetId by remember { mutableStateOf<String?>(null) }
    var alertTargetName by remember { mutableStateOf("") }
    var alertMsgInput by remember { mutableStateOf("") }

    fun refreshDevices() {
        coroutineScope.launch {
            isLoading = true
            val res = AdminCloudService.fetchDevices()
            isLoading = false
            res.onSuccess {
                devices = it
            }.onFailure {
                Toast.makeText(context, "فشل جلب الأجهزة: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-check for updates on start
    LaunchedEffect(Unit) {
        val hasUpdate = AdminUpdateManager.checkForUpdates(context)
        if (hasUpdate) {
            showUpdateDialog = true
        }
    }

    // Auto-refresh devices telemetry every 7 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val res = AdminCloudService.fetchDevices()
            res.onSuccess { devices = it }
            delay(7_000)
        }
    }

    // Pulse animation for update badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulseUpdatePill")
    val updatePillScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "updatePillScale"
    )

    // Filter devices
    val filteredDevices = remember(devices, selectedFilter) {
        when (selectedFilter) {
            "ONLINE" -> devices.filter { it.isOnline }
            "BANNED" -> devices.filter { it.isBanned }
            else -> devices
        }
    }

    // Stats
    val totalCount = devices.size
    val onlineCount = devices.count { it.isOnline }
    val bannedCount = devices.count { it.isBanned }
    val chargingCount = devices.count { it.isCharging }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(RoyalGold, NeonCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DeepDarkBackground)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("مركز قيادة الأدمن ⚡", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("متصل بالخادم السحابي اللحظي 🟢", fontSize = 10.sp, color = NeonGreen)
                        }
                    }
                },
                actions = {
                    // In-App Version Updater Icon
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val hasNew = AdminUpdateManager.checkForUpdates(context)
                            if (hasNew) {
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(context, "أنت تستخدم أحدث إصدار v${AdminUpdateManager.getCurrentVersionName(context)} ✅", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = "فحص التحديثات",
                                tint = if (isUpdateAvailable) RoyalGold else TextSecondary
                            )
                            if (isUpdateAvailable) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { refreshDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = NeonCyan)
                    }
                    IconButton(onClick = { onLogout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "خروج", tint = DangerRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurfaceCard)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Update Notification Banner (if available)
            if (isUpdateAvailable && adminConfig != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(updatePillScale)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Brush.horizontalGradient(listOf(RoyalGold, NeonCyan)), RoundedCornerShape(16.dp))
                            .clickable { showUpdateDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color(0xF01D1A2F))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.NewReleases, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تحديث تلقائي جديد متاح (v${adminConfig?.latestVersionName}) 🚀",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "انقر هنا للتثبيت الفوري بنقرة زر واحدة",
                                        fontSize = 11.sp,
                                        color = NeonCyan
                                    )
                                }
                            }
                            Button(
                                onClick = { showUpdateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("تحديث ⚡", color = DeepDarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Section 1: Global Service Status & Controls (إعدادات حالة الخدمة العامة)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, RoyalGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎛️ حالة الخدمة العامة والتحكم المركزي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalGold
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (currentGlobalMode) {
                                    "ACTIVE" -> NeonGreen.copy(alpha = 0.2f)
                                    "MAINTENANCE" -> WarningAmber.copy(alpha = 0.2f)
                                    else -> DangerRed.copy(alpha = 0.2f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (currentGlobalMode) {
                                        "ACTIVE" -> NeonGreen
                                        "MAINTENANCE" -> WarningAmber
                                        else -> DangerRed
                                    }
                                )
                            ) {
                                Text(
                                    text = when (currentGlobalMode) {
                                        "ACTIVE" -> "🟢 تشغيل عادي"
                                        "MAINTENANCE" -> "🛠️ وضع الصيانة"
                                        else -> "🛑 متوقف"
                                    },
                                    color = when (currentGlobalMode) {
                                        "ACTIVE" -> NeonGreen
                                        "MAINTENANCE" -> WarningAmber
                                        else -> DangerRed
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: Active & Maintenance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = AdminCloudService.sendCommand("active")
                                        if (res.isSuccess) {
                                            currentGlobalMode = "ACTIVE"
                                            Toast.makeText(context, "تم تفعيل وضع التشغيل العادي للجميع 🟢", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E676)),
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("🟢 وضع التشغيل العادي", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showMaintenanceDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFAB00)),
                                border = BorderStroke(1.dp, WarningAmber),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("🛠️ وضع الصيانة", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Emergency Stop & Broadcast Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showStopDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                                border = BorderStroke(1.dp, DangerRed),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("🛑 إيقاف الخدمة", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showBroadcastDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF007F)),
                                border = BorderStroke(1.dp, NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("📢 إشعار للنظام", color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 3: Restart All
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = AdminCloudService.sendCommand("force_restart", targetAndroidId = "all")
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "تم إرسال أمر إعادة التشغيل لجميع الهواتف المتصلة 🔄", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300F2FE)),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Text("🔄 إعادة تشغيل جميع الأجهزة النشطة", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 2: Active Sessions Analytics Overview
            item {
                Column {
                    Text(
                        text = "📊 تحليلات الجلسات النشطة (Active Sessions Analytics)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoyalGold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(title = "إجمالي الأجهزة", count = totalCount.toString(), color = RoyalGold, icon = Icons.Default.Devices, modifier = Modifier.weight(1f))
                        StatCard(title = "المتصلين الآن", count = onlineCount.toString(), color = NeonGreen, icon = Icons.Default.Wifi, modifier = Modifier.weight(1f))
                        StatCard(title = "يشحنون الآن", count = chargingCount.toString(), color = NeonCyan, icon = Icons.Default.Bolt, modifier = Modifier.weight(1f))
                        StatCard(title = "المحظورين", count = bannedCount.toString(), color = DangerRed, icon = Icons.Default.Block, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Section 3: Filters Row & Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        text = "📱 قائمة الأجهزة المتصلة (${filteredDevices.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("الكل ($totalCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold.copy(alpha = 0.25f),
                                selectedLabelColor = RoyalGold
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "ONLINE",
                            onClick = { selectedFilter = "ONLINE" },
                            label = { Text("المتصلين 🟢 ($onlineCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen.copy(alpha = 0.25f),
                                selectedLabelColor = NeonGreen
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "BANNED",
                            onClick = { selectedFilter = "BANNED" },
                            label = { Text("المحظورين 🚫 ($bannedCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRed.copy(alpha = 0.25f),
                                selectedLabelColor = DangerRed
                            )
                        )
                    }
                }
            }

            if (filteredDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhonelinkRing, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد أجهزة مطابقة في هذا التصنيف حالياً",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Text(
                                text = "بمجرد أن يتصل أي مستخدم بالإنترنت ستظهر بياناته الحية هنا فوراً.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                            )
                        }
                    }
                }
            } else {
                items(filteredDevices, key = { it.androidId }) { dev ->
                    DeviceCard(
                        device = dev,
                        onBanToggle = {
                            coroutineScope.launch {
                                val action = if (dev.isBanned) "unban" else "ban"
                                val res = AdminCloudService.sendCommand(action, targetAndroidId = dev.androidId)
                                if (res.isSuccess) {
                                    refreshDevices()
                                    Toast.makeText(context, if (dev.isBanned) "تم فك الحظر" else "تم حظر الجهاز 🚫", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRestart = {
                            coroutineScope.launch {
                                val res = AdminCloudService.sendCommand("force_restart", targetAndroidId = dev.androidId)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "تم إرسال أمر إعادة التشغيل لهاتف ${dev.deviceModel} 🔄", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSendAlert = {
                            alertTargetId = dev.androidId
                            alertTargetName = dev.deviceModel
                            alertMsgInput = ""
                            showAlertDialog = true
                        },
                        onDelete = {
                            AdminCloudService.deleteDeviceLocally(dev.androidId)
                            refreshDevices()
                        }
                    )
                }
            }
        }
    }

    // OTA Version Updater Dialog
    if (showUpdateDialog && adminConfig != null) {
        AdminUpdateDialog(
            config = adminConfig!!,
            onDismissRequest = { showUpdateDialog = false }
        )
    }

    // Maintenance Dialog
    if (showMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceDialog = false },
            title = { Text("🛠️ تفعيل وضع الصيانة للجميع") },
            text = {
                Column {
                    Text("اكتب الرسالة التي ستظهر للمستخدمين داخل شاشة الصيانة:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maintenanceMsgInput,
                        onValueChange = { maintenanceMsgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMaintenanceDialog = false
                        coroutineScope.launch {
                            val res = AdminCloudService.sendCommand("maintenance", message = maintenanceMsgInput)
                            if (res.isSuccess) {
                                currentGlobalMode = "MAINTENANCE"
                                Toast.makeText(context, "تم تفعيل وضع الصيانة 🛠️", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("تطبيق وضع الصيانة", color = DeepDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaintenanceDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Stop / Emergency Kill Switch Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("🛑 إيقاف الخدمة الشامل (Emergency Stop)") },
            text = {
                Column {
                    Text("اكتب رسالة الإيقاف التي ستظهر لجميع المستخدمين على الشاشة التحذيرية:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopMsgInput,
                        onValueChange = { stopMsgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        coroutineScope.launch {
                            val res = AdminCloudService.sendCommand("stopped", message = stopMsgInput)
                            if (res.isSuccess) {
                                currentGlobalMode = "STOPPED"
                                Toast.makeText(context, "تم إيقاف التطبيق للجميع 🛑", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("إيقاف الخدمة الآن", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // System Broadcast Notification Dialog
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("📢 إشعار النظام الفوري (Broadcast Notification)") },
            text = {
                Column {
                    Text("ستظهر هذه الرسالة لجميع مستخدمي التطبيق في نافذة منبثقة نيونية على أجهزتهم:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = broadcastMsgInput,
                        onValueChange = { broadcastMsgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("اكتب رسالة النظام لجميع المستخدمين هنا...") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBroadcastDialog = false
                        coroutineScope.launch {
                            val res = AdminCloudService.sendCommand("alert", message = broadcastMsgInput, targetAndroidId = "all")
                            if (res.isSuccess) {
                                Toast.makeText(context, "تم إرسال إشعار النظام لجميع المستخدمين 🚀", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Text("إرسال للجميع الآن 📢", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Send Specific Alert Dialog
    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("💬 إرسال رسالة مخصصة إلى: $alertTargetName") },
            text = {
                Column {
                    Text("ستظهر هذه الرسالة في نافذة منبثقة فورية على شاشة هذا الجهاز تحديداً:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alertMsgInput,
                        onValueChange = { alertMsgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("اكتب الرسالة هنا...") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = alertTargetId ?: "all"
                        showAlertDialog = false
                        coroutineScope.launch {
                            val res = AdminCloudService.sendCommand("alert", message = alertMsgInput, targetAndroidId = target)
                            if (res.isSuccess) {
                                Toast.makeText(context, "تم إرسال الرسالة بنجاح 🚀", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("إرسال الآن", color = DeepDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceTelemetry,
    onBanToggle: () -> Unit,
    onRestart: () -> Unit,
    onSendAlert: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (device.isBanned) DangerRed else if (device.isOnline) NeonCyan.copy(alpha = 0.5f) else Color(0x33FFFFFF)

    // Last seen calculation
    val diffMs = System.currentTimeMillis() - device.timestamp
    val lastSeenText = when {
        diffMs < 30_000 -> "الآن 🟢"
        diffMs < 60_000 -> "منذ 30 ثانية"
        diffMs < 3600_000 -> "منذ ${(diffMs / 60_000).toInt()} دقيقة"
        else -> "منذ ${(diffMs / 3600_000).toInt()} ساعة"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Device Model & Manufacturer & Online Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (device.isOnline) NeonCyan.copy(alpha = 0.2f) else Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = if (device.isOnline) NeonCyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${device.manufacturer} ${device.deviceModel}".trim(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Android ${device.androidVersion}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RoyalGold.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, RoyalGold.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v${device.appVersion}",
                                    fontSize = 10.sp,
                                    color = RoyalGold,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (device.isBanned) DangerRed.copy(alpha = 0.2f)
                    else if (device.isOnline) NeonGreen.copy(alpha = 0.2f)
                    else Color(0x22888888),
                    border = BorderStroke(
                        1.dp,
                        if (device.isBanned) DangerRed else if (device.isOnline) NeonGreen else Color(0x44888888)
                    )
                ) {
                    Text(
                        text = if (device.isBanned) "🚫 محظور" else if (device.isOnline) "🟢 متصل" else "⚪ $lastSeenText",
                        color = if (device.isBanned) DangerRed else if (device.isOnline) NeonGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery and Power State
            val batteryColor = when {
                device.batteryPct > 50 -> NeonGreen
                device.batteryPct > 20 -> WarningAmber
                else -> DangerRed
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x25000000))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (device.isCharging) Icons.Default.Bolt else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (device.isCharging) "حالة الطاقة: يشحن الآن ⚡" else "حالة الطاقة: على البطارية 🔋",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${device.batteryPct}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (device.batteryPct.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = batteryColor,
                        trackColor = Color(0x332E2E3E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Android ID row with copyable look
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ID: ${device.androidId}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "آخر ظهور: $lastSeenText",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ban / Unban
                Button(
                    onClick = onBanToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isBanned) NeonGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, if (device.isBanned) NeonGreen else DangerRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (device.isBanned) "فك الحظر" else "حظر 🚫",
                        color = if (device.isBanned) NeonGreen else DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Restart
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("ريستارت 🔄", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Alert message
                Button(
                    onClick = onSendAlert,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, RoyalGold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("إشعار 💬", color = RoyalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
