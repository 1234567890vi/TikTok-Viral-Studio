package com.creator.tiktokstudio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.ui.theme.DeepDarkBackground
import com.creator.tiktokstudio.ui.theme.TextPrimary
import com.creator.tiktokstudio.ui.theme.TextSecondary

@Composable
fun AppMaintenanceScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    // Unskippable
    BackHandler(enabled = true) {}

    val infiniteTransition = rememberInfiniteTransition(label = "rotateWrench")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationAngle"
    )

    val maintenanceAmber = Color(0xFFFFB300)
    val maintenanceDark = Color(0x33FFB300)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, maintenanceAmber, RoundedCornerShape(26.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xF516140E)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(maintenanceDark)
                        .border(2.dp, maintenanceAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = maintenanceAmber,
                        modifier = Modifier
                            .size(46.dp)
                            .rotate(rotationAngle)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "🛠️ النظام في وضع الصيانة",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = maintenanceAmber
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (message.isNotBlank()) message else "نقوم حالياً بإجراء تحديثات وتحسينات دورية على الخوادم لضمان أعلى سرعة واستقرار. سنعود للعمل قريباً جداً!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = maintenanceAmber,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "جاري مزامنة الأنظمة والعودة التلقائية...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
