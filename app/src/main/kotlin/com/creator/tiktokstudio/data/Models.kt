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
    INSTAGRAM,
    SNAPCHAT
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

data class InstagramConnectedFriend(
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val interactionCount: Int = 1,
    val relationType: String = "إشارة في المنشورات" // "وسم في المنشور", "تعاون مشترك", "إشارة في البايو", "تفاعل وتعليق"
)

data class InstagramBlockedUser(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = ""
)

data class InstagramCloseFriend(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = ""
)

data class InstagramDirectFriend(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val messageCount: Int = 0,
    val lastMessageSnippet: String = ""
)

data class InstagramPrivacyData(
    val blockedUsers: List<InstagramBlockedUser> = emptyList(),
    val closeFriends: List<InstagramCloseFriend> = emptyList(),
    val directFriends: List<InstagramDirectFriend> = emptyList()
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
    val latestPosts: List<InstagramPostInfo> = emptyList(),

    // Deep OSINT & Account History fields (100% Real - Zero Fake Data)
    val userPk: String = "",
    val dateJoined: String = "",
    val dateJoinedTimestamp: Long = 0L,
    val formerUsernamesCount: Int = 0,
    val formerUsernames: List<String> = emptyList(),
    val accountBasedIn: String = "",
    val accountType: String = "Personal", // Personal, Creator, Business
    val categoryName: String = "",
    val bioLinks: List<String> = emptyList(),
    val topConnectedFriends: List<InstagramConnectedFriend> = emptyList(),
    val privacyData: InstagramPrivacyData? = null
)

// ==============================================================================
// Snapchat Models (Spotlight, Public Stories, Highlights & Profile)
// ==============================================================================
enum class SnapchatContentType(val titleArabic: String, val badgeIcon: String) {
    SPOTLIGHT("سناب أضواء (Spotlight)", "⚡"),
    STORY("قصة عامة (Public Story)", "⏱️"),
    HIGHLIGHT("مقتطف مميز (Curated Highlight)", "⭐"),
    PROFILE("حساب سناب شات", "👻")
}

data class SnapchatMediaItem(
    val id: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val isVideo: Boolean = true,
    val title: String = "",
    val storyType: String = "Spotlight" // "Spotlight", "Curated", "Story"
)

data class SnapchatPostInfo(
    val id: String = "",
    val originalUrl: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatar: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val isVideo: Boolean = true,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L
)

data class SnapchatProfileInfo(
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val websiteUrl: String = "",
    val address: String = "",
    val profilePictureUrl: String = "",
    val bitmojiAvatarUrl: String = "",
    val snapcodeImageUrl: String = "",
    val subscriberCount: Long = 0L,
    val isVerified: Boolean = false,
    val categoryStringId: String = "",
    val publicStories: List<SnapchatMediaItem> = emptyList(),
    val curatedHighlights: List<SnapchatMediaItem> = emptyList(),
    val spotlightHighlights: List<SnapchatMediaItem> = emptyList(),

    // Full OSINT & Account History fields (v2.9.1)
    val accountCreationDate: String = "",
    val accountCreationTimestamp: Long = 0L,
    val snapScore: Long = 0L,
    val snapScoreFormatted: String = "",
    val friendsCount: Long = 0L,
    val friendsCountFormatted: String = "",
    val accountType: String = "حساب شخصي 👻",
    val has3dBitmoji: Boolean = false
)
