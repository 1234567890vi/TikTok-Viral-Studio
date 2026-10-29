package com.creator.tiktokstudio.core

import android.util.Log

object NativeEngine {
    private const val TAG = "NativeEngine"
    private var isLoaded = false

    fun init() {
        try {
            System.loadLibrary("tiktokstudio")
            isLoaded = true
            Log.i(TAG, "Native C++ NDK library loaded successfully: ${getEngineVersionNative()}")
        } catch (t: Throwable) {
            Log.e(TAG, "Native library load deferred or failed: ${t.message}")
            isLoaded = false
        }
    }

    fun isNativeLoaded(): Boolean = isLoaded

    // Native JNI Declarations
    @JvmStatic
    external fun getEngineVersionNative(): String

    @JvmStatic
    external fun validateVipKeyNative(key: String): Boolean

    @JvmStatic
    external fun calculateDownloadSpeedNative(bytesDelta: Long, timeDeltaMs: Long): Double

    @JvmStatic
    external fun formatBitrateNative(totalBytes: Long): String

    @JvmStatic
    external fun sanitizeFilenameNative(rawTitle: String, ext: String): String

    @JvmStatic
    external fun initWebcastRingBufferNative(capacity: Int): Boolean

    @JvmStatic
    external fun pushWebcastFrameNative(data: ByteArray): Boolean

    @JvmStatic
    external fun decompressGzipNative(compressed: ByteArray): ByteArray?

    @JvmStatic
    external fun parseWebcastFrameNative(data: ByteArray): String

    // Safe wrappers with Kotlin fallback for previews/unit tests
    fun validateVipKey(key: String): Boolean {
        val clean = key.trim().uppercase()
        val remoteKeys = RemoteUpdateManager.remoteConfig.value?.vipCodes?.map { it.trim().uppercase() } ?: emptyList()
        if (clean in remoteKeys) return true

        if (isLoaded) {
            try {
                return validateVipKeyNative(key)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for validateVipKey", e)
            }
        }
        return clean in listOf("TIKTOK-VIP-2026", "CREATOR-PRO-999", "VIRAL-STUDIO-GOLD", "ULTRA-SPEED-NDK")
    }

    fun calculateDownloadSpeed(bytesDelta: Long, timeDeltaMs: Long): Double {
        if (isLoaded) {
            try {
                return calculateDownloadSpeedNative(bytesDelta, timeDeltaMs)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for calculateDownloadSpeed", e)
            }
        }
        if (timeDeltaMs <= 0) return 0.0
        val bytesPerSec = (bytesDelta.toDouble() / timeDeltaMs) * 1000.0
        return bytesPerSec / (1024.0 * 1024.0)
    }

    fun formatBitrate(totalBytes: Long): String {
        if (isLoaded) {
            try {
                return formatBitrateNative(totalBytes)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for formatBitrate", e)
            }
        }
        val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    fun sanitizeFilename(rawTitle: String, ext: String): String {
        if (isLoaded) {
            try {
                return sanitizeFilenameNative(rawTitle, ext)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for sanitizeFilename", e)
            }
        }
        val safe = rawTitle.replace(Regex("[^a-zA-Z0-9_\\-\\s\\u0600-\\u06FF]"), "_").take(50)
        return "TikTokStudio_${if (safe.isBlank()) "video" else safe}.$ext"
    }

    fun initRingBuffer(capacity: Int = 200): Boolean {
        if (isLoaded) {
            try {
                return initWebcastRingBufferNative(capacity)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for initRingBuffer", e)
            }
        }
        return true
    }

    fun pushWebcastFrame(data: ByteArray): Boolean {
        if (isLoaded) {
            try {
                return pushWebcastFrameNative(data)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for pushWebcastFrame", e)
            }
        }
        return true
    }

    fun decompressGzip(compressed: ByteArray): ByteArray? {
        if (isLoaded) {
            try {
                return decompressGzipNative(compressed)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for decompressGzip", e)
            }
        }
        return null
    }

    fun parseWebcastFrame(data: ByteArray): String {
        if (isLoaded) {
            try {
                return parseWebcastFrameNative(data)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "UnsatisfiedLinkError fallback for parseWebcastFrame", e)
            }
        }
        return "{}"
    }
}
