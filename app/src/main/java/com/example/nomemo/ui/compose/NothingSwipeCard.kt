package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Swipe-to-Action with Mechanical Industrial Red (#D71921) Reveal for Nothing OS 5.0 Bento Cards.
 *
 * Motion:
 * - Industrial Crimson action reveal background.
 * - Drags horizontally with threshold at -220f.
 * - Triggers HapticFeedbackType.LongPress when crossing threshold.
 * - Snaps off-screen to -1000f on delete, or snaps back with non-bouncy high-stiffness spring.
 */
@Composable
fun NothingSwipeCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var hasVibrated by remember { mutableStateOf(value = false) }
    val threshold = -220f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFD71921)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newOffset = (offsetX.value + delta).coerceAtMost(0f)
                            offsetX.snapTo(newOffset)

                            if (newOffset <= threshold && !hasVibrated) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasVibrated = true
                            } else if (newOffset > threshold) {
                                hasVibrated = false
                            }
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value <= threshold) {
                                offsetX.animateTo(
                                    targetValue = -1000f,
                                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                )
                                onDelete()
                            } else {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                    ),
                                )
                            }
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

/**
 * Preview Composable demonstrating Nothing Swipe-to-Action Card.
 */
@Preview
@Composable
fun NothingSwipeCardPreview() {
    NothingSwipeCard(onDelete = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "SWIPE LEFT TO DELETE",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}