package com.example.nomemo.ui.compose

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.voice.ParsedVoiceNote
import com.example.nomemo.voice.VoiceParser

/**
 * Minimalist Nothing OS 5.0 Live Voice Transcription Overlay Card with Intelligent Title Parsing.
 *
 * Automatically parses spoken speech into Title & Body fields live on screen,
 * displaying them in crisp Nothing Ndot typography over a 40dp hardware blur backdrop.
 */
@Composable
fun NothingVoiceOverlay(
    isListening: Boolean,
    spokenText: String,
    rmsVolume: Float,
    onSaveClick: (ParsedVoiceNote) -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = isListening || spokenText.isNotBlank()
    val isDark = LocalIsDarkTheme.current
    val parsedNote = remember(spokenText) { VoiceParser.parse(spokenText) }

    val backdropDimColor = if (isDark) Color(0xD9000000) else Color(0xD9EBEBEF)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { /* Block touch passes to background */ },
            contentAlignment = Alignment.Center,
        ) {
            // LAYER 1: Deep 40dp Hardware Blur Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(40.dp)
                        } else {
                            Modifier
                        },
                    )
                    .background(backdropDimColor),
            )

            // LAYER 2: Centered Razor-Sharp Transcription Bento Card
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                FrostedBentoCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Top Status Bar & Live Waveform
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 6.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isListening) "REC // LISTENING..." else "TRANSCRIPTION",
                                color = if (isListening) Color(0xFFD71921) else NothingThemeColors.textSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                            )

                            NothingAudioWaveform(
                                isRecording = isListening,
                                barCount = 6,
                                maxHeight = 16.dp,
                            )
                        }

                        // Live Parsed Title Field
                        if (parsedNote.title.isNotBlank()) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    text = "TITLE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.accentCrimson,
                                    letterSpacing = 1.sp,
                                )
                                Text(
                                    text = parsedNote.title.uppercase(),
                                    color = NothingThemeColors.textPrimary,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp,
                                )
                            }
                        }

                        // Live Parsed Body Field
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            if (parsedNote.title.isNotBlank() && parsedNote.body.isNotBlank()) {
                                Text(
                                    text = "BODY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textMuted,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Text(
                                text = if (spokenText.isEmpty()) "Speak now to record note..." else (parsedNote.body.ifBlank { spokenText }),
                                color = if (spokenText.isEmpty()) NothingThemeColors.textMuted else NothingThemeColors.textSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp,
                            )
                        }

                        // Action Buttons
                        if (spokenText.isNotBlank() || isListening) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onDismissClick() }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = "DISMISS",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textMuted,
                                    )
                                }

                                if (spokenText.isNotBlank()) {
                                    Spacer(modifier = Modifier.padding(start = 8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFD71921))
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable { onSaveClick(parsedNote) }
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = "SAVE NOTE",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}