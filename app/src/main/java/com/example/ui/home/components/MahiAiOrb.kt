package com.example.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun MahiAiOrb(
    state: OrbState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Slow orbital rotation for ambient rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.THINKING -> 3500
                    OrbState.LISTENING -> 6000
                    else -> 18000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_rotation"
    )

    // Reverse rotation for counter ring
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Breathing pulse for center core
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = if (state == OrbState.LISTENING) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.LISTENING) 700 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Equalizer wave factor
    val waveFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_factor"
    )

    Box(
        modifier = modifier
            .size(310.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag("ai_orb_container"),
        contentAlignment = Alignment.Center
    ) {
        // Background tech canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(coreScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Soft radial ambient glow in center
            val glowColor = when (state) {
                OrbState.LISTENING -> Color(0xFF00D2FF)
                OrbState.THINKING -> Color(0xFFA855F7)
                OrbState.SPEAKING -> Color(0xFF38BDF8)
                OrbState.IDLE -> ElectricBlue
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.28f),
                        Color(0xFF131D33).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 0.95f
                )
            )

            // Inner dark glass core circle
            drawCircle(
                color = Color(0xFF0D1424).copy(alpha = 0.75f),
                center = center,
                radius = maxR * 0.76f
            )
            drawCircle(
                color = Color(0xFF1C2C4A),
                center = center,
                radius = maxR * 0.76f,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Faint outer compass rings
            drawCircle(
                color = Color(0xFF1B283E).copy(alpha = 0.6f),
                center = center,
                radius = maxR * 0.88f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF162032).copy(alpha = 0.5f),
                center = center,
                radius = maxR * 0.98f,
                style = Stroke(width = 0.8.dp.toPx())
            )
        }

        // Layer 1: Rotating forward technical arcs
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
        ) {
            val maxR = size.minDimension / 2f

            // Prominent glowing blue arc (top-left)
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF4E85FF),
                        Color.Transparent
                    )
                ),
                startAngle = 170f,
                sweepAngle = 110f,
                useCenter = false,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Bottom-right secondary cyan arc
            drawArc(
                color = Color(0xFF60A5FA).copy(alpha = 0.7f),
                startAngle = 20f,
                sweepAngle = 65f,
                useCenter = false,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Layer 2: Rotating reverse dashed ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(reverseRotation)
        ) {
            // Right-side prominent white-blue highlight arc
            drawArc(
                color = Color(0xFFC7D2FE).copy(alpha = 0.85f),
                startAngle = 330f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Center Content Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            // "— AI ASSISTANT —"
            Text(
                text = "— AI ASSISTANT —",
                color = Color(0xFF7DD3FC),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Large stylized futuristic "M.A.H.I"
            Row(verticalAlignment = Alignment.CenterVertically) {
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA),
                        Color(0xFFA78BFA),
                        Color(0xFFF472B6)
                    )
                )

                Text(
                    text = "M.A.H.I",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = gradientBrush,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 6.sp
                    ),
                    modifier = Modifier.testTag("orb_title_text")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Equalizer Waveform Bars / Dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.height(18.dp)
            ) {
                val barHeights = listOf(
                    3.dp, 5.dp, 8.dp, 12.dp, 16.dp, 10.dp, 15.dp, 18.dp,
                    14.dp, 9.dp, 16.dp, 11.dp, 7.dp, 4.dp, 3.dp
                )

                barHeights.forEachIndexed { index, baseHeight ->
                    val dynamicMultiplier = if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                        val phase = ((index * 0.25f + waveFactor) % 1.0f)
                        0.4f + (phase * 0.8f)
                    } else {
                        0.4f
                    }

                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(baseHeight * dynamicMultiplier)
                            .clip(CircleShape)
                            .background(if (index % 2 == 0) Color(0xFF60A5FA) else Color(0xFFA855F7))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // State Subtitle Text: "HOW CAN I HELP YOU?" or active status
            val statusText = when (state) {
                OrbState.LISTENING -> "LISTENING TO YOU..."
                OrbState.THINKING -> "THINKING..."
                OrbState.SPEAKING -> "MAHI IS SPEAKING..."
                OrbState.IDLE -> "HOW CAN I HELP YOU?"
            }

            Text(
                text = statusText,
                color = when (state) {
                    OrbState.LISTENING -> Color(0xFF38BDF8)
                    OrbState.THINKING -> Color(0xFFC084FC)
                    OrbState.SPEAKING -> Color(0xFF4ADE80)
                    OrbState.IDLE -> Color(0xFF75859B)
                },
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.2.sp
            )
        }
    }
}
