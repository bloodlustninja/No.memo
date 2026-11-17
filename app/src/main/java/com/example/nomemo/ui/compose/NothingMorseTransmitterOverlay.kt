package com.example.nomemo.ui.compose

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.ui.anim.NothingAudioFeedback
import com.example.nomemo.voice.GlyphCipherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nothing OS 5.0 Glyph Morse Telegraph & Optical Transmitter Studio.
 *
 * Encodes text into Morse signals, pulses the rear Nothing Glyph LED strips,
 * and synthesizes 880Hz telegraph audio tones. Also includes an interactive
 * mechanical telegraph keyer for manual Morse transmission.
 */
@Composable
fun NothingMorseTransmitterOverlay(
    isVisible: Boolean,
    initialText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var textToTransmit by remember(initialText) { mutableStateOf(initialText.ifBlank { "NOTHING" }) }
    val morseCode = remember(textToTransmit) { GlyphCipherManager.textToMorse(textToTransmit) }

    var isTransmitting by remember { mutableStateOf(false) }
    var currentSymbolIndex by remember { mutableStateOf(-1) }
    var isGlyphLit by remember { mutableStateOf(false) }
    var transmissionJob by remember { mutableStateOf<Job?>(null) }

    // Manual Keyer State
    var manualMorseInput by remember { mutableStateOf("") }
    val decodedManualText = remember(manualMorseInput) { GlyphCipherManager.morseToText(manualMorseInput) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { /* Block touches */ },
            contentAlignment = Alignment.Center,
        ) {
            // LAYER 1: Deep 45dp Hardware Blur Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(45.dp)
                        } else {
                            Modifier
                        },
                    )
                    .background(Color(0xF2000000)),
            )

            // LAYER 2: Centered Bento Studio Card
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                FrostedBentoCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isGlyphLit) Color(0xFF00FF66) else Color(0xFFD71921)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GLYPH CIPHER // TELEGRAPH",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textPrimary,
                                    letterSpacing = 1.sp,
                                )
                            }

                            Text(
                                text = "✕",
                                color = NothingThemeColors.textMuted,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        transmissionJob?.cancel()
                                        isTransmitting = false
                                        isGlyphLit = false
                                        NothingAudioFeedback.playTactileClick()
                                        onDismiss()
                                    }
                                    .padding(4.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Glyph Simulator Unit
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0C0C0E))
                                .border(1.dp, if (isGlyphLit) Color.White else Color(0xFF222228), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            NothingGlyphInterface(
                                volume = if (isGlyphLit) 1.0f else 0.0f,
                                isRecording = isGlyphLit,
                                size = 110.dp,
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Morse Tape Readout Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D0D10))
                                .border(1.dp, Color(0xFF24242A), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                        ) {
                            Column {
                                Text(
                                    text = "MORSE CIPHERSTREAM",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.accentCrimson,
                                    letterSpacing = 1.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = morseCode.ifBlank { "..." },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTransmitting) Color.White else NothingThemeColors.textSecondary,
                                    lineHeight = 22.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Transmission Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Start/Stop Transmit Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isTransmitting) Color(0xFFD71921) else Color(0xFF1E1E24))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isTransmitting) {
                                            transmissionJob?.cancel()
                                            isTransmitting = false
                                            isGlyphLit = false
                                        } else {
                                            isTransmitting = true
                                            transmissionJob = scope.launch(Dispatchers.Default) {
                                                val symbols = morseCode.toCharArray()
                                                for (i in symbols.indices) {
                                                    if (!isActive) break
                                                    val ch = symbols[i]
                                                    withContext(Dispatchers.Main) {
                                                        currentSymbolIndex = i
                                                    }
                                                    when (ch) {
                                                        '.' -> {
                                                            withContext(Dispatchers.Main) { isGlyphLit = true }
                                                            GlyphCipherManager.playMorseTone(isDash = false)
                                                            withContext(Dispatchers.Main) { isGlyphLit = false }
                                                            delay(70)
                                                        }
                                                        '-' -> {
                                                            withContext(Dispatchers.Main) { isGlyphLit = true }
                                                            GlyphCipherManager.playMorseTone(isDash = true)
                                                            withContext(Dispatchers.Main) { isGlyphLit = false }
                                                            delay(70)
                                                        }
                                                        ' ' -> delay(160)
                                                        '/' -> delay(350)
                                                    }
                                                }
                                                withContext(Dispatchers.Main) {
                                                    isTransmitting = false
                                                    isGlyphLit = false
                                                    currentSymbolIndex = -1
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isTransmitting) "HALT TRANSMISSION" else "TRANSMIT GLYPH MORSE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            // Copy Morse Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingThemeColors.tabTrackBackground)
                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        NothingAudioFeedback.playTactileClick()
                                        clipboardManager.setText(AnnotatedString(morseCode))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "COPY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textPrimary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Section 2: Manual Telegraph Keyer
                        Text(
                            text = "MANUAL TELEGRAPH KEYER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.textMuted,
                            letterSpacing = 1.sp,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Decoded Output
                        Text(
                            text = if (decodedManualText.isBlank()) "DECODED: [TAP KEYS BELOW]" else "DECODED: $decodedManualText",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.textPrimary,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mechanical DOT and DASH Keys
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // DOT KEY
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .border(1.5.dp, Color(0xFF2C2C34), RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        manualMorseInput += "."
                                        scope.launch(Dispatchers.Default) {
                                            isGlyphLit = true
                                            GlyphCipherManager.playMorseTone(isDash = false)
                                            isGlyphLit = false
                                        }
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "•  DOT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            // DASH KEY
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .border(1.5.dp, Color(0xFF2C2C34), RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        manualMorseInput += "-"
                                        scope.launch(Dispatchers.Default) {
                                            isGlyphLit = true
                                            GlyphCipherManager.playMorseTone(isDash = true)
                                            isGlyphLit = false
                                        }
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "—  DASH",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            // SPACE / CLEAR
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(14.dp))
                                    .clickable {
                                        manualMorseInput += " "
                                        NothingAudioFeedback.playTactileClick()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "SPACE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NothingThemeColors.textSecondary,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(14.dp))
                                    .clickable {
                                        manualMorseInput = ""
                                        NothingAudioFeedback.playTactileClick()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "CLR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NothingThemeColors.accentCrimson,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
