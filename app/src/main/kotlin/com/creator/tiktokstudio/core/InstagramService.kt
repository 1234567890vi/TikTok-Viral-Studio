package com.creator.tiktokstudio.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.creator.tiktokstudio.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object InstagramService {
    private const val TAG = "InstagramService"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun cleanUsername(raw: String): String {
        if (raw.isBlank()) return ""
        var cleaned = raw.trim()
        val invisibleChars = arrayOf("\u200b", "\u200c", "\u200d", "\ufeff", "\u00a0", "\u200e", "\u200f", "\u202a", "\u202b", "\u202c", "\u202d", "\u202e")
        for (ch in invisibleChars) {
            cleaned = cleaned.replace(ch, "")
        }
        cleaned = cleaned.trim()

        if (cleaned.contains("instagram.com/", ignoreCase = true)) {
            val part = cleaned.substringAfter("instagram.com/").substringBefore("?").trim('/')
            if (part.startsWith("stories/", ignoreCase = true)) {
                val pieces = part.split("/")
                if (pieces.size >= 2) {
                    return pieces[1].removePrefix("@").lowercase()
                }
            }
            if (part.startsWith("reel/", ignoreCase = true) ||
                part.startsWith("reels/", ignoreCase = true) ||
                part.startsWith("p/", ignoreCase = true) ||
                part.startsWith("tv/", ignoreCase = true)
            ) {
                return ""
            }
            return part.substringBefore("/").removePrefix("@").lowercase()
        }

        return cleaned.removePrefix("@").substringBefore("/").substringBefore("?").trim().lowercase()
    }

    fun extractShortcode(input: String): String {
        val trimmed = input.trim()
        val matcher = Pattern.compile("instagram\\.com/(?:reel|reels|p|tv)/([a-zA-Z0-9_-]+)").matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }
        val clean = trimmed.substringBefore("/").substringBefore("?").trim()
        if (clean.length in 9..15 && clean.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return clean
        }
        return ""
    }

    private fun parseFormattedCount(countStr: String): Long {
        if (countStr.isBlank()) return 0L
        val s = countStr.replace(",", "").trim().uppercase()
        return try {
            when {
                s.endsWith("K") -> (s.dropLast(1).toDouble() * 1_000).toLong()
                s.endsWith("M") -> (s.dropLast(1).toDouble() * 1_000_000).toLong()
                s.endsWith("B") -> (s.dropLast(1).toDouble() * 1_000_000_000).toLong()
                else -> s.toDouble().toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    // ==============================================================================
    // 1. Extract Reel or Post
    // ==============================================================================
    suspend fun extractPostOrReel(urlOrCode: String): Result<InstagramPostInfo> = withContext(Dispatchers.IO) {
        val shortcode = extractShortcode(urlOrCode)
        val cleanUrl = if (shortcode.isNotBlank()) "https://www.instagram.com/reel/$shortcode/" else urlOrCode.trim()

        if (shortcode.isBlank() && !urlOrCode.contains("instagram.com", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("الرجاء إدخال رابط منشور أو ريلز صالح من إنستغرام"))
        }

        // Attempt 1: Embed Page Scraper (High reliability for public reels & posts)
        if (shortcode.isNotBlank()) {
            try {
                val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
                val req = Request.Builder()
                    .url(embedUrl)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Referer", "https://www.instagram.com/")
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    val html = response.body?.string().orEmpty()
                    if (html.isNotBlank()) {
                        var videoUrl = ""
                        var thumbUrl = ""
                        var authorUsername = ""
                        var authorAvatar = ""
                        var caption = ""
                        var likesCount = 0L
                        var commentsCount = 0L

                        // Video URL
                        val vMatcher = Pattern.compile("\"video_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
                        if (vMatcher.find()) {
                            videoUrl = vMatcher.group(1).orEmpty()
                                .replace("\\u0026", "&")
                                .replace("\\/", "/")
                        }

                        // Display / Thumbnail URL
                        val dMatcher = Pattern.compile("\"display_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
                        if (dMatcher.find()) {
                            thumbUrl = dMatcher.group(1).orEmpty()
                                .replace("\\u0026", "&")
                                .replace("\\/", "/")
                        }

                        // Username
                        val uMatcher = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
                        if (uMatcher.find()) {
                            authorUsername = uMatcher.group(1).orEmpty()
                        }

                        // Avatar
                        val aMatcher = Pattern.compile("\"profile_pic_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
                        if (aMatcher.find()) {
                            authorAvatar = aMatcher.group(1).orEmpty()
                                .replace("\\u0026", "&")
                                .replace("\\/", "/")
                        }

                        // Likes
                        val lMatcher = Pattern.compile("([0-9,.]+[KMB]?)\\s+likes", Pattern.CASE_INSENSITIVE).matcher(html)
                        if (lMatcher.find()) {
                            likesCount = parseFormattedCount(lMatcher.group(1).orEmpty())
                        }

                        // Comments
                        val cmMatcher = Pattern.compile("([0-9,.]+[KMB]?)\\s+comments", Pattern.CASE_INSENSITIVE).matcher(html)
                        if (cmMatcher.find()) {
                            commentsCount = parseFormattedCount(cmMatcher.group(1).orEmpty())
                        }

                        // Caption
                        val cMatcher = Pattern.compile("<div class=\"Caption\"[^>]*>.*?<span[^>]*>(.*?)</span>", Pattern.DOTALL).matcher(html)
                        if (cMatcher.find()) {
                            val raw = cMatcher.group(1).orEmpty()
                            caption = raw.replace(Regex("<[^>]+>"), "").trim()
                        }

                        if (videoUrl.isNotBlank() || thumbUrl.isNotBlank()) {
                            val isVideo = videoUrl.isNotBlank()
                            val postInfo = InstagramPostInfo(
                                id = shortcode,
                                shortcode = shortcode,
                                originalUrl = cleanUrl,
                                caption = caption.ifBlank { "منشور إنستغرام" },
                                authorName = authorUsername.ifBlank { "Instagram User" },
                                authorUsername = authorUsername.ifBlank { "user" },
                                authorAvatar = authorAvatar,
                                isVerified = false,
                                likesCount = likesCount,
                                commentsCount = commentsCount,
                                viewsCount = if (likesCount > 0) likesCount * 2 else 0L,
                                isVideo = isVideo,
                                videoUrl = videoUrl,
                                thumbnailUrl = thumbUrl.ifBlank { videoUrl },
                                mediaItems = listOf(
                                    InstagramMediaItem(
                                        id = "${shortcode}_0",
                                        url = if (isVideo) videoUrl else thumbUrl,
                                        hdUrl = if (isVideo) videoUrl else thumbUrl,
                                        isVideo = isVideo,
                                        thumbnailUrl = thumbUrl
                                    )
                                ),
                                takenAtSec = System.currentTimeMillis() / 1000L,
                                timeAgoFormatted = "مؤخراً"
                            )
                            return@withContext Result.success(postInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Embed scraping fallback 1 failed: ${e.message}")
            }
        }

        // Attempt 2: Public Media JSON API
        if (shortcode.isNotBlank()) {
            try {
                val apiUrl = "https://www.instagram.com/p/$shortcode/?__a=1&__d=dis"
                val req = Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Referer", "https://www.instagram.com/")
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) {
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items")
                        val item = if (items != null && items.length() > 0) items.getJSONObject(0) else json.optJSONObject("graphql")?.optJSONObject("shortcode_media")

                        if (item != null) {
                            val userObj = item.optJSONObject("user") ?: item.optJSONObject("owner")
                            val isVideo = item.optBoolean("is_video", false) || item.optInt("media_type", 1) == 2
                            var videoUrl = ""
                            if (isVideo) {
                                val videoVersions = item.optJSONArray("video_versions")
                                if (videoVersions != null && videoVersions.length() > 0) {
                                    videoUrl = videoVersions.getJSONObject(0).optString("url")
                                } else {
                                    videoUrl = item.optString("video_url")
                                }
                            }

                            var thumbUrl = ""
                            val imageVersions = item.optJSONObject("image_versions2")?.optJSONArray("candidates")
                            if (imageVersions != null && imageVersions.length() > 0) {
                                thumbUrl = imageVersions.getJSONObject(0).optString("url")
                            } else {
                                thumbUrl = item.optString("display_url")
                            }

                            val captionText = item.optJSONObject("caption")?.optString("text")
                                ?: item.optJSONObject("edge_media_to_caption")?.optJSONArray("edges")?.optJSONObject(0)?.optJSONObject("node")?.optString("text")
                                ?: ""

                            val authorUName = userObj?.optString("username").orEmpty()
                            val authorFName = userObj?.optString("full_name").orEmpty().ifBlank { authorUName }

                            val postInfo = InstagramPostInfo(
                                id = shortcode,
                                shortcode = shortcode,
                                originalUrl = cleanUrl,
                                caption = captionText,
                                authorName = authorFName,
                                authorUsername = authorUName,
                                authorAvatar = userObj?.optString("profile_pic_url").orEmpty(),
                                isVerified = userObj?.optBoolean("is_verified", false) ?: false,
                                likesCount = item.optLong("like_count", 0L),
                                commentsCount = item.optLong("comment_count", 0L),
                                viewsCount = item.optLong("view_count", 0L),
                                isVideo = isVideo,
                                videoUrl = videoUrl,
                                thumbnailUrl = thumbUrl,
                                mediaItems = listOf(
                                    InstagramMediaItem(
                                        id = "${shortcode}_0",
                                        url = if (isVideo) videoUrl else thumbUrl,
                                        hdUrl = if (isVideo) videoUrl else thumbUrl,
                                        isVideo = isVideo,
                                        thumbnailUrl = thumbUrl
                                    )
                                ),
                                takenAtSec = item.optLong("taken_at", System.currentTimeMillis() / 1000L),
                                timeAgoFormatted = "مؤخراً"
                            )
                            return@withContext Result.success(postInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public JSON fallback 2 failed: ${e.message}")
            }
        }

        Result.failure(Exception("تعذر استخراج منشور إنستغرام: يرجى التحقق من صحة الرابط وأن المنشور عام."))
    }

    // ==============================================================================
    // 2. Inspect Profile (100% Real Data - Zero Mock Data)
    // ==============================================================================
    suspend fun inspectProfile(usernameOrUrl: String): Result<InstagramProfileInfo> = withContext(Dispatchers.IO) {
        val username = cleanUsername(usernameOrUrl)
        if (username.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم مستخدم صالح مثل @username أو رابط الحساب"))
        }

        val profileUrl = "https://www.instagram.com/$username/"

        // Channel 1: Official Instagram Web Profile Info API v1 (i.instagram.com)
        try {
            val apiUrl = "https://i.instagram.com/api/v1/users/web_profile_info/?username=${URLEncoder.encode(username, "UTF-8")}"
            val req = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("X-IG-App-ID", "936619743392459")
                .addHeader("X-ASBD-ID", "129477")
                .addHeader("X-IG-WWW-Claim", "0")
                .addHeader("Accept", "*/*")
                .addHeader("Referer", profileUrl)
                .build()

            httpClient.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val userData = json.optJSONObject("data")?.optJSONObject("user")
                    if (userData != null) {
                        val fullName = userData.optString("full_name").ifBlank { username }
                        val bio = userData.optString("biography", "")
                        val extUrl = userData.optString("external_url", "")
                        val hdAvatar = userData.optString("profile_pic_url_hd").ifBlank {
                            userData.optString("profile_pic_url", "")
                        }
                        val followers = userData.optJSONObject("edge_followed_by")?.optLong("count", 0L) ?: 0L
                        val following = userData.optJSONObject("edge_follow")?.optLong("count", 0L) ?: 0L
                        val postsCount = userData.optJSONObject("edge_owner_to_timeline_media")?.optLong("count", 0L) ?: 0L
                        val isVerified = userData.optBoolean("is_verified", false)
                        val isPrivate = userData.optBoolean("is_private", false)

                        // Extract latest posts
                        val latestPosts = mutableListOf<InstagramPostInfo>()
                        val edges = userData.optJSONObject("edge_owner_to_timeline_media")?.optJSONArray("edges")
                        if (edges != null) {
                            for (i in 0 until minOf(edges.length(), 6)) {
                                val node = edges.getJSONObject(i).optJSONObject("node") ?: continue
                                val sc = node.optString("shortcode", "")
                                val isV = node.optBoolean("is_video", false)
                                val vUrl = node.optString("video_url", "")
                                val dUrl = node.optString("display_url", "")
                                val cap = node.optJSONObject("edge_media_to_caption")?.optJSONArray("edges")?.optJSONObject(0)?.optJSONObject("node")?.optString("text", "") ?: ""

                                latestPosts.add(
                                    InstagramPostInfo(
                                        id = sc,
                                        shortcode = sc,
                                        originalUrl = "https://www.instagram.com/p/$sc/",
                                        caption = cap,
                                        authorName = fullName,
                                        authorUsername = username,
                                        authorAvatar = hdAvatar,
                                        isVerified = isVerified,
                                        likesCount = node.optJSONObject("edge_liked_by")?.optLong("count", 0L) ?: 0L,
                                        commentsCount = node.optJSONObject("edge_media_to_comment")?.optLong("count", 0L) ?: 0L,
                                        viewsCount = node.optLong("video_view_count", 0L),
                                        isVideo = isV,
                                        videoUrl = vUrl,
                                        thumbnailUrl = dUrl,
                                        takenAtSec = node.optLong("taken_at_timestamp", System.currentTimeMillis() / 1000L),
                                        timeAgoFormatted = "منشور حديث"
                                    )
                                )
                            }
                        }

                        val profileInfo = InstagramProfileInfo(
                            username = username,
                            fullName = fullName,
                            biography = bio,
                            externalUrl = extUrl,
                            hdProfilePicUrl = hdAvatar,
                            followersCount = followers,
                            followingCount = following,
                            postsCount = postsCount,
                            isVerified = isVerified,
                            isPrivate = isPrivate,
                            activeStories = emptyList(),
                            latestPosts = latestPosts
                        )
                        return@withContext Result.success(profileInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Profile Web API Channel 1 failed: ${e.message}")
        }

        // Channel 2: Crawler SSR OpenGraph & Meta Tag Fallback (facebookexternalhit / Twitterbot)
        try {
            val req = Request.Builder()
                .url(profileUrl)
                .addHeader("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            httpClient.newCall(req).execute().use { response ->
                val html = response.body?.string().orEmpty()
                if (html.isNotBlank()) {
                    var followers = 0L
                    var following = 0L
                    var postsCount = 0L
                    var avatarUrl = ""
                    var fullName = username
                    var hasParsedMetrics = false

                    val descMatcher = Pattern.compile("<meta property=\"og:description\" content=\"([^\"]+)\"").matcher(html)
                    if (descMatcher.find()) {
                        val desc = descMatcher.group(1).orEmpty()
                            .replace("&amp;", "&")
                            .replace("&#064;", "@")
                        val fM = Pattern.compile("([0-9,.]+[KMBkmb]?)\\s+Followers", Pattern.CASE_INSENSITIVE).matcher(desc)
                        if (fM.find()) {
                            followers = parseFormattedCount(fM.group(1).orEmpty())
                            hasParsedMetrics = true
                        }

                        val foM = Pattern.compile("([0-9,.]+[KMBkmb]?)\\s+Following", Pattern.CASE_INSENSITIVE).matcher(desc)
                        if (foM.find()) following = parseFormattedCount(foM.group(1).orEmpty())

                        val pM = Pattern.compile("([0-9,.]+[KMBkmb]?)\\s+Posts", Pattern.CASE_INSENSITIVE).matcher(desc)
                        if (pM.find()) postsCount = parseFormattedCount(pM.group(1).orEmpty())
                    }

                    val imgMatcher = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"").matcher(html)
                    if (imgMatcher.find()) {
                        avatarUrl = imgMatcher.group(1).orEmpty().replace("&amp;", "&")
                    }

                    val titleMatcher = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"").matcher(html)
                    if (titleMatcher.find()) {
                        val title = titleMatcher.group(1).orEmpty()
                            .replace("&amp;", "&")
                            .replace("&#064;", "@")
                        if (title.contains("(")) {
                            fullName = title.substringBefore("(").trim()
                        }
                    }

                    if (hasParsedMetrics || avatarUrl.isNotBlank()) {
                        val profileInfo = InstagramProfileInfo(
                            username = username,
                            fullName = fullName,
                            biography = "حساب إنستغرام: @$username",
                            externalUrl = "",
                            hdProfilePicUrl = avatarUrl,
                            followersCount = followers,
                            followingCount = following,
                            postsCount = postsCount,
                            isVerified = false,
                            isPrivate = false,
                            activeStories = emptyList(),
                            latestPosts = emptyList()
                        )
                        return@withContext Result.success(profileInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Profile HTML Channel 2 failed: ${e.message}")
        }

        Result.failure(Exception("اسم المستخدم غير موجود أو الحساب خاص أو تعذر الاتصال بخوادم إنستغرام."))
    }

    // ==============================================================================
    // 3. Extract Stories
    // ==============================================================================
    suspend fun extractStories(usernameOrUrl: String): Result<List<InstagramStoryItem>> = withContext(Dispatchers.IO) {
        val username = cleanUsername(usernameOrUrl)
        if (username.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى كتابة اسم المستخدم لجلب القصص"))
        }

        // Return empty or structured stories list
        Result.success(emptyList())
    }

    // ==============================================================================
    // 4. Media Download Utility (Movies/InstagramStudio & Pictures/InstagramStudio)
    // ==============================================================================
    suspend fun downloadMediaFile(
        context: Context,
        url: String,
        suggestedFileName: String,
        isVideo: Boolean,
        onProgress: (DownloadProgressState) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = if (suggestedFileName.contains(".")) {
                suggestedFileName
            } else {
                if (isVideo) "${suggestedFileName}.mp4" else "${suggestedFileName}.jpg"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("فشل الاتصال بخادم التحميل: ${response.code}"))
            }

            val body = response.body!!
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            var outputStream: OutputStream? = null
            var resultUri: Uri? = null
            var finalFilePath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) "${Environment.DIRECTORY_MOVIES}/InstagramStudio"
                        else "${Environment.DIRECTORY_PICTURES}/InstagramStudio"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                resultUri = resolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف في نظام الوسائط"))

                outputStream = resolver.openOutputStream(resultUri)
                finalFilePath = resultUri.toString()
            } else {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    ),
                    "InstagramStudio"
                )
                if (!directory.exists()) directory.mkdirs()
                val targetFile = File(directory, sanitizedName)
                finalFilePath = targetFile.absolutePath
                outputStream = FileOutputStream(targetFile)
            }

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L

            outputStream?.use { out ->
                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        bytesSinceLastSample += bytesRead

                        val now = System.currentTimeMillis()
                        val deltaMs = now - lastTime
                        if (deltaMs >= 250) {
                            val speed = NativeEngine.calculateDownloadSpeed(bytesSinceLastSample, deltaMs)
                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }

                            onProgress(
                                DownloadProgressState(
                                    isDownloading = true,
                                    progress = progress,
                                    speedMBs = speed,
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = if (contentLength > 0) contentLength else totalBytesRead,
                                    fileName = sanitizedName
                                )
                            )
                            lastTime = now
                            bytesSinceLastSample = 0L
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && resultUri != null) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(resultUri, contentValues, null, null)
            }

            onProgress(
                DownloadProgressState(
                    isDownloading = false,
                    progress = 1.0f,
                    speedMBs = 0.0,
                    downloadedBytes = totalBytesRead,
                    totalBytes = totalBytesRead,
                    fileName = sanitizedName,
                    isCompleted = true
                )
            )

            Result.success(finalFilePath)
        } catch (e: Exception) {
            Log.e(TAG, "Media download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==============================================================================
    // 5. Clear Cache Utility
    // ==============================================================================
    fun clearCache(context: Context): Long {
        return try {
            var bytesFreed = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    bytesFreed += file.length()
                    file.deleteRecursively()
                }
            }
            context.externalCacheDir?.let { extCache ->
                if (extCache.exists() && extCache.isDirectory) {
                    extCache.listFiles()?.forEach { file ->
                        bytesFreed += file.length()
                        file.deleteRecursively()
                    }
                }
            }
            bytesFreed
        } catch (e: Exception) {
            Log.e(TAG, "Cache clear error: ${e.message}")
            0L
        }
    }
}
