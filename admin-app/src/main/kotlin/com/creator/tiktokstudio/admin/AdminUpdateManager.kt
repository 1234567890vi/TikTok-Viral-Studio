package com.creator.tiktokstudio.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AdminRemoteConfig(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val apkUrl: String = "",
    val updateTitle: String = "🚀 تحديث جديد للوحة الإدارة متاح!",
    val updateMessage: String = "يتوفر إصدار جديد يتضمن تحسينات ومزايا جديدة للمشرفين.",
    val changelog: List<String> = emptyList()
)

data class AdminDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val speedMBs: Double = 0.0,
    val downloadedMB: Double = 0.0,
    val totalMB: Double = 0.0,
    val isReadyToInstall: Boolean = false,
    val error: String? = null
)

object AdminUpdateManager {
    private const val TAG = "AdminUpdateManager"
    private const val REMOTE_CONFIG_URL =
        "https://raw.githubusercontent.com/1234567890vi/TikTok-Viral-Studio/main/remote_config.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _adminRemoteConfig = MutableStateFlow<AdminRemoteConfig?>(null)
    val adminRemoteConfig: StateFlow<AdminRemoteConfig?> = _adminRemoteConfig.asStateFlow()

    private val _downloadState = MutableStateFlow(AdminDownloadState())
    val downloadState: StateFlow<AdminDownloadState> = _downloadState.asStateFlow()

    private var downloadedApkFile: File? = null

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Checking for Admin App updates from remote config...")
            val request = Request.Builder()
                .url(REMOTE_CONFIG_URL)
                .addHeader("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.w(TAG, "Failed to fetch remote config: HTTP ${response.code}")
                return@withContext false
            }

            val jsonStr = response.body!!.string()
            val json = JSONObject(jsonStr)

            // Extract admin specific version fields or fallback to main
            val latestCode = if (json.has("admin_latest_version_code")) {
                json.optInt("admin_latest_version_code", 1)
            } else {
                json.optInt("latest_version_code", 1)
            }

            val latestName = if (json.has("admin_latest_version_name")) {
                json.optString("admin_latest_version_name", "1.0.0")
            } else {
                json.optString("latest_version_name", "1.0.0")
            }

            val apkUrl = when {
                json.has("admin_apk_url") && json.optString("admin_apk_url").isNotBlank() ->
                    json.optString("admin_apk_url")
                json.has("admin_apk_download_url") && json.optString("admin_apk_download_url").isNotBlank() ->
                    json.optString("admin_apk_download_url")
                else ->
                    "https://github.com/1234567890vi/TikTok-Viral-Studio/releases/download/v$latestName/TikTok-Studio-Admin-v$latestName.apk"
            }

            val title = json.optString("update_title_ar", "🚀 تحديث جديد للوحة الإدارة mتاح!")
            val message = json.optString("update_message_ar", "يتوفر إصدار جديد يتضمن تحسينات ومزايا جديدة للمشرفين.")

            val changelogList = mutableListOf<String>()
            val changelogArray = json.optJSONArray("changelog_ar")
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    changelogList.add(changelogArray.optString(i))
                }
            }

            val config = AdminRemoteConfig(
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                apkUrl = apkUrl,
                updateTitle = title,
                updateMessage = message,
                changelog = changelogList
            )
            _adminRemoteConfig.value = config

            val currentCode = getCurrentVersionCode(context)
            val hasNewVersion = latestCode > currentCode
            _isUpdateAvailable.value = hasNewVersion

            Log.i(TAG, "Current Version: $currentCode, Remote Latest Admin: $latestCode, Update Available: $hasNewVersion")
            return@withContext hasNewVersion
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin update: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun startInAppDownloadAndInstall(context: Context) = withContext(Dispatchers.IO) {
        val config = _adminRemoteConfig.value
        val url = config?.apkUrl?.takeIf { it.isNotBlank() }
            ?: "https://github.com/1234567890vi/TikTok-Viral-Studio/releases/download/v1.9.0/TikTok-Studio-Admin-v1.9.0.apk"

        try {
            _downloadState.value = AdminDownloadState(isDownloading = true, progress = 0.05f)

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TikTokStudio-Admin-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                throw Exception("فشل الاتصال بسيرفر التحميل: ${response.code}")
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val targetApk = File(updateDir, "TikTok-Studio-Admin-Update_${System.currentTimeMillis()}.apk")

            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceSample = 0L

            FileOutputStream(targetApk).use { output ->
                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        bytesSinceSample += bytesRead

                        val now = System.currentTimeMillis()
                        val delta = now - lastSampleTime
                        if (delta >= 200) {
                            val speed = if (delta > 0) (bytesSinceSample.toDouble() / (1024.0 * 1024.0)) / (delta.toDouble() / 1000.0) else 0.0
                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.65f
                            }

                            _downloadState.value = AdminDownloadState(
                                isDownloading = true,
                                progress = progress,
                                speedMBs = speed,
                                downloadedMB = totalBytesRead.toDouble() / (1024.0 * 1024.0),
                                totalMB = if (contentLength > 0) contentLength.toDouble() / (1024.0 * 1024.0) else (totalBytesRead.toDouble() / (1024.0 * 1024.0)),
                                isReadyToInstall = false
                            )

                            lastSampleTime = now
                            bytesSinceSample = 0L
                        }
                    }
                }
            }

            downloadedApkFile = targetApk
            _downloadState.value = AdminDownloadState(
                isDownloading = false,
                progress = 1.0f,
                downloadedMB = totalBytesRead.toDouble() / (1024.0 * 1024.0),
                totalMB = totalBytesRead.toDouble() / (1024.0 * 1024.0),
                isReadyToInstall = true
            )

            // Trigger installer immediately
            launchApkInstaller(context, targetApk)
        } catch (e: Exception) {
            Log.e(TAG, "Download admin update failed: ${e.message}", e)
            _downloadState.value = AdminDownloadState(
                isDownloading = false,
                progress = 0f,
                error = e.localizedMessage ?: "حدث خطأ أثناء تحميل ملف التحديث"
            )
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File? = downloadedApkFile) {
        val file = apkFile ?: downloadedApkFile ?: return
        if (!file.exists()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch admin APK installer: ${e.message}", e)
        }
    }
}
