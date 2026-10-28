package com.creator.tiktokstudio.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.creator.tiktokstudio.data.CreatorStudioRepository
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

data class RemoteConfig(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val minSupportedVersionCode: Int = 1,
    val apkDownloadUrl: String = "",
    val updateTitleAr: String = "🚀 تحديث إجباري جديد متاح!",
    val updateMessageAr: String = "يجب تثبيت هذا التحديث للاستمرار في استخدام التطبيق.",
    val changelogAr: List<String> = emptyList(),
    val vipCodes: List<String> = emptyList()
)

data class UpdateDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val speedMBs: Double = 0.0,
    val downloadedMB: Double = 0.0,
    val totalMB: Double = 0.0,
    val isReadyToInstall: Boolean = false,
    val error: String? = null
)

object RemoteUpdateManager {
    private const val TAG = "RemoteUpdateManager"
    private const val REMOTE_CONFIG_URL =
        "https://raw.githubusercontent.com/1234567890vi/TikTok-Viral-Studio/main/remote_config.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _isUpdateRequired = MutableStateFlow(false)
    val isUpdateRequired: StateFlow<Boolean> = _isUpdateRequired.asStateFlow()

    private val _remoteConfig = MutableStateFlow<RemoteConfig?>(null)
    val remoteConfig: StateFlow<RemoteConfig?> = _remoteConfig.asStateFlow()

    private val _downloadState = MutableStateFlow(UpdateDownloadState())
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private var downloadedApkFile: File? = null

    suspend fun checkForRemoteUpdate(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Fetching remote configuration from GitHub...")
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

            val latestCode = json.optInt("latest_version_code", 1)
            val latestName = json.optString("latest_version_name", "1.0.0")
            val forceUpdate = json.optBoolean("force_update", false)
            val minSupportedCode = json.optInt("min_supported_version_code", 1)
            val downloadUrl = json.optString("apk_download_url", "")
            val titleAr = json.optString("update_title_ar", "🚀 تحديث إجباري جديد متاح!")
            val messageAr = json.optString("update_message_ar", "")

            val changelogList = mutableListOf<String>()
            val changelogArray = json.optJSONArray("changelog_ar")
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    changelogList.add(changelogArray.optString(i))
                }
            }

            val vipCodesList = mutableListOf<String>()
            val vipArray = json.optJSONArray("vip_codes")
            if (vipArray != null) {
                for (i in 0 until vipArray.length()) {
                    vipCodesList.add(vipArray.optString(i))
                }
            }

            val config = RemoteConfig(
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                forceUpdate = forceUpdate,
                minSupportedVersionCode = minSupportedCode,
                apkDownloadUrl = downloadUrl,
                updateTitleAr = titleAr,
                updateMessageAr = messageAr,
                changelogAr = changelogList,
                vipCodes = vipCodesList
            )

            _remoteConfig.value = config

            // Check against current package version code
            val currentVersionCode = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }
            } catch (e: Exception) {
                1
            }

            Log.i(TAG, "Current Version: $currentVersionCode, Remote Latest: $latestCode, Force: $forceUpdate")

            val isBlockingRequired = (forceUpdate && (latestCode > currentVersionCode || currentVersionCode < minSupportedCode))
            _isUpdateRequired.value = isBlockingRequired
            return@withContext isBlockingRequired
        } catch (e: Exception) {
            Log.e(TAG, "Error checking remote update: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun startInAppDownloadAndInstall(context: Context) = withContext(Dispatchers.IO) {
        val config = _remoteConfig.value
        val url = config?.apkDownloadUrl?.takeIf { it.isNotBlank() }
            ?: "https://github.com/1234567890vi/TikTok-Viral-Studio/releases/download/v1.0.0/TikTok-Viral-Studio-v1.0.0.apk"

        try {
            _downloadState.value = UpdateDownloadState(isDownloading = true, progress = 0.05f)

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TikTokStudio-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                throw Exception("فشل الاتصال بسيرفر التحميل: ${response.code}")
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            // Target storage in internal cache / external files
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val targetApk = File(updateDir, "update_${System.currentTimeMillis()}.apk")

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
                            val speed = NativeEngine.calculateDownloadSpeed(bytesSinceSample, delta)
                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.6f
                            }

                            _downloadState.value = UpdateDownloadState(
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
            _downloadState.value = UpdateDownloadState(
                isDownloading = false,
                progress = 1.0f,
                downloadedMB = totalBytesRead.toDouble() / (1024.0 * 1024.0),
                totalMB = totalBytesRead.toDouble() / (1024.0 * 1024.0),
                isReadyToInstall = true
            )

            // Trigger installer
            launchApkInstaller(context, targetApk)
        } catch (e: Exception) {
            Log.e(TAG, "Download update failed: ${e.message}", e)
            _downloadState.value = UpdateDownloadState(
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
            // Check unknown sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    // Continue to fire installer intent
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer: ${e.message}", e)
        }
    }
}
