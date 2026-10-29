package com.creator.tiktokstudio.admin

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DeviceTelemetry(
    val androidId: String,
    val manufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val batteryPct: Int,
    val isCharging: Boolean,
    val appVersion: String,
    val isBanned: Boolean,
    val currentStatus: String,
    val timestamp: Long
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - timestamp) < 120_000 // Seen in last 2 minutes
}

object AdminCloudService {
    private const val TAG = "AdminCloudService"
    private const val TELEMETRY_URL = "https://ntfy.sh/tiktok_studio_cloud_telemetry_2026/json?poll=1&since=12h"
    private const val COMMANDS_URL = "https://ntfy.sh/tiktok_studio_cloud_commands_2026"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Locally tracked overrides (bans & deletions)
    private val localBannedIds = mutableSetOf<String>()
    private val deletedIds = mutableSetOf<String>()

    suspend fun fetchDevices(): Result<List<DeviceTelemetry>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(TELEMETRY_URL)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()

            if (body.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val deviceMap = mutableMapOf<String, DeviceTelemetry>()
            val lines = body.trim().split("\n")

            for (line in lines) {
                if (line.isBlank()) continue
                try {
                    val root = JSONObject(line)
                    val msgStr = root.optString("message", "")
                    if (msgStr.isBlank() || !msgStr.startsWith("{")) continue

                    val item = JSONObject(msgStr)
                    val id = item.optString("androidId", "")
                    if (id.isBlank() || deletedIds.contains(id)) continue

                    val time = item.optLong("timestamp", 0L)
                    val existing = deviceMap[id]

                    if (existing == null || time > existing.timestamp) {
                        val isBanned = localBannedIds.contains(id) || item.optBoolean("isBanned", false)
                        deviceMap[id] = DeviceTelemetry(
                            androidId = id,
                            manufacturer = item.optString("manufacturer", "Unknown"),
                            deviceModel = item.optString("deviceModel", "Android Device"),
                            androidVersion = item.optString("androidVersion", "Unknown"),
                            batteryPct = item.optInt("batteryPct", 50),
                            isCharging = item.optBoolean("isCharging", false),
                            appVersion = item.optString("appVersion", "1.3.0"),
                            isBanned = isBanned,
                            currentStatus = item.optString("currentStatus", "ACTIVE"),
                            timestamp = time
                        )
                    }
                } catch (_: Exception) {}
            }

            val resultList = deviceMap.values.sortedByDescending { it.timestamp }
            Result.success(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching devices: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendCommand(
        command: String,
        message: String = "",
        targetAndroidId: String = "all"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("command", command.lowercase())
                put("message", message)
                put("targetAndroidId", targetAndroidId)
                put("timestamp", System.currentTimeMillis())
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(COMMANDS_URL)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()

            if (command.equals("ban", ignoreCase = true) && targetAndroidId != "all") {
                localBannedIds.add(targetAndroidId)
            } else if (command.equals("unban", ignoreCase = true) && targetAndroidId != "all") {
                localBannedIds.remove(targetAndroidId)
            }

            Result.success(isSuccess)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun deleteDeviceLocally(androidId: String) {
        deletedIds.add(androidId)
    }
}
