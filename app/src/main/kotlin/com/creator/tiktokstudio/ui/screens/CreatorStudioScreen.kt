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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.data.CreatorStudioRepository
import com.creator.tiktokstudio.data.PostingTimeSlot
import com.creator.tiktokstudio.data.VideoIdeaCard
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*

@Composable
fun CreatorStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var selectedStudioTab by remember { mutableStateOf(0) }
    val studioTabs = listOf(
        "أوقات النشر",
        "الهاشتاجات",
        "أفكار يومية",
        "زخرفة البايو",
        "حاسبة الأرباح"
    )

    // Bio styler input
    var bioInputText by remember { mutableStateOf("TikTok Pro") }

    // Earnings calculator state
    var viewsSlider by remember { mutableFloatStateOf(250_000f) }
    var rpmSlider by remember { mutableFloatStateOf(0.65f) }

    // Tinder style cards state
    var currentIdeaIndex by remember { mutableIntStateOf(0) }
    val ideas = CreatorStudioRepository.videoIdeaCards

    // Hashtags filter state
    val hashtagCategories = CreatorStudioRepository.hashtagCategories.keys.toList()
    var selectedCategory by remember { mutableStateOf(hashtagCategories.first()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Header
        item {
            GlassCard(
                borderColor = NeonCyan.copy(alpha = 0.5f),
                backgroundColor = DarkSurfaceCard
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonPink, NeonPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "استوديو وأدوات صناع المحتوى",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "أدوات ذكية متكاملة لتصدر التريند ومضاعفة المشاهدات والأرباح",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Module Tabs Selector (Horizontal scroll)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(studioTabs.size) { index ->
                    val isSelected = selectedStudioTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan else DarkSurfaceCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) NeonCyan else GlassBorder),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedStudioTab = index }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = studioTabs[index],
                            color = if (isSelected) DeepDarkBackground else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // TAB 0: Golden Posting Times
        if (selectedStudioTab == 0) {
            item {
                Text(
                    text = "جدول ساعات النشر الذهبية (أعلى نسبة اكتمال وتفاعل)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
            }

            items(CreatorStudioRepository.goldenPostingTimes) { slot ->
                var reminderSet by remember { mutableStateOf(false) }

                GlassCard(
                    borderColor = if (slot.scorePercentage >= 95) NeonPink.copy(alpha = 0.6f) else GlassBorder,
                    backgroundColor = DarkSurfaceCard
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = slot.dayArabic,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                NeonBadge(
                                    text = "${slot.scorePercentage}% تفاعل",
                                    textColor = if (slot.scorePercentage >= 95) NeonPink else NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "الساعة: ${slot.timeSlotArabic}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonCyan
                                )
                            )

                            Text(
                                text = "المجال المقترح: ${slot.recommendedNiche}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = slot.strategyTip,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                            )
                        }

                        IconButton(
                            onClick = {
                                reminderSet = !reminderSet
                                Toast.makeText(
                                    context,
                                    if (reminderSet) "تم تفعيل تذكير النشر لـ ${slot.dayArabic}" else "تم إلغاء التذكير",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (reminderSet) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "تذكير",
                                tint = if (reminderSet) NeonCyan else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // TAB 1: Trending Hashtags
        if (selectedStudioTab == 1) {
            item {
                Column {
                    Text(
                        text = "الهاشتاجات المتصدرة حسب مجالك",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Categories Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hashtagCategories) { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) NeonPink else DarkSurfaceElevated)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Copy All Button
            item {
                val currentTags = CreatorStudioRepository.hashtagCategories[selectedCategory] ?: emptyList()
                Button(
                    onClick = {
                        val allTagsString = currentTags.joinToString(" ") { it.tag }
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("hashtags", allTagsString))
                        Toast.makeText(context, "تم نسخ جميع الهاشتاجات بنقرة واحدة!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ جميع هاشتاجات $selectedCategory بنقرة واحدة", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            val tags = CreatorStudioRepository.hashtagCategories[selectedCategory] ?: emptyList()
            items(tags) { tagItem ->
                GlassCard(
                    borderColor = GlassBorder,
                    backgroundColor = DarkSurfaceCard
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = tagItem.tag,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "حجم المشاهدات: ${tagItem.reachEstimate}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                NeonBadge(text = tagItem.growthBadge, textColor = NeonPink)
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("hashtag", tagItem.tag))
                                Toast.makeText(context, "تم نسخ ${tagItem.tag}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = NeonCyan)
                        }
                    }
                }
            }
        }

        // TAB 2: Daily Video Ideas (Tinder-style swipeable card)
        if (selectedStudioTab == 2) {
            val idea = ideas[currentIdeaIndex % ideas.size]
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "بطاقة الفكرة اليومية (اسحب أو اضغط للتالي)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassCard(
                        borderColor = NeonPink,
                        backgroundColor = DarkSurfaceCard,
                        cornerRadius = 24.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NeonBadge(text = idea.categoryArabic, textColor = NeonCyan)
                            Text(
                                text = "فكرة #${(currentIdeaIndex % ideas.size) + 1} من ${ideas.size}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hook
                        Text(
                            text = "الجملة الافتتاحية الخاطفة (Hook):",
                            style = MaterialTheme.typography.labelLarge.copy(color = NeonPink, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "« ${idea.hook} »",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Camera Movement
                        Text(
                            text = "حركة وزاوية الكاميرا المقترحة:",
                            style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = idea.cameraMovement,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Execution Secret
                        Text(
                            text = "سر نجاح المقطع:",
                            style = MaterialTheme.typography.labelLarge.copy(color = GoldVip, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = idea.executionSecret,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Audio Vibe
                        Text(
                            text = "الصوت المقترح: ${idea.audioVibe}",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val textToCopy = "فكرة فيديو:\nالخاطف: ${idea.hook}\nحركة الكاميرا: ${idea.cameraMovement}\nالسر: ${idea.executionSecret}"
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("idea", textToCopy))
                                Toast.makeText(context, "تم حفظ ونسخ الفكرة!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ الفكرة", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                currentIdeaIndex = (currentIdeaIndex + 1) % ideas.size
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Text("الفكرة التالية ➔", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TAB 3: Bio Styler
        if (selectedStudioTab == 3) {
            item {
                GlassCard {
                    Text(
                        text = "زخرفة وتنسيق بايو التيك توك الفوري",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اكتب النص أو اسمك بالإنجليزية ليتم تحويله فوراً إلى خطوط إبداعية مقبولة في تيك توك:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bioInputText,
                        onValueChange = { bioInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text("اكتب نص البايو هنا...") }
                    )
                }
            }

            val styledList = CreatorStudioRepository.styleBioText(bioInputText)
            items(styledList) { (styleName, styledText) ->
                GlassCard(
                    borderColor = GlassBorder,
                    backgroundColor = DarkSurfaceCard
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = styleName,
                                style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = styledText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("bio", styledText))
                                Toast.makeText(context, "تم نسخ: $styledText", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = NeonPink)
                        }
                    }
                }
            }
        }

        // TAB 4: Views Earnings Calculator
        if (selectedStudioTab == 4) {
            item {
                val (baseEarnings, maxPotential) = CreatorStudioRepository.calculateEarnings(viewsSlider.toLong(), rpmSlider.toDouble())

                GlassCard(
                    borderColor = GoldVip.copy(alpha = 0.6f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Text(
                        text = "حاسبة أرباح مشاهدات تيك توك التفاعلية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldVip
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Earnings Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, GoldVip.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الأرباح المتوقعة بالدولار",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$${String.format("%.2f", baseEarnings)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldVip
                                )
                            )
                            Text(
                                text = "مع المكافآت والبثوث تصل إلى: $${String.format("%.2f", maxPotential)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = SuccessGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Views Slider
                    Text(
                        text = "عدد المشاهدات: ${String.format("%,d", viewsSlider.toLong())} مشاهدة",
                        style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary)
                    )
                    Slider(
                        value = viewsSlider,
                        onValueChange = { viewsSlider = it },
                        valueRange = 10_000f..5_000_000f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // RPM Slider
                    Text(
                        text = "معدل الـ RPM (لكل 1000 مشاهدة مؤهلة): $${String.format("%.2f", rpmSlider)}",
                        style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary)
                    )
                    Slider(
                        value = rpmSlider,
                        onValueChange = { rpmSlider = it },
                        valueRange = 0.20f..1.80f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonPink,
                            inactiveTrackColor = DarkSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tips to increase RPM
                    Text(
                        text = "نصائح لرفع معدل الـ RPM في حسابك:",
                        style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "• اجعل مدة مقاطعك أطول من دقيقة (1:01 دقيقة) للتأهل لبرنامج TikTok Creator Rewards.\n• ركز على موضوعات التجارة، التقنية، والعقارات حيث يكون معلنوها ذوي ميزانيات أعلى.\n• استهدف جمهور الخليج وأمريكا وأوروبا لرفع سعر الألف مشاهدة بمعدل 4 أضعاف.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
