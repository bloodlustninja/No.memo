package com.example.nomemo.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.data.entity.Note
import com.example.nomemo.ui.anim.NothingAudioFeedback
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nothing OS 5.0 Standby Ambient Desk Clock & Memo HUD.
 *
 * Designed for desk docks or charging stands: an ultra-low power pure black
 * OLED screen featuring a monumental NDot digital clock, ambient breathing
 * Glyph simulator ring, and pinned note ticker stream.
 */
@Composable
fun NothingAmbientDeskScreen(
    isVisible: Boolean,
    pinnedNotes: List<Note>,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val haptic = LocalHapticFeedback.current

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US) }
    var currentTimeStr by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDateStr by remember { mutableStateOf(dateFormat.format(Date())) }

    // Live clock ticker
    LaunchedEffect(isVisible) {
        while (isVisible) {
            val now = Date()
            currentTimeStr = timeFormat.format(now)
            currentDateStr = dateFormat.format(now).uppercase()
            delay(1000)
        }
    }

    // Breathing Glyph Animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glyph")
    val breathingRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glyph_breath",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glyph_glow",
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NothingAudioFeedback.playTactileClick()
                            onExit()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Procedural Background Glyph Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.38f * breathingRadius

                // 36-dot outer glyph halo
                val dotCount = 36
                for (i in 0 until dotCount) {
                    val angle = (i * (2 * Math.PI) / dotCount).toFloat()
                    val x = center.x + baseRadius * cos(angle)
                    val y = center.y + baseRadius * sin(angle)
                    val isAccent = i % 9 == 0
                    drawCircle(
                        color = if (isAccent) Color(0xFFD71921).copy(alpha = glowAlpha) else Color.White.copy(alpha = 0.20f * glowAlpha),
                        radius = if (isAccent) 3.5.dp.toPx() else 2.dp.toPx(),
                        center = Offset(x, y),
                    )
                }

                // Inner fine orbit ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = baseRadius * 0.72f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Status Ticker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD71921)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESK AMBIENT HUD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF888892),
                            letterSpacing = 1.5.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141418))
                            .border(1.dp, Color(0xFF282830), RoundedCornerShape(8.dp))
                            .clickable {
                                NothingAudioFeedback.playTactileClick()
                                onExit()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "EXIT [2-TAP]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD71921),
                        )
                    }
                }

                // Center Monumental NDot Clock
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentDateStr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAF),
                        letterSpacing = 2.sp,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentTimeStr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "● 120 FPS // OLED LOW POWER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF55555F),
                        letterSpacing = 1.sp,
                    )
                }

                // Bottom Pinned Notes Stream
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        val activePinned = pinnedNotes.first()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xCC111116))
                                .border(1.dp, Color(0xFF262630), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "PINNED MEMO // ${activePinned.title.ifBlank { "UNTITLED" }.uppercase()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD71921),
                                    )
                                    Text(
                                        text = "${pinnedNotes.size} ACTIVE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Color(0xFF777782),
                                    )
                                }
                                if (activePinned.body.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = activePinned.body,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFFCCCCD0),
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "NO PINNED MEMOS // STANDBY MODE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF444450),
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
