package com.creator.tiktokstudio.core

import java.util.Locale

enum class InputTargetType {
    INSPECTOR,
    LIVE_TRACKER,
    DOWNLOADER
}

object InputUtils {
    /**
     * Sanitizes username:
     * - Trims whitespace
     * - Removes zero-width spaces and invisible characters
     * - Converts to lowercase
     * - Strips leading @, URL path, query params
     */
    fun sanitizeUsername(raw: String): String {
        if (raw.isBlank()) return ""
        val cleaned = raw.trim()
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace("\u00A0", "")
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u202A", "")
            .replace("\u202B", "")
            .replace("\u202C", "")
            .replace("\u202D", "")
            .replace("\u202E", "")
            .trim()

        var user = cleaned
        if (user.contains("tiktok.com/")) {
            val part = user.substringAfter("tiktok.com/")
            if (part.contains("@")) {
                user = "@" + part.substringAfter("@").substringBefore("/").substringBefore("?")
            }
        }
        val regex = Regex("""@([a-zA-Z0-9_.-]+)""")
        val match = regex.find(user)
        if (match != null) {
            return match.groupValues[1].lowercase(Locale.ROOT)
        }
        return user.removePrefix("@")
            .substringBefore("/")
            .substringBefore("?")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Normalizes any raw TikTok input:
     * - Cleans whitespaces (.trim())
     * - If username (e.g. "@khaby.lame" or "khaby.lame"):
     *     - INSPECTOR -> "https://www.tiktok.com/@$cleanUser"
     *     - LIVE_TRACKER -> "https://www.tiktok.com/@$cleanUser/live"
     *     - DOWNLOADER -> "https://www.tiktok.com/@$cleanUser"
     * - If full or shortened URL missing scheme (e.g. "vm.tiktok.com/..."): prepends "https://"
     * - If already contains "http://" or "https://": preserves original clean URL
     */
    fun normalizeTikTokInput(rawInput: String, targetType: InputTargetType): String {
        val clean = rawInput.trim()
        if (clean.isBlank()) return ""

        // Preserve already valid HTTP(S) URLs
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }

        // If it starts with a recognized TikTok domain missing scheme, prepend https://
        val lower = clean.lowercase(Locale.ROOT)
        if (lower.startsWith("tiktok.com") ||
            lower.startsWith("www.tiktok.com") ||
            lower.startsWith("vm.tiktok.com") ||
            lower.startsWith("vt.tiktok.com") ||
            clean.contains("tiktok.com/")
        ) {
            return "https://$clean"
        }

        // It is a direct username (e.g. "@username" or "username")
        val cleanUser = sanitizeUsername(clean)

        return when (targetType) {
            InputTargetType.INSPECTOR -> "https://www.tiktok.com/@$cleanUser"
            InputTargetType.LIVE_TRACKER -> "https://www.tiktok.com/@$cleanUser/live"
            InputTargetType.DOWNLOADER -> "https://www.tiktok.com/@$cleanUser"
        }
    }

    /**
     * Extracts pure username without '@', slashes, or query parameters.
     */
    fun extractCleanUsername(input: String): String {
        return sanitizeUsername(input)
    }
}
