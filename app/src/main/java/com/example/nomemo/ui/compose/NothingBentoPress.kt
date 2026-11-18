package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Jetpack Compose modifier for Nothing OS 5.0 Bento Grid items.
 *
 * Provides a mechanical 120Hz flagship tactile spring press:
 * - Scales down to 0.965f on press, 1.0f on release (stiffness = 500f, dampingRatio = 0.75f).
 * - Triggers subtle HapticFeedbackType.TextHandleMove instantly on pointer down.
 * - Executes scale transformations purely via Modifier.graphicsLayer for 120 FPS performance.
 */
fun Modifier.nothingBentoPress(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    stiffness: Float = 500f,
    dampingRatio: Float = 0.75f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
): Modifier = composed {
    if (!enabled) return@composed this

    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(value = false) }

    // Unified 120Hz flagship tactile spring
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
        ),
        label = "nothingBentoPressScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = {
                    onClick()
                },
                onLongPress = onLongClick?.let { callback ->
                    { callback() }
                },
            )
        }
}

/**
 * Universal tactile spring press modifier for pills, buttons, and icons.
 * Scales down to 0.965f on press, 1.0f on release (stiffness = 500f, dampingRatio = 0.75f).
 */
fun Modifier.tactilePress(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    stiffness: Float = 500f,
    dampingRatio: Float = 0.75f,
    onClick: () -> Unit = {},
): Modifier = composed {
    if (!enabled) return@composed this

    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
        ),
        label = "tactilePressScale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = {
                    onClick()
                },
            )
        }
}

/**
 * Preview Composable demonstrating the mechanical Nothing Bento Press modifier.
 */
@Preview
@Composable
fun NothingBentoPressPreview() {
    Box(
        modifier = Modifier
            .size(width = 160.dp, height = 120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .nothingBentoPress(
                onClick = {},
                onLongClick = {},
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PRESS ME",
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}