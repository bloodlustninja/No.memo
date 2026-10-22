package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated Nothing OS Audio Waveform Indicator with Fixed Outer Bounds.
 *
 * Keeps the container height strictly fixed (24.dp) so dynamic bar animations
 * do NOT trigger parent layout re-measurements or vertical heartbeat shaking.
 */
@Composable
fun NothingAudioWaveform(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 6,
    maxHeight: Dp = 16.dp,
) {
    val barHeights = remember { List(barCount) { Animatable(0.25f) } }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            barHeights.forEachIndexed { index, animatable ->
                val duration = 280 + (index * 70)
                animatable.animateTo(
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                )
            }
        } else {
            barHeights.forEach { it.snapTo(0.25f) }
        }
    }

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        barHeights.forEach { heightRatio ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(maxHeight * heightRatio.value)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isRecording) Color(0xFFD71921) else Color(0xFF666666)),
            )
        }
    }
}