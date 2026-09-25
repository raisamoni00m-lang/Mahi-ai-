package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DeepMidnightNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SubtleNavyRadial
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

@Composable
fun MahiCompanionScreen(
    onRevisitSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )
    val glowIntensity by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .drawBehind {
                val center = Offset(size.width * 0.5f, size.height * 0.38f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SubtleNavyRadial.copy(alpha = 0.65f),
                            DeepMidnightNavy,
                            DarkNavyBackground
                        ),
                        center = center,
                        radius = size.width * 0.95f
                    )
                )
            }
            .testTag("companion_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Heart
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricBlue.copy(alpha = 0.6f * glowIntensity),
                                ElectricBlue.copy(alpha = 0.15f * glowIntensity),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Mahi AI Heart",
                    tint = ElectricBlue,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Mahi AI is Active",
                color = TextPureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mahi AI is configured and ready in the background. Her brain runs on Gemini with battery optimisation disabled.",
                color = TextSoftGray,
                fontSize = 13.5.sp,
                lineHeight = 19.5.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1728))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Background & AI Engine Ready",
                            color = TextPureWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Encrypted key and background service running",
                            color = Color(0xFF8291A8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onRevisitSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF0E1626)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = TextPureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reconfigure Setup & Keys",
                    color = TextPureWhite,
                    fontSize = 14.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF0E1626)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = TextPureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Android System Settings",
                    color = TextPureWhite,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}
