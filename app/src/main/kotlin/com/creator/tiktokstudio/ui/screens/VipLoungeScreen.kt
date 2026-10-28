package com.creator.tiktokstudio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.NativeEngine
import com.creator.tiktokstudio.data.AccountAnalysisReport
import com.creator.tiktokstudio.data.CreatorStudioRepository
import com.creator.tiktokstudio.data.ViralScript
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*

@Composable
fun VipLoungeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var isVipUnlocked by remember { mutableStateOf(false) }
    var activationKeyInput by remember { mutableStateOf("") }
    var keyErrorMessage by remember { mutableStateOf<String?>(null) }

    // VIP Modules
    var selectedVipTab by remember { mutableStateOf(0) }
    val vipTabs = listOf("مولد السكريبتات الفيرال", "محلل الحسابات المتقدم")

    // Script Generator inputs
    var scriptTopic by remember { mutableStateOf("تطبيق سري لا يعرفه أحد") }
    val scriptCategories = listOf("عام وترفيه", "تقنية وشروحات", "تجارة ومال")
    var selectedCategory by remember { mutableStateOf(scriptCategories[0]) }
    var generatedScript by remember { mutableStateOf<ViralScript?>(null) }

    // Account Analyzer inputs
    var usernameInput by remember { mutableStateOf("creator_pro") }
    var followersInput by remember { mutableStateOf("45000") }
    var viewsInput by remember { mutableStateOf("120000") }
    var likesInput by remember { mutableStateOf("8500") }
    var commentsInput by remember { mutableStateOf("620") }
    var analysisResult by remember { mutableStateOf<AccountAnalysisReport?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIP Banner
        item {
            GlassCard(
                borderColor = GoldVip.copy(alpha = 0.7f),
                backgroundColor = DarkSurfaceCard
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GoldVip, GoldVipDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVipUnlocked) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                            contentDescription = null,
                            tint = DeepDarkBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "جناح الـ VIP الملكي الحصري",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldVip
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NeonBadge(
                                text = if (isVipUnlocked) "مفعل ومفتوح 👑" else "مغلق 🔒",
                                textColor = if (isVipUnlocked) GoldVip else NeonPink,
                                borderColor = if (isVipUnlocked) GoldVip else NeonPink
                            )
                        }
                        Text(
                            text = "أقوى أدوات الذكاء الاصطناعي وخوارزميات FYP لصناع المحتوى المحترفين",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // LOCK SCREEN VIEW (If not unlocked)
        if (!isVipUnlocked) {
            item {
                GlassCard(
                    borderColor = GoldVip.copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard,
                    cornerRadius = 24.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(2.dp, GoldVip, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = GoldVip,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "أدخل كود التفعيل لفتح الجناح الملكي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "يتم التحقق من الكود بتشفير فائق السرعة عبر محرك C++ NDK",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = activationKeyInput,
                            onValueChange = {
                                activationKeyInput = it
                                keyErrorMessage = null
                            },
                            placeholder = { Text("مثال: TIKTOK-VIP-2026", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = GoldVip,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = GoldVip,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        if (keyErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = keyErrorMessage!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val isValid = NativeEngine.validateVipKey(activationKeyInput)
                                if (isValid) {
                                    isVipUnlocked = true
                                    Toast.makeText(context, "تم تفعيل جناح الـ VIP بنجاح عبر C++ NDK!", Toast.LENGTH_LONG).show()
                                } else {
                                    keyErrorMessage = "كود التفعيل غير صالح. تأكد من إدخال كود VIP المعتمد."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldVip)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = DeepDarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تفعيل العضوية الملكية VIP",
                                color = DeepDarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick demo key chips
                        Text(
                            text = "أكواد التجربة السريعة المعتمدة:",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("TIKTOK-VIP-2026", "CREATOR-PRO-999").forEach { sampleKey ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceElevated)
                                        .border(1.dp, GoldVip.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { activationKeyInput = sampleKey }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(sampleKey, color = GoldVip, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // UNLOCKED VIP CONTENT
        if (isVipUnlocked) {
            // VIP Module Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    vipTabs.forEachIndexed { index, tabName ->
                        val isSel = selectedVipTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) GoldVip else DarkSurfaceCard)
                                .border(1.dp, if (isSel) GoldVip else GlassBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedVipTab = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                color = if (isSel) DeepDarkBackground else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // MODULE 1: Viral Script Generator
            if (selectedVipTab == 0) {
                item {
                    GlassCard(
                        borderColor = GoldVip.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "مولد السكريبتات الفيرال (Viral Script Architect)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldVip
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scriptCategories) { cat ->
                                val isSel = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) NeonPink else DarkSurfaceElevated)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(cat, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Topic input
                        OutlinedTextField(
                            value = scriptTopic,
                            onValueChange = { scriptTopic = it },
                            label = { Text("موضوع أو عنوان الفيديو القادم", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = GoldVip,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                generatedScript = CreatorStudioRepository.generateVipScript(scriptTopic, selectedCategory)
                                Toast.makeText(context, "تم توليد السكريبت الفيرال بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldVip)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = DeepDarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد سكريبت الفيرال الاحترافي", color = DeepDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Render Generated Script Card
                if (generatedScript != null) {
                    item {
                        val s = generatedScript!!
                        GlassCard(
                            borderColor = NeonCyan.copy(alpha = 0.6f),
                            backgroundColor = DarkSurfaceCard
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "السيناريو الكامل: ${s.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        val fullText = "الخاطف (Hook):\n${s.hook0To3Sec}\n\nالمحتوى الرئيسي:\n${s.coreValue}\n\nدعوة التفاعل:\n${s.callToAction}\n\nتوجيه التصوير:\n${s.cinematographyTip}"
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("script", fullText))
                                        Toast.makeText(context, "تم نسخ السيناريو كاملاً!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = GoldVip)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Step 1: Hook
                            Text(
                                text = "1. الخاطف / Hook (أول 3 ثوانٍ - Stop Scrolling):",
                                style = MaterialTheme.typography.labelLarge.copy(color = NeonPink, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = s.hook0To3Sec,
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Step 2: Core Value
                            Text(
                                text = "2. المحتوى الرئيسي والقيمة (The Core):",
                                style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = s.coreValue,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Step 3: CTA
                            Text(
                                text = "3. دعوة للتفاعل والمتابعة (Call To Action):",
                                style = MaterialTheme.typography.labelLarge.copy(color = GoldVip, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = s.callToAction,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cinematography
                            Text(
                                text = "توجيهات التصوير وحركة الكاميرا:",
                                style = MaterialTheme.typography.labelLarge.copy(color = SuccessGreen, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = s.cinematographyTip,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // MODULE 2: Advanced Account Analyzer
            if (selectedVipTab == 1) {
                item {
                    GlassCard(borderColor = GoldVip.copy(alpha = 0.5f)) {
                        Text(
                            text = "محلل خوارزمية الحسابات والـ FYP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldVip
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Inputs grid
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("اسم المستخدم (@username)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = followersInput,
                                onValueChange = { followersInput = it },
                                label = { Text("المتابعون") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewsInput,
                                onValueChange = { viewsInput = it },
                                label = { Text("متوسط المشاهدات") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = likesInput,
                                onValueChange = { likesInput = it },
                                label = { Text("متوسط الإعجابات") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = commentsInput,
                                onValueChange = { commentsInput = it },
                                label = { Text("متوسط التعليقات") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val followers = followersInput.toLongOrNull() ?: 1000L
                                val views = viewsInput.toLongOrNull() ?: 5000L
                                val likes = likesInput.toLongOrNull() ?: 500L
                                val comments = commentsInput.toLongOrNull() ?: 50L

                                analysisResult = CreatorStudioRepository.analyzeAccount(
                                    username = usernameInput,
                                    followers = followers,
                                    views = views,
                                    likes = likes,
                                    comments = comments
                                )
                                Toast.makeText(context, "تم تحليل الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldVip)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = DeepDarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحليل الحساب وفحص درجة الـ FYP", color = DeepDarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Render Analysis Report
                if (analysisResult != null) {
                    val report = analysisResult!!
                    item {
                        GlassCard(
                            borderColor = SuccessGreen.copy(alpha = 0.6f),
                            backgroundColor = DarkSurfaceCard
                        ) {
                            Text(
                                text = "تقرير التحليل الخوارزمي لحساب @${report.username}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceElevated)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("معدل التفاعل الفعلي", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                        Text(
                                            "${String.format("%.2f", report.engagementRatePercentage)}%",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceElevated)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("تقييم الخوارزمية", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                        Text(
                                            report.algorithmGrade,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = GoldVip,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "حالة الترشيح في إكسبلور:",
                                style = MaterialTheme.typography.labelLarge.copy(color = NeonPink, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = report.viralityStatusArabic,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )

                            // Findings
                            Text(
                                text = "أهم المؤشرات المشخصة:",
                                style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                            )
                            report.keyFindings.forEach { finding ->
                                Text("• $finding", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted), modifier = Modifier.padding(vertical = 2.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recommendations
                            Text(
                                text = "خطة العمل لتصدر التريند القادم:",
                                style = MaterialTheme.typography.labelLarge.copy(color = GoldVip, fontWeight = FontWeight.Bold)
                            )
                            report.actionRecommendations.forEach { rec ->
                                Text("✓ $rec", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary), modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
