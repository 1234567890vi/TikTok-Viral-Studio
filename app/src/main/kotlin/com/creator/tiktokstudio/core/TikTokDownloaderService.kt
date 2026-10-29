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
import java.util.*
import java.util.concurrent.TimeUnit

object TikTokDownloaderService {
    private const val TAG = "TikTokDownloaderService"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val ARABIC_MONTHS = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun resolveRedirects(inputUrl: String): String {
        val clean = inputUrl.trim()
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }
        return try {
            val request = Request.Builder()
                .url(clean)
                .addHeader("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (finalUrl.isNotBlank()) finalUrl else clean
            }
        } catch (e: Exception) {
            Log.w(TAG, "Redirect resolution failed: ${e.message}")
            clean
        }
    }

    fun sanitizeUrl(url: String): String {
        if (url.isBlank()) return ""
        var u = url.trim()
        if (u.startsWith("http://")) {
            u = u.replace("http://", "https://")
        }
        if (!u.startsWith("http")) {
            u = "https://www.tikwm.com$u"
        }
        return u
    }

    fun formatArabicTimestamp(seconds: Long): String {
        if (seconds <= 0L) return "تاريخ النشر غير متوفر"
        return try {
            val date = Date(seconds * 1000L)
            val cal = Calendar.getInstance()
            cal.time = date
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val minute = String.format("%02d", cal.get(Calendar.MINUTE))
            val monthName = ARABIC_MONTHS.getOrElse(month) { "" }

            val nowSec = System.currentTimeMillis() / 1000L
            val diffSec = nowSec - seconds
            val timeAgo = when {
                diffSec < 60 -> "منذ لحظات"
                diffSec < 3600 -> "منذ ${diffSec / 60} دقيقة"
                diffSec < 7200 -> "منذ ساعة"
                diffSec < 14400 -> "منذ ساعتين"
                diffSec < 86400 -> "منذ ${diffSec / 3600} ساعة"
                diffSec < 172800 -> "منذ يوم"
                diffSec < 2592000 -> "منذ ${diffSec / 86400} يوم"
                else -> "منذ ${diffSec / 2592000} شهر"
            }
            "نُشر في $day $monthName $year - الساعة $hour:$minute ($timeAgo)"
        } catch (e: Exception) {
            "تاريخ غير صالح"
        }
    }

    fun formatCommentTimeAgo(seconds: Long): String {
        if (seconds <= 0L) return ""
        val diffSec = (System.currentTimeMillis() / 1000L) - seconds
        return when {
            diffSec < 60 -> "الآن"
            diffSec < 3600 -> "منذ ${diffSec / 60} د"
            diffSec < 86400 -> "منذ ${diffSec / 3600} س"
            diffSec < 172800 -> "منذ يوم"
            diffSec < 2592000 -> "منذ ${diffSec / 86400} ي"
            else -> "منذ ${diffSec / 2592000} ش"
        }
    }

    suspend fun extractMediaInfo(url: String): Result<TikTokVideoInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            val normalizedUrl = InputUtils.normalizeTikTokInput(cleanUrl, InputTargetType.DOWNLOADER)
            if (normalizedUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("الرجاء إدخال رابط تيك توك صالح أو اسم مستخدم"))
            }

            val resolvedUrl = resolveRedirects(normalizedUrl)
            val apiEndpoint = "https://www.tikwm.com/api/?url=${URLEncoder.encode(resolvedUrl, "UTF-8")}&hd=1"
            val request = Request.Builder()
                .url(apiEndpoint)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val code = json.optInt("code", -1)
                if (code == 0 && json.has("data")) {
                    val dataObj = json.getJSONObject("data")
                    val hdPlay = sanitizeUrl(dataObj.optString("hdplay", ""))
                    val sdPlay = sanitizeUrl(dataObj.optString("play", ""))
                    val music = sanitizeUrl(dataObj.optString("music", ""))
                    val title = dataObj.optString("title", "فيديو تيك توك بدون علامة مائية")
                    val cover = sanitizeUrl(dataObj.optString("cover", "").ifBlank { dataObj.optString("origin_cover", "") })
                    val duration = dataObj.optInt("duration", 0)

                    val author = dataObj.optJSONObject("author")
                    val authorName = author?.optString("nickname") ?: "صانع محتوى"
                    val authorId = author?.optString("unique_id") ?: "creator"
                    val authorAvatar = sanitizeUrl(author?.optString("avatar", "") ?: "")

                    val videoInfo = TikTokVideoInfo(
                        id = dataObj.optString("id", System.currentTimeMillis().toString()),
                        originalUrl = resolvedUrl,
                        title = title,
                        authorName = authorName,
                        authorId = authorId,
                        authorAvatar = authorAvatar,
                        thumbnailUrl = cover,
                        videoHdUrl = if (hdPlay.isNotBlank()) hdPlay else sdPlay,
                        videoSdUrl = sdPlay,
                        audioUrl = music,
                        durationSec = duration,
                        likesCount = dataObj.optLong("digg_count", 0L),
                        commentsCount = dataObj.optLong("comment_count", 0L),
                        sharesCount = dataObj.optLong("share_count", 0L),
                        viewsCount = dataObj.optLong("play_count", 0L),
                        isWatermarkFree = true
                    )
                    return@withContext Result.success(videoInfo)
                } else {
                    val msg = json.optString("msg", "فشل استخراج الفيديو: تأكد من أن الرابط صحيح وأن المنشور عام.")
                    return@withContext Result.failure(Exception(msg))
                }
            }
            return@withContext Result.failure(Exception("تعذر الاتصال بخوادم تيك توك"))
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun inspectTikTokUrl(url: String): Result<TikTokDeepInspectionData> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val normalizedUrl = InputUtils.normalizeTikTokInput(cleanUrl, InputTargetType.INSPECTOR)
        val resolvedUrl = resolveRedirects(normalizedUrl)

        // Profile detection:
        // /@username without /video/, /photo/, or /story/, or bare username, or starts with @
        val isProfile = (
            (!resolvedUrl.contains("/video/") && !resolvedUrl.contains("/photo/") && !resolvedUrl.contains("/story/")) &&
            (resolvedUrl.contains("/@") || cleanUrl.startsWith("@") || !cleanUrl.contains("/"))
        )

        try {
            if (isProfile) {
                val username = InputUtils.extractCleanUsername(if (cleanUrl.startsWith("@") || !cleanUrl.contains("/")) cleanUrl else resolvedUrl)
                if (username.isBlank()) {
                    return@withContext Result.failure(Exception("اسم المستخدم غير صالح"))
                }

                val profileApi = "https://www.tikwm.com/api/user/info?unique_id=${URLEncoder.encode(username, "UTF-8")}"
                val req = Request.Builder()
                    .url(profileApi)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    if (json.optInt("code", -1) == 0 && json.has("data")) {
                        val data = json.getJSONObject("data")
                        val user = data.optJSONObject("user")
                        val stats = data.optJSONObject("stats")
                        val uniqueId = user?.optString("uniqueId") ?: username
                        val nickname = user?.optString("nickname") ?: uniqueId
                        val avatarRaw = user?.optString("avatarLarger").orEmpty().ifBlank {
                            user?.optString("avatarMedium").orEmpty().ifBlank {
                                user?.optString("avatarThumb").orEmpty().ifBlank {
                                    user?.optString("avatar").orEmpty()
                                }
                            }
                        }
                        val avatar = sanitizeUrl(avatarRaw).ifBlank {
                            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200"
                        }
                        val signature = user?.optString("signature", "") ?: ""
                        val followers = stats?.optLong("followerCount", 0L) ?: 0L
                        val following = stats?.optLong("followingCount", 0L) ?: 0L
                        val heart = stats?.optLong("heartCount", 0L) ?: 0L
                        val videos = stats?.optLong("videoCount", 0L) ?: 0L
                        val isVerified = user?.optBoolean("verified", false) ?: false

                        return@withContext Result.success(
                            TikTokDeepInspectionData(
                                url = normalizedUrl,
                                contentType = TikTokContentType.PROFILE,
                                title = "حساب تيك توك: @$uniqueId",
                                authorName = nickname,
                                authorUniqueId = uniqueId,
                                authorAvatar = avatar,
                                authorBio = signature,
                                followerCount = followers,
                                followingCount = following,
                                authorTotalLikes = heart,
                                authorVideoCount = videos,
                                isVerified = isVerified,
                                createTimeFormatted = "حساب نشط ومسجل",
                                topComments = emptyList()
                            )
                        )
                    } else {
                        return@withContext Result.failure(
                            Exception("اسم المستخدم غير موجود أو الحساب خاص")
                        )
                    }
                }
                return@withContext Result.failure(
                    Exception("اسم المستخدم غير موجود أو الحساب خاص")
                )
            }

            // Normal Video, Photo Mode, or Story
            val apiEndpoint = "https://www.tikwm.com/api/?url=${URLEncoder.encode(resolvedUrl, "UTF-8")}&hd=1"
            val req = Request.Builder()
                .url(apiEndpoint)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string()

            if (!resp.isSuccessful || body.isNullOrBlank()) {
                return@withContext Result.failure(Exception("تعذر الاتصال بخوادم الفحص. يرجى التحقق من اتصالك بالإنترنت."))
            }

            val json = JSONObject(body)
            val code = json.optInt("code", -1)
            if (code != 0 || !json.has("data")) {
                val errorMsg = json.optString("msg", "فشل فحص الرابط: يرجى التأكد من أن المنشور متاح وعام وصحيح.")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val dataObj = json.getJSONObject("data")
            val hdPlay = sanitizeUrl(dataObj.optString("hdplay", ""))
            val sdPlay = sanitizeUrl(dataObj.optString("play", ""))
            val music = sanitizeUrl(dataObj.optString("music", ""))
            val musicInfo = dataObj.optJSONObject("music_info")
            val audioTitle = musicInfo?.optString("title", "الصوت الأصلي") ?: "الصوت الأصلي"
            val audioAuthor = musicInfo?.optString("author", "TikTok") ?: "TikTok"
            val title = dataObj.optString("title", "منشور تيك توك")
            val cover = sanitizeUrl(dataObj.optString("cover", "").ifBlank { dataObj.optString("origin_cover", "") })
            val duration = dataObj.optInt("duration", 0)

            val author = dataObj.optJSONObject("author")
            val authorName = author?.optString("nickname", "صانع محتوى") ?: "صانع محتوى"
            val authorId = author?.optString("unique_id", "creator") ?: "creator"
            val authorAvatar = sanitizeUrl(author?.optString("avatar", "") ?: "")

            // Parse Photos
            val imagesArr = dataObj.optJSONArray("images")
            val photosList = mutableListOf<TikTokPhotoItem>()
            if (imagesArr != null) {
                for (i in 0 until imagesArr.length()) {
                    val imgUrl = sanitizeUrl(imagesArr.getString(i))
                    photosList.add(TikTokPhotoItem(index = i + 1, url = imgUrl, hdUrl = imgUrl))
                }
            }

            // Classification
            val isPhoto = photosList.isNotEmpty() || resolvedUrl.contains("/photo/") || cleanUrl.contains("/photo/")
            val isStory = (
                resolvedUrl.contains("/story/") ||
                cleanUrl.contains("/story/") ||
                dataObj.optBoolean("is_story", false) ||
                dataObj.optInt("is_story", 0) == 1 ||
                title.lowercase().contains("#story")
            )

            val detectedType = when {
                isPhoto -> TikTokContentType.PHOTO_ALBUM
                isStory -> TikTokContentType.STORY
                else -> TikTokContentType.VIDEO
            }

            val rawCreateTime = dataObj.optLong("create_time", 0L)
            val formattedDate = formatArabicTimestamp(rawCreateTime)

            val sizeBytes = dataObj.optLong("hd_size", 0L).let { if (it > 0) it else dataObj.optLong("size", 0L) }
            val sizeMb = if (sizeBytes > 0) {
                Math.round((sizeBytes.toDouble() / (1024.0 * 1024.0)) * 100.0) / 100.0
            } else 0.0

            // Fetch Real Comments
            val commentsList = mutableListOf<TikTokCommentItem>()
            try {
                val commentEndpoint = "https://www.tikwm.com/api/comment/list?url=${URLEncoder.encode(resolvedUrl, "UTF-8")}&count=30"
                val cReq = Request.Builder()
                    .url(commentEndpoint)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .build()
                val cResp = httpClient.newCall(cReq).execute()
                val cBody = cResp.body?.string()
                if (cResp.isSuccessful && !cBody.isNullOrBlank()) {
                    val cJson = JSONObject(cBody)
                    if (cJson.optInt("code", -1) == 0 && cJson.has("data")) {
                        val cData = cJson.getJSONObject("data")
                        val commentsArr = cData.optJSONArray("comments")
                        if (commentsArr != null) {
                            for (j in 0 until commentsArr.length()) {
                                val cObj = commentsArr.getJSONObject(j)
                                val uObj = cObj.optJSONObject("user")
                                val cUser = uObj?.optString("nickname", "مستخدم تيك توك") ?: "مستخدم تيك توك"
                                val cUid = uObj?.optString("unique_id", "user") ?: "user"
                                val cAvatar = sanitizeUrl(uObj?.optString("avatar", "") ?: "")
                                val cText = cObj.optString("text", "")
                                val cLikes = cObj.optLong("digg_count", 0L)
                                val cReplies = cObj.optLong("reply_total", 0L)
                                val cTime = cObj.optLong("create_time", 0L)
                                commentsList.add(
                                    TikTokCommentItem(
                                        id = cObj.optString("id", j.toString()),
                                        authorName = cUser,
                                        authorUniqueId = cUid,
                                        authorAvatar = cAvatar,
                                        text = cText,
                                        diggCount = cLikes,
                                        replyCount = cReplies,
                                        timeAgoArabic = formatCommentTimeAgo(cTime)
                                    )
                                )
                            }
                            commentsList.sortByDescending { it.diggCount }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Comments fetch note: ${e.message}")
            }

            return@withContext Result.success(
                TikTokDeepInspectionData(
                    url = resolvedUrl,
                    contentType = detectedType,
                    title = title,
                    authorName = authorName,
                    authorUniqueId = authorId,
                    authorAvatar = authorAvatar,
                    thumbnailUrl = cover,
                    createTimeFormatted = formattedDate,
                    rawCreateTime = rawCreateTime,
                    likesCount = dataObj.optLong("digg_count", 0L),
                    commentsCount = dataObj.optLong("comment_count", 0L),
                    collectCount = dataObj.optLong("collect_count", 0L),
                    downloadCount = dataObj.optLong("download_count", 0L),
                    sharesCount = dataObj.optLong("share_count", 0L),
                    viewsCount = dataObj.optLong("play_count", 0L),
                    resolution = if (isPhoto) "ألبوم صور عالي الدقة" else "1080x1920 (Full HD)",
                    durationSec = duration,
                    fileSizeMB = sizeMb,
                    qualityBadge = if (isPhoto) "ألبوم صور" else "Full HD 1080p",
                    audioTitle = audioTitle,
                    audioAuthor = audioAuthor,
                    audioUrl = music,
                    videoHdUrl = if (hdPlay.isNotBlank()) hdPlay else sdPlay,
                    videoSdUrl = sdPlay,
                    photos = photosList,
                    topComments = commentsList
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Deep inspection exception: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun downloadMediaFile(
        context: Context,
        downloadUrl: String,
        title: String,
        isVideo: Boolean,
        onProgress: (DownloadProgressState) -> Unit
    ): Result<DownloadedMedia> = withContext(Dispatchers.IO) {
        try {
            val ext = if (isVideo) "mp4" else "mp3"
            val sanitizedName = NativeEngine.sanitizeFilename(title, ext)

            onProgress(
                DownloadProgressState(
                    isDownloading = true,
                    progress = 0.05f,
                    speedMBs = 0.0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    fileName = sanitizedName
                )
            )

            val request = Request.Builder()
                .url(downloadUrl)
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
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "audio/mpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) "${Environment.DIRECTORY_MOVIES}/TikTokStudio"
                        else "${Environment.DIRECTORY_MUSIC}/TikTokStudio"
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
                        if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                    ),
                    "TikTokStudio"
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

            val media = DownloadedMedia(
                id = System.currentTimeMillis().toString(),
                title = title,
                filePath = finalFilePath,
                fileUriString = resultUri?.toString() ?: finalFilePath,
                sizeBytes = totalBytesRead,
                isVideo = isVideo,
                timestamp = System.currentTimeMillis()
            )

            Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            onProgress(
                DownloadProgressState(
                    isDownloading = false,
                    progress = 0f,
                    error = e.localizedMessage ?: "حدث خطأ أثناء تحميل الملف"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun downloadImageFile(
        context: Context,
        downloadUrl: String,
        title: String,
        onProgress: (DownloadProgressState) -> Unit
    ): Result<DownloadedMedia> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = NativeEngine.sanitizeFilename(title, "jpg")
            onProgress(
                DownloadProgressState(
                    isDownloading = true,
                    progress = 0.2f,
                    speedMBs = 0.0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    fileName = sanitizedName
                )
            )

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.failure(Exception("فشل الاتصال بخادم الصور: ${response.code}"))
            }

            val body = response.body!!
            val bytes = body.bytes()

            var resultUri: Uri? = null
            var finalFilePath = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TikTokStudio")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                resultUri = resolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(Exception("تعذر إنشاء ملف صورة في النظام"))

                resolver.openOutputStream(resultUri)?.use { out ->
                    out.write(bytes)
                }
                finalFilePath = resultUri.toString()

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(resultUri, contentValues, null, null)
            } else {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "TikTokStudio"
                )
                if (!directory.exists()) directory.mkdirs()
                val targetFile = File(directory, sanitizedName)
                finalFilePath = targetFile.absolutePath
                FileOutputStream(targetFile).use { it.write(bytes) }
            }

            onProgress(
                DownloadProgressState(
                    isDownloading = false,
                    progress = 1.0f,
                    speedMBs = 0.0,
                    downloadedBytes = bytes.size.toLong(),
                    totalBytes = bytes.size.toLong(),
                    fileName = sanitizedName,
                    isCompleted = true
                )
            )

            Result.success(
                DownloadedMedia(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    filePath = finalFilePath,
                    fileUriString = resultUri?.toString() ?: finalFilePath,
                    sizeBytes = bytes.size.toLong(),
                    isVideo = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Image download failed: ${e.message}", e)
            onProgress(
                DownloadProgressState(
                    isDownloading = false,
                    progress = 0f,
                    error = e.localizedMessage ?: "حدث خطأ أثناء حفظ الصورة"
                )
            )
            Result.failure(e)
        }
    }
}
