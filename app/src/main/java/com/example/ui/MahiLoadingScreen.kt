package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DeepMidnightNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MahiTheme
import com.example.ui.theme.SubtleNavyRadial
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

/**
 * Premium modern mobile AI assistant loading screen for "Mahi AI".
 * Minimal aesthetic, dark navy background, glowing blue heart, bold typography,
 * soft-gray status text, and circular blue spinner.
 */
@Composable
fun MahiLoadingScreen(modifier: Modifier = Modifier) {
    val pulseTransition = rememberInfiniteTransition(label = "heart_pulse")
    val heartScale by pulseTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )
    val glowIntensity by pulseTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    val spinnerTransition = rememberInfiniteTransition(label = "spinner_rotation")
    val spinnerRotation by spinnerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .drawBehind {
                val center = Offset(size.width * 0.5f, size.height * 0.44f)
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
            .testTag("mahi_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(y = (-22).dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .testTag("heart_container"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricBlue.copy(alpha = 0.55f * glowIntensity),
                                ElectricBlue.copy(alpha = 0.20f * glowIntensity),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Mahi AI Heart Icon",
                    tint = ElectricBlue,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer(
                            scaleX = heartScale,
                            scaleY = heartScale
                        )
                        .testTag("glowing_heart")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                color = TextPureWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("app_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.tagline),
                color = TextSoftGray,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("loading_message")
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .testTag("loading_spinner"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = ElectricBlue.copy(alpha = 0.35f),
                        startAngle = spinnerRotation,
                        sweepAngle = 78f,
                        useCenter = false,
                        style = Stroke(
                            width = 4.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                    drawArc(
                        color = ElectricBlue,
                        startAngle = spinnerRotation,
                        sweepAngle = 72f,
                        useCenter = false,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, widthDp = 390, heightDp = 844)
@Composable
fun MahiLoadingScreenPreview() {
    MahiTheme {
        MahiLoadingScreen()
    }
}
