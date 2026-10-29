package com.creator.tiktokstudio.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    // Dialog state
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceMsgInput by remember { mutableStateOf("نقوم حالياً بإجراء تحديثات وتحسينات دورية على الخوادم. سنعود قريباً!") }

    var showStopDialog by remember { mutableStateOf(false) }
    var stopMsgInput by remember { mutableStateOf("تم إيقاف تشغيل التطبيق مؤقتاً من قِبل إدارة النظام.") }

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

    // Auto-refresh loop every 8 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val res = AdminCloudService.fetchDevices()
            res.onSuccess { devices = it }
            delay(8_000)
        }
    }

    // Stats
    val totalCount = devices.size
    val onlineCount = devices.count { it.isOnline }
    val bannedCount = devices.count { it.isBanned }

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
                            Text("مركز قيادة الأدمن", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("متصل بالخادم السحابي اللحظي 🟢", fontSize = 11.sp, color = NeonGreen)
                        }
                    }
                },
                actions = {
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
            // Section 1: Global Control Center
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
                                text = "🎛️ مركز التحكم الشامل بالتطبيق",
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
                                        "ACTIVE" -> "🟢 نشط للجميع"
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

                        // Grid of Global Actions
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
                                            Toast.makeText(context, "تم تفعيل التطبيق للجميع 🟢", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E676)),
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("🟢 تفعيل للجميع", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text("🛑 إيقاف فوري", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = AdminCloudService.sendCommand("force_restart", targetAndroidId = "all")
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "تم إرسال أمر إعادة التشغيل لجميع الهواتف 🔄", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300F2FE)),
                                border = BorderStroke(1.dp, NeonCyan),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text("🔄 ريستارت للكل", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 2: Stats Counter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(title = "إجمالي الأجهزة", count = totalCount.toString(), color = RoyalGold, modifier = Modifier.weight(1f))
                    StatCard(title = "المتصلين الآن", count = onlineCount.toString(), color = NeonGreen, modifier = Modifier.weight(1f))
                    StatCard(title = "المحظورين", count = bannedCount.toString(), color = DangerRed, modifier = Modifier.weight(1f))
                }
            }

            // Section 3: Live Device Cards
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "📱 الأجهزة والمستخدمين المتصلين (${devices.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonCyan)
                    }
                }
            }

            if (devices.isEmpty()) {
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
                                text = "لا توجد أجهزة متصلة حالياً",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Text(
                                text = "بمجرد أن يفتح أي مستخدم التطبيق ستظهر بياناته هنا فوراً.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                            )
                        }
                    }
                }
            } else {
                items(devices, key = { it.androidId }) { dev ->
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
                    Text("تطبيق الصيانة", color = DeepDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaintenanceDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Stop / Kill Switch Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("🛑 إيقاف التطبيق نهائياً (Kill Switch)") },
            text = {
                Column {
                    Text("اكتب رسالة الإيقاف التي ستظهر لجميع المستخدمين على الشاشة الحمراء:", fontSize = 12.sp, color = TextSecondary)
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
                                Toast.makeText(context, "تم إيقاف التطبيق 🛑", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("إيقاف فوري", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Send Specific Alert Dialog
    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("💬 إرسال رسالة فورية إلى: $alertTargetName") },
            text = {
                Column {
                    Text("ستظهر هذه الرسالة في نافذة منبثقة فورية على شاشة هذا المستخدم:", fontSize = 12.sp, color = TextSecondary)
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Device Model & Online Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (device.isOnline) NeonCyan.copy(alpha = 0.2f) else Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = if (device.isOnline) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = device.deviceModel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Android ${device.androidVersion} • v${device.appVersion}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
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
                        text = if (device.isBanned) "🚫 محظور" else if (device.isOnline) "🟢 متصل" else "⚪ غير متصل",
                        color = if (device.isBanned) DangerRed else if (device.isOnline) NeonGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery Progress Row
            val batteryColor = when {
                device.batteryPct > 50 -> NeonGreen
                device.batteryPct > 20 -> WarningAmber
                else -> DangerRed
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (device.isCharging) "⚡ ${device.batteryPct}% (يشحن)" else "🔋 ${device.batteryPct}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = batteryColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { (device.batteryPct.coerceIn(0, 100) / 100f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = batteryColor,
                    trackColor = Color(0x332E2E3E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Android ID display
            Text(
                text = "ID: ${device.androidId}",
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ban/Unban
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
                    Text("إعادة تشغيل 🔄", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Text("رسالة 💬", color = RoyalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
