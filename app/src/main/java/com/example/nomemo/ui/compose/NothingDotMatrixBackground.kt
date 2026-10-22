package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Nothing OS 5.0 60 FPS GPU Canvas Dot-Matrix Background.
 *
 * Highly optimized for 60 FPS scrolling performance & low battery draw:
 * 1. Capped at smooth 60 FPS wave timing (3600ms linear loop).
 * 2. Eliminates all object/Pair allocations inside drawing loops to prevent GC jank.
 * 3. Uses direct primitive alpha blending on drawCircle calls.
 */
@Composable
fun NothingDotMatrixBackground(
    modifier: Modifier = Modifier,
    dotSpacing: Float = 28f,         // Distance between dot centers in px
    baseDotRadius: Float = 1.5f,     // Resting dot size
    waveColor: Color = if (LocalIsDarkTheme.current) Color.White else Color(0xFF121214),
) {
    val coroutineScope = rememberCoroutineScope()
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val touchRipple = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "dot_matrix_wave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_progress",
    )

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchPoint = offset
                    coroutineScope.launch {
                        touchRipple.snapTo(0f)
                        touchRipple.animateTo(
                            targetValue = 800f,
                            animationSpec = tween(1200, easing = LinearOutSlowInEasing),
                        )
                        touchPoint = null
                    }
                }
            },
    ) {
        val totalSpan = size.width + size.height
        if (totalSpan <= 0f) return@Canvas

        val cols = (size.width / dotSpacing).toInt()
        val rows = (size.height / dotSpacing).toInt()
        val waveBandwidth = 0.14f

        val currentRippleRadius = touchRipple.value
        val currentTouch = touchPoint

        for (ix in 0..cols) {
            val posX = ix * dotSpacing

            for (iy in 0..rows) {
                val posY = iy * dotSpacing
                val dotDiagonal = (posX + posY) / totalSpan

                val distanceToWave = abs(dotDiagonal - waveProgress)
                var totalIntensity = if (distanceToWave < waveBandwidth) {
                    1f - (distanceToWave / waveBandwidth)
                } else {
                    0f
                }

                if (currentTouch != null && currentRippleRadius > 0f) {
                    val distFromTouch = hypot(posX - currentTouch.x, posY - currentTouch.y)
                    val distFromRippleWave = abs(distFromTouch - currentRippleRadius)
                    if (distFromRippleWave < 90f) {
                        val waveAlpha = 1f - (currentRippleRadius / 800f)
                        val touchIntensity = (1f - (distFromRippleWave / 90f)) * waveAlpha * 0.8f
                        if (touchIntensity > totalIntensity) {
                            totalIntensity = touchIntensity
                        }
                    }
                }

                val activeAlpha = (0.08f + (totalIntensity * 0.55f)).coerceAtMost(1f)
                val activeRadius = baseDotRadius + (totalIntensity * 1.4f)

                drawCircle(
                    color = waveColor,
                    alpha = activeAlpha,
                    radius = activeRadius,
                    center = Offset(posX, posY),
                )
            }
        }
    }
}

/**
 * Bento Box Card container wrapping NothingDotMatrixBackground.
 */
@Composable
fun BentoMatrixCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(Color(0xFF111111), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
    ) {
        NothingDotMatrixBackground(
            modifier = Modifier.fillMaxSize(),
            dotSpacing = 32f,
        )

        content()
    }
}

/**
 * Preview Composable demonstrating Nothing Dot-Matrix Background.
 */
@Preview
@Composable
fun NothingDotMatrixBackgroundPreview() {
    BentoMatrixCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}