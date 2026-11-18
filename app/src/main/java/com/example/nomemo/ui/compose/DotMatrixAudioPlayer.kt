package com.example.nomemo.ui.compose

import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * Interactive Dot-Matrix Audio Player for Voice Notes.
 * Features:
 * - Play / Pause toggle with mechanical Nothing crimson icon
 * - Dynamic scrubbable dot matrix amplitude visualizer
 * - Real-time elapsed time vs total duration telemetry (00:14 // 00:30)
 */
@Composable
fun DotMatrixAudioPlayer(
    audioUri: String,
    durationMs: Long,
    waveform: List<Float>,
    modifier: Modifier = Modifier,
    onPlaybackComplete: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val unplayedColor = if (isDark) Color(0x55FFFFFF) else Color(0x44000000)

    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val activeWaveform = remember(waveform) {
        if (waveform.isNotEmpty()) waveform
        else List(24) { (0.25f + ((it * 7) % 10) * 0.075f) }
    }

    DisposableEffect(audioUri) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun startPlayback() {
        try {
            val player = mediaPlayer ?: MediaPlayer().also { mp ->
                if (audioUri.startsWith("content://") || audioUri.startsWith("file://")) {
                    mp.setDataSource(context, android.net.Uri.parse(audioUri))
                } else {
                    mp.setDataSource(audioUri)
                }
                mp.prepare()
                mp.setOnCompletionListener {
                    isPlaying = false
                    currentProgress = 0f
                    onPlaybackComplete()
                }
                mediaPlayer = mp
            }
            player.start()
            isPlaying = true
        } catch (_: Exception) {
            isPlaying = false
        }
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    // Polling progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val duration = player.duration.coerceAtLeast(1)
                    currentProgress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
            }
            kotlinx.coroutines.delay(50L)
        }
    }

    val elapsedSec = ((durationMs * currentProgress) / 1000).toInt()
    val totalSec = (durationMs / 1000).toInt()
    val timeTelemetry = remember(elapsedSec, totalSec) {
        String.format(Locale.US, "%02d:%02d // %02d:%02d", elapsedSec / 60, elapsedSec % 60, totalSec / 60, totalSec % 60)
    }

    FrostedBentoCard(
        cornerRadius = 16.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Play / Pause Capsule Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) NothingThemeColors.iconActive else NothingThemeColors.surface)
                    .border(1.dp, NothingThemeColors.surfaceBorder, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isPlaying) pausePlayback() else startPlayback()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Color.Black else NothingThemeColors.iconActive,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform & Telemetry Column
            Column(modifier = Modifier.weight(1f)) {
                // Interactive Waveform Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(activeWaveform) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                currentProgress = fraction
                                mediaPlayer?.let { player ->
                                    val targetMs = (player.duration * fraction).toInt()
                                    player.seekTo(targetMs)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                ) {
                    val barWidth = 3.dp.toPx()
                    val barSpacing = 2.dp.toPx()
                    val totalBars = activeWaveform.size
                    val totalWidth = (barWidth + barSpacing) * totalBars
                    val startX = (size.width - totalWidth).coerceAtLeast(0f) / 2f

                    for (i in 0 until totalBars) {
                        val barFraction = i.toFloat() / totalBars.toFloat()
                        val isPlayed = barFraction <= currentProgress

                        val barHeight = (size.height * activeWaveform[i].coerceIn(0.15f, 1f))
                        val x = startX + i * (barWidth + barSpacing)
                        val yTop = (size.height - barHeight) / 2f

                        val barColor = if (isPlayed) {
                            NothingThemeColors.accentCrimson
                        } else {
                            unplayedColor
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, yTop),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Telemetry Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isPlaying) "PLAYING // AUDIO MEMO" else "PAUSED // AUDIO MEMO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isPlaying) NothingThemeColors.accentCrimson else NothingThemeColors.textMuted,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = timeTelemetry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NothingThemeColors.textSecondary,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}
