package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nothing Phone Glyph Interface Hardware LED Simulator.
 *
 * Meticulously models the physical Glyph LED geometry of Nothing Phone:
 * 1. Camera Ring (Top Left)
 * 2. Diagonal Slash (Top Right)
 * 3. Central G-Loop Circuit (Center)
 * 4. Exclamation Bar & Dot (Bottom)
 *
 * Light strips dynamically illuminate and pulse in real-time response to
 * speech volume (RMS dB) and voice memo playback.
 */
@Composable
fun NothingGlyphInterface(
    modifier: Modifier = Modifier,
    volume: Float = 0f,          // 0f to 10f+ dB input
    isRecording: Boolean = false,
    size: Dp = 200.dp,
) {
    val normalizedVol = (volume / 8f).coerceIn(0f, 1f)
    val glowIntensity = remember { Animatable(0.2f) }

    LaunchedEffect(volume) {
        glowIntensity.animateTo(
            targetValue = (0.25f + normalizedVol * 0.75f).coerceIn(0.25f, 1.0f),
            animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val recPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rec_red_led",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val strokeWidth = 3.5.dp.toPx()
            val baseAlpha = 0.18f
            val activeAlpha = glowIntensity.value

            val ledColor = Color.White.copy(alpha = activeAlpha)
            val traceColor = Color.White.copy(alpha = baseAlpha)

            // 1. Camera Ring (Top Left)
            val camCenter = Offset(w * 0.32f, h * 0.24f)
            val camRadius = w * 0.16f
            // Base trace
            drawCircle(color = traceColor, radius = camRadius, center = camCenter, style = Stroke(strokeWidth))
            // Active glow
            drawCircle(color = ledColor, radius = camRadius, center = camCenter, style = Stroke(strokeWidth))

            // Recording Red LED dot inside Camera Ring
            if (isRecording) {
                drawCircle(
                    color = Color(0xFFD71921).copy(alpha = recPulseAlpha),
                    radius = 3.dp.toPx(),
                    center = Offset(camCenter.x + camRadius * 0.65f, camCenter.y - camRadius * 0.65f),
                )
            }

            // 2. Diagonal Slash (Top Right)
            val slashStart = Offset(w * 0.68f, h * 0.12f)
            val slashEnd = Offset(w * 0.84f, h * 0.28f)
            drawLine(traceColor, slashStart, slashEnd, strokeWidth, StrokeCap.Round)
            drawLine(ledColor, slashStart, slashEnd, strokeWidth, StrokeCap.Round)

            // 3. Central G-Loop Ring
            val gCenter = Offset(w * 0.5f, h * 0.52f)
            val gRadius = w * 0.26f
            // Arc representing the central loop with Nothing's characteristic break
            drawArc(
                color = traceColor,
                startAngle = 30f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(gCenter.x - gRadius, gCenter.y - gRadius),
                size = Size(gRadius * 2, gRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = ledColor,
                startAngle = 30f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(gCenter.x - gRadius, gCenter.y - gRadius),
                size = Size(gRadius * 2, gRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )

            // 4. Bottom Vertical Exclamation Strip
            val barStart = Offset(w * 0.5f, h * 0.80f)
            val barEnd = Offset(w * 0.5f, h * 0.90f)
            drawLine(traceColor, barStart, barEnd, strokeWidth, StrokeCap.Round)
            drawLine(ledColor, barStart, barEnd, strokeWidth, StrokeCap.Round)

            // 5. Bottom Dot
            val dotCenter = Offset(w * 0.5f, h * 0.95f)
            drawCircle(color = traceColor, radius = strokeWidth * 0.8f, center = dotCenter)
            drawCircle(color = ledColor, radius = strokeWidth * 0.8f, center = dotCenter)
        }
    }
}
