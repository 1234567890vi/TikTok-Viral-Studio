package com.creator.tiktokstudio.core

enum class InputTargetType {
    INSPECTOR,
    LIVE_TRACKER,
    DOWNLOADER
}

object InputUtils {
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
        val lower = clean.lowercase()
        if (lower.startsWith("tiktok.com") ||
            lower.startsWith("www.tiktok.com") ||
            lower.startsWith("vm.tiktok.com") ||
            lower.startsWith("vt.tiktok.com") ||
            clean.contains("tiktok.com/")
        ) {
            return "https://$clean"
        }

        // It is a direct username (e.g. "@username" or "username")
        val cleanUser = clean.removePrefix("@").trim()

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
        val clean = input.trim()
        if (clean.isBlank()) return ""

        // Pattern matching @username anywhere in url/string
        val regex = Regex("""@([a-zA-Z0-9_.-]+)""")
        val match = regex.find(clean)
        if (match != null) {
            return match.groupValues[1]
        }

        // Starts with @
        if (clean.startsWith("@")) {
            return clean.removePrefix("@").substringBefore("/").substringBefore("?").trim()
        }

        // Pure username without http or /
        if (!clean.contains("http://", ignoreCase = true) &&
            !clean.contains("https://", ignoreCase = true) &&
            !clean.contains("/")
        ) {
            return clean.removePrefix("@").trim()
        }

        return clean.substringAfterLast("@", "").substringBefore("/").substringBefore("?").trim()
    }
}
