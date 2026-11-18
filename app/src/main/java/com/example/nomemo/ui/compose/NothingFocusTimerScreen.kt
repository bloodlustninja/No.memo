package com.example.nomemo.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.ui.anim.NothingAudioFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nothing OS 5.0 "Focus Matrix" — Minimalist Pomodoro Studio.
 *
 * Implements a hardware-inspired circular dot-matrix glyph progress ring,
 * large-format NDot mechanical digits, and tactile focus session tracking.
 */
@Composable
fun NothingFocusTimerScreen(
    noteTitle: String = "UNTITLED NOTE",
    onSessionComplete: (minutes: Int) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    var selectedMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    val totalSeconds = remember(selectedMinutes) { selectedMinutes * 60 }
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds == 0) 0f else (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0 && isActive) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0) {
                isRunning = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                NothingAudioFeedback.playCheckChime()
                onSessionComplete(selectedMinutes)
            }
        }
    }

    val minutesStr = String.format(Locale.US, "%02d", remainingSeconds / 60)
    val secondsStr = String.format(Locale.US, "%02d", remainingSeconds % 60)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding(),
    ) {
        // GPU Canvas Dot Matrix background
        NothingDotMatrixBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                KineticDotMatrixText(
                    text = "FOCUS // MATRIX",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                )

                // Status LED
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFFD71921) else Color.DarkGray),
                )
            }

            // Note Context Card
            FrostedBentoCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "FOCUSING ON NOTE:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = noteTitle.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
            }

            // Circular Glyph Dot-Matrix Timer Ring
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Procedural 60-dot circular glyph ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width * 0.42f
                    val totalDots = 48
                    val litDots = (totalDots * progress).toInt()

                    for (i in 0 until totalDots) {
                        val angle = (i.toFloat() / totalDots.toFloat()) * 2f * Math.PI - (Math.PI / 2f)
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()

                        val isLit = i <= litDots
                        val dotColor = if (isLit) Color(0xFFD71921) else Color(0x33FFFFFF)
                        val dotRadius = if (isLit) 3.5.dp.toPx() else 2.dp.toPx()

                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y),
                        )
                    }
                }

                // Center Large-Format Monospace Digital Digits
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = minutesStr,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "min_flip",
                    ) { min ->
                        Text(
                            text = min,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Text(
                        text = ":",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFFD71921) else Color.White,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )

                    AnimatedContent(
                        targetState = secondsStr,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "sec_flip",
                    ) { sec ->
                        Text(
                            text = sec,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            // Duration Mode Selector Pills
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(15 to "15 MIN", 25 to "25 MIN", 50 to "50 MIN").forEach { (mins, label) ->
                    val isSelected = selectedMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFD71921) else Color(0xFF141416))
                            .border(1.dp, if (isSelected) Color(0xFFD71921) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRunning) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                NothingAudioFeedback.playTactileClick()
                                selectedMinutes = mins
                                remainingSeconds = mins * 60
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            // Main Mechanical Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Reset Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF1C1C1E))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(26.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NothingAudioFeedback.playTactileClick()
                            isRunning = false
                            remainingSeconds = selectedMinutes * 60
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "RESET",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                }

                // Start / Pause Capsule
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isRunning) Color(0xFF222226) else Color(0xFFD71921))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NothingAudioFeedback.playTactileClick()
                            isRunning = !isRunning
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isRunning) "PAUSE SESSION" else "START FOCUS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}
