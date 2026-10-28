package com.creator.tiktokstudio

import com.creator.tiktokstudio.core.NativeEngine
import com.creator.tiktokstudio.data.CreatorStudioRepository
import org.junit.Assert.*
import org.junit.Test

class CreatorStudioTests {

    @Test
    fun testVipKeyValidation() {
        // Test master VIP keys
        assertTrue(NativeEngine.validateVipKey("TIKTOK-VIP-2026"))
        assertTrue(NativeEngine.validateVipKey("CREATOR-PRO-999"))
        assertTrue(NativeEngine.validateVipKey("VIRAL-STUDIO-GOLD"))

        // Test invalid keys
        assertFalse(NativeEngine.validateVipKey("WRONG-KEY-1234"))
        assertFalse(NativeEngine.validateVipKey(""))
    }

    @Test
    fun testDownloadSpeedCalculation() {
        // 10 MB in 2000 ms = 5 MB/s
        val speed = NativeEngine.calculateDownloadSpeed(10 * 1024 * 1024L, 2000L)
        assertEquals(5.0, speed, 0.1)
    }

    @Test
    fun testFilenameSanitization() {
        val sanitized = NativeEngine.sanitizeFilename("مقطع تيك توك رهيب: 100% فعال?", "mp4")
        assertTrue(sanitized.startsWith("TikTokStudio_"))
        assertTrue(sanitized.endsWith(".mp4"))
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("?"))
    }

    @Test
    fun testEarningsCalculator() {
        val (base, max) = CreatorStudioRepository.calculateEarnings(1_000_000L, 0.80)
        assertEquals(800.0, base, 0.01)
        assertTrue(max > base)
    }

    @Test
    fun testBioStyling() {
        val styled = CreatorStudioRepository.styleBioText("Viral")
        assertTrue(styled.isNotEmpty())
        assertTrue(styled.any { it.first.contains("Bold Sans") })
    }

    @Test
    fun testAccountAnalysis() {
        val report = CreatorStudioRepository.analyzeAccount(
            username = "viral_user",
            followers = 100_000L,
            views = 500_000L,
            likes = 45_000L,
            comments = 3_000L
        )
        assertNotNull(report)
        assertTrue(report.engagementRatePercentage > 0.0)
        assertTrue(report.keyFindings.isNotEmpty())
        assertTrue(report.actionRecommendations.isNotEmpty())
    }
}
