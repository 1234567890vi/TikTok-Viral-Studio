package com.creator.tiktokstudio.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

enum class AppControlState {
    ACTIVE,
    MAINTENANCE,
    STOPPED
}

object TelemetryAndCommandService {
    private const val TAG = "TelemetryService"
    private const val PREFS_NAME = "tiktok_studio_admin_control"
    private const val KEY_IS_BANNED = "is_banned"
    private const val KEY_LAST_STATUS = "last_status"
    private const val KEY_LAST_MESSAGE = "last_message"

    private const val TELEMETRY_URL = "https://ntfy.sh/tiktok_studio_cloud_telemetry_2026"
    private const val COMMANDS_URL = "https://ntfy.sh/tiktok_studio_cloud_commands_2026/json?poll=1&since=60s"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _controlState = MutableStateFlow(AppControlState.ACTIVE)
    val controlState: StateFlow<AppControlState> = _controlState.asStateFlow()

    private val _controlMessage = MutableStateFlow("")
    val controlMessage: StateFlow<String> = _controlMessage.asStateFlow()

    private val _isDeviceBanned = MutableStateFlow(false)
    val isDeviceBanned: StateFlow<Boolean> = _isDeviceBanned.asStateFlow()

    private val _pendingAlertMessage = MutableStateFlow<String?>(null)
    val pendingAlertMessage: StateFlow<String?> = _pendingAlertMessage.asStateFlow()

    private var isStarted = false
    private var serviceJob: Job? = null
    private var lastCommandTimestamp = System.currentTimeMillis() - 10_000

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDeviceBanned.value = prefs.getBoolean(KEY_IS_BANNED, false)
        val savedStatus = prefs.getString(KEY_LAST_STATUS, "ACTIVE") ?: "ACTIVE"
        val savedMessage = prefs.getString(KEY_LAST_MESSAGE, "") ?: ""
        _controlMessage.value = savedMessage
        _controlState.value = try {
            AppControlState.valueOf(savedStatus)
        } catch (_: Exception) {
            AppControlState.ACTIVE
        }

        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            // Heartbeat telemetry loop
            launch {
                while (isActive) {
                    sendDeviceTelemetry(context)
                    delay(25_000)
                }
            }

            // Command listening loop
            launch {
                while (isActive) {
                    listenForCommands(context)
                    delay(4_000)
                }
            }
        }
    }

    private fun sendDeviceTelemetry(context: Context) {
        try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            val androidVer = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            // Battery status
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 50

            val chargeStatus = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    chargeStatus == BatteryManager.BATTERY_STATUS_FULL

            val payload = JSONObject().apply {
                put("androidId", androidId)
                put("manufacturer", manufacturer)
                put("deviceModel", "$manufacturer $model")
                put("androidVersion", androidVer)
                put("batteryPct", batteryPct)
                put("isCharging", isCharging)
                put("appVersion", "1.3.0")
                put("isBanned", _isDeviceBanned.value)
                put("currentStatus", _controlState.value.name)
                put("timestamp", System.currentTimeMillis())
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(TELEMETRY_URL)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send telemetry: ${e.message}")
        }
    }

    private fun listenForCommands(context: Context) {
        try {
            val request = Request.Builder()
                .url(COMMANDS_URL)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()

            if (body.isBlank()) return

            val lines = body.trim().split("\n")
            val myAndroidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

            for (line in lines) {
                if (line.isBlank()) continue
                try {
                    val root = JSONObject(line)
                    val messageStr = root.optString("message", "")
                    if (messageStr.isBlank() || !messageStr.startsWith("{")) continue

                    val cmdObj = JSONObject(messageStr)
                    val cmdTime = cmdObj.optLong("timestamp", 0L)
                    if (cmdTime <= lastCommandTimestamp) continue
                    lastCommandTimestamp = cmdTime

                    val command = cmdObj.optString("command", "").lowercase()
                    val targetId = cmdObj.optString("targetAndroidId", "all")
                    val msgText = cmdObj.optString("message", "")

                    val isTargetedAtMe = targetId == "all" || targetId.equals(myAndroidId, ignoreCase = true)

                    if (!isTargetedAtMe && command != "active" && command != "maintenance" && command != "stopped") {
                        continue
                    }

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                    when (command) {
                        "active" -> {
                            _controlState.value = AppControlState.ACTIVE
                            _controlMessage.value = ""
                            prefs.edit().putString(KEY_LAST_STATUS, "ACTIVE").putString(KEY_LAST_MESSAGE, "").apply()
                        }
                        "maintenance" -> {
                            _controlState.value = AppControlState.MAINTENANCE
                            _controlMessage.value = if (msgText.isNotBlank()) msgText else "يجري الآن تحديث خوادم التطبيق وصيانتها مؤقتاً."
                            prefs.edit().putString(KEY_LAST_STATUS, "MAINTENANCE").putString(KEY_LAST_MESSAGE, _controlMessage.value).apply()
                        }
                        "stopped" -> {
                            _controlState.value = AppControlState.STOPPED
                            _controlMessage.value = if (msgText.isNotBlank()) msgText else "تم إيقاف التطبيق نهائياً من قِبل الإدارة."
                            prefs.edit().putString(KEY_LAST_STATUS, "STOPPED").putString(KEY_LAST_MESSAGE, _controlMessage.value).apply()
                        }
                        "force_restart" -> {
                            if (isTargetedAtMe) {
                                restartApp(context)
                            }
                        }
                        "ban" -> {
                            if (targetId.equals(myAndroidId, ignoreCase = true)) {
                                _isDeviceBanned.value = true
                                prefs.edit().putBoolean(KEY_IS_BANNED, true).apply()
                            }
                        }
                        "unban" -> {
                            if (targetId.equals(myAndroidId, ignoreCase = true)) {
                                _isDeviceBanned.value = false
                                prefs.edit().putBoolean(KEY_IS_BANNED, false).apply()
                            }
                        }
                        "alert" -> {
                            if (isTargetedAtMe && msgText.isNotBlank()) {
                                _pendingAlertMessage.value = msgText
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for commands: ${e.message}")
        }
    }

    fun restartApp(context: Context) {
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val restartPendingIntent = PendingIntent.getActivity(
                    context,
                    123456,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 600,
                    restartPendingIntent
                )
                exitProcess(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart: ${e.message}")
        }
    }

    fun clearAlert() {
        _pendingAlertMessage.value = null
    }
}
