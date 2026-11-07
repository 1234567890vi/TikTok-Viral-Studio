package com.creator.tiktokstudio.data

data class TikTokVideoInfo(
    val id: String = "",
    val originalUrl: String = "",
    val title: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val authorAvatar: String = "",
    val thumbnailUrl: String = "",
    val videoHdUrl: String = "",
    val videoSdUrl: String = "",
    val audioUrl: String = "",
    val durationSec: Int = 0,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val sharesCount: Long = 0,
    val viewsCount: Long = 0,
    val isWatermarkFree: Boolean = true
)

data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val speedMBs: Double = 0.0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val fileName: String = "",
    val isCompleted: Boolean = false,
    val error: String? = null
)

data class DownloadedMedia(
    val id: String,
    val title: String,
    val filePath: String,
    val fileUriString: String,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val timestamp: Long,
    val durationSec: Int = 0
)

data class PostingTimeSlot(
    val id: String,
    val dayArabic: String,
    val timeSlotArabic: String,
    val scorePercentage: Int,
    val recommendedNiche: String,
    val strategyTip: String,
    val isReminderActive: Boolean = false
)

data class HashtagItem(
    val tag: String,
    val reachEstimate: String,
    val growthBadge: String,
    val category: String
)

data class VideoIdeaCard(
    val id: String,
    val categoryArabic: String,
    val hook: String,
    val cameraMovement: String,
    val executionSecret: String,
    val audioVibe: String
)

data class ViralScript(
    val title: String,
    val category: String,
    val hook0To3Sec: String,
    val coreValue: String,
    val callToAction: String,
    val cinematographyTip: String
)

data class AccountAnalysisReport(
    val username: String,
    val followers: Long,
    val averageViews: Long,
    val engagementRatePercentage: Double,
    val algorithmGrade: String,
    val viralityStatusArabic: String,
    val keyFindings: List<String>,
    val actionRecommendations: List<String>
)

enum class TikTokContentType(val titleArabic: String, val badgeIcon: String) {
    VIDEO("فيديو عادي", "🎬"),
    PHOTO_ALBUM("ألبوم صور (Photo Mode)", "🖼️"),
    STORY("قصة (Story)", "⏱️"),
    PROFILE("حساب شخصي", "👤")
}

data class TikTokCommentItem(
    val id: String,
    val authorName: String,
    val authorUniqueId: String,
    val authorAvatar: String,
    val text: String,
    val diggCount: Long,
    val replyCount: Long = 0,
    val timeAgoArabic: String = ""
)

data class TikTokPhotoItem(
    val index: Int,
    val url: String,
    val hdUrl: String = ""
)

data class TikTokDeepInspectionData(
    val url: String,
    val contentType: TikTokContentType,
    val title: String,
    val authorName: String,
    val authorUniqueId: String,
    val authorAvatar: String,
    val authorBio: String = "",
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val authorTotalLikes: Long = 0,
    val authorVideoCount: Long = 0,
    val isVerified: Boolean = false,

    // Specs & Metrics
    val createTimeFormatted: String = "",
    val rawCreateTime: Long = 0,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val collectCount: Long = 0,
    val downloadCount: Long = 0,
    val sharesCount: Long = 0,
    val viewsCount: Long = 0,
    val resolution: String = "1080x1920 (Full HD)",
    val durationSec: Int = 0,
    val fileSizeMB: Double = 0.0,
    val qualityBadge: String = "Full HD 1080p",
    val audioTitle: String = "",
    val audioAuthor: String = "",
    val audioUrl: String = "",

    // Media URLs
    val videoHdUrl: String = "",
    val videoSdUrl: String = "",
    val thumbnailUrl: String = "",
    val photos: List<TikTokPhotoItem> = emptyList(),

    // Top Liked Comments
    val topComments: List<TikTokCommentItem> = emptyList()
)

// ==============================================================================
// Studio Platform Switcher
// ==============================================================================
enum class StudioPlatform {
    TIKTOK,
    INSTAGRAM
}

// ==============================================================================
// Instagram Models (Reels, Posts, Carousels, Stories & Profile)
// ==============================================================================
enum class InstagramContentType(val titleArabic: String, val badgeIcon: String) {
    REEL("ريلز (Reels)", "🎥"),
    POST_VIDEO("فيديو منشور", "🎬"),
    POST_IMAGE("صورة واحدة", "🖼️"),
    CAROUSEL("ألبوم وسائط متعدد", "📚"),
    STORY("قصة نشطة (Story)", "⏱️"),
    PROFILE("حساب إنستغرام", "👤")
}

data class InstagramMediaItem(
    val id: String = "",
    val url: String = "",
    val hdUrl: String = "",
    val isVideo: Boolean = false,
    val thumbnailUrl: String = "",
    val width: Int = 1080,
    val height: Int = 1920
)

data class InstagramPostInfo(
    val id: String = "",
    val shortcode: String = "",
    val originalUrl: String = "",
    val caption: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatar: String = "",
    val isVerified: Boolean = false,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val isVideo: Boolean = true,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val mediaItems: List<InstagramMediaItem> = emptyList(),
    val takenAtSec: Long = 0L,
    val timeAgoFormatted: String = ""
)

data class InstagramStoryItem(
    val id: String = "",
    val mediaUrl: String = "",
    val isVideo: Boolean = false,
    val thumbnailUrl: String = "",
    val takenAtSec: Long = 0L,
    val durationSec: Int = 0,
    val timeAgoFormatted: String = ""
)

data class InstagramProfileInfo(
    val username: String = "",
    val fullName: String = "",
    val biography: String = "",
    val externalUrl: String = "",
    val hdProfilePicUrl: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val activeStories: List<InstagramStoryItem> = emptyList(),
    val latestPosts: List<InstagramPostInfo> = emptyList()
)
