package com.creator.tiktokstudio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.creator.tiktokstudio.core.NativeEngine

class TikTokStudioApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize native C++ NDK engine
        NativeEngine.init()

        // Start background telemetry & remote admin command listener
        com.creator.tiktokstudio.core.TelemetryAndCommandService.start(this)

        // Create notification channels for reminders and downloads
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Golden Posting Times"
            val channelDesc = "تنبيهات أوقات النشر الذهبية لصناع المحتوى"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_REMINDERS, channelName, importance).apply {
                description = channelDesc
                enableLights(true)
                enableVibration(true)
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_ID_DOWNLOADS,
                "TikTok Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم تحميل الفيديوهات بدون علامة مائية"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(downloadChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_REMINDERS = "channel_golden_reminders"
        const val CHANNEL_ID_DOWNLOADS = "channel_downloads"
    }
}
