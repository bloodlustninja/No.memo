package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Lightweight 2D "Throw-Out" Mechanical Bento Card with Subtle Translucent Red (#4DD71921) Reveal
 * and low 120f dismiss threshold for effortless flicking.
 */
@Composable
fun NothingThrowCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var hasVibrated by remember { mutableStateOf(value = false) }

    // Lightweight effortless dismiss threshold (120f instead of 260f)
    val dismissThreshold = 120f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x4DD71921)), // Subtle 30% translucent crimson reveal
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value.x
                    translationY = offset.value.y

                    // Smooth rotational torque
                    rotationZ = (offset.value.x / 18f).coerceIn(-24f, 24f)

                    // Card alpha fade on drag
                    val totalDistance = hypot(offset.value.x, offset.value.y)
                    alpha = (1f - (totalDistance / 600f)).coerceIn(0f, 1f)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val currentOffset = offset.value + dragAmount
                                offset.snapTo(currentOffset)

                                val currentDistance = hypot(currentOffset.x, currentOffset.y)
                                if ((currentDistance >= dismissThreshold) && !hasVibrated) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasVibrated = true
                                } else if (currentDistance < dismissThreshold) {
                                    hasVibrated = false
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val current = offset.value
                                val totalDistance = hypot(current.x, current.y)

                                if (totalDistance >= dismissThreshold) {
                                    val throwMultiplier = 1400f / totalDistance
                                    val targetExit = Offset(
                                        x = current.x * throwMultiplier,
                                        y = current.y * throwMultiplier,
                                    )

                                    offset.animateTo(
                                        targetValue = targetExit,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessHigh,
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                        ),
                                    )
                                    onDismiss()
                                } else {
                                    offset.animateTo(
                                        targetValue = Offset.Zero,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium,
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                        ),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                    ),
                                )
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

/**
 * Preview Composable demonstrating Nothing Throw Card motion.
 */
@Preview
@Composable
fun NothingThrowCardPreview() {
    NothingThrowCard(onDismiss = {}) {
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
                text = "FLICK OR THROW TO DISMISS",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}