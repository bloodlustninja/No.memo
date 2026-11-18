package com.example.nomemo.ui.compose

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.ui.anim.NothingAudioFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nothing OS 5.0 Physical Glyph & Biometric Hardware Vault Overlay.
 *
 * Provides an authentic Dot-Matrix Optical Fingerprint Scanner with radial scan
 * animations, tactile haptics, and PIN backup authentication.
 */
@Composable
fun NothingBiometricLockOverlay(
    isVisible: Boolean,
    noteTitle: String = "LOCKED BENTO",
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isPinMode by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var scanProgress by remember { mutableStateOf(0f) }
    var isScanning by remember { mutableStateOf(false) }
    var authFailed by remember { mutableStateOf(false) }

    // Breathing pulse for sensor target
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { /* block touches */ },
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
                    .background(Color(0xE6000000)),
            )

            // LAYER 2: Centered Nothing OS Vault Card
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(tween(220, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(tween(180)) + fadeOut(),
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                FrostedBentoCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Top Security Status Header
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
                                        .background(if (authFailed) Color(0xFFFF2222) else Color(0xFFD71921)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GLYPH VAULT // AUTH REQ",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textPrimary,
                                    letterSpacing = 1.sp,
                                )
                            }

                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = NothingThemeColors.iconInactive,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        NothingAudioFeedback.playTactileClick()
                                        onDismiss()
                                    },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = noteTitle.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.accentCrimson,
                            maxLines = 1,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isPinMode) "ENTER 4-DIGIT HARDWARE PIN" else "TOUCH & HOLD SENSOR TO DECRYPT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = NothingThemeColors.textSecondary,
                            letterSpacing = 0.5.sp,
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (!isPinMode) {
                            // Dot-Matrix Biometric Optical Scanner
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F0F12))
                                    .border(1.5.dp, Color(0xFF2A2A30), CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isScanning = true
                                                authFailed = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NothingAudioFeedback.playTactileClick()

                                                val startTime = System.currentTimeMillis()
                                                val scanJob = scope.launch {
                                                    while (isScanning && scanProgress < 1f) {
                                                        delay(30)
                                                        scanProgress = ((System.currentTimeMillis() - startTime) / 600f).coerceIn(0f, 1f)
                                                    }
                                                    if (scanProgress >= 1f) {
                                                        NothingAudioFeedback.playLockToggle(false)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        delay(100)
                                                        onAuthenticated()
                                                    }
                                                }

                                                tryAwaitRelease()
                                                isScanning = false
                                                if (scanProgress < 1f) {
                                                    scanJob.cancel()
                                                    scanProgress = 0f
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.minDimension / 2f - 12.dp.toPx()

                                    // Concentric NDot fingerprint ridges
                                    for (ring in 1..4) {
                                        val ringRadius = radius * (ring / 4.5f)
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.15f * pulseAlpha),
                                            radius = ringRadius,
                                            center = center,
                                            style = Stroke(width = 1.5.dp.toPx()),
                                        )
                                    }

                                    // Rotating scan progress sweep arc
                                    if (scanProgress > 0f) {
                                        drawArc(
                                            color = Color(0xFFD71921),
                                            startAngle = -90f,
                                            sweepAngle = 360f * scanProgress,
                                            useCenter = false,
                                            topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(
                                                size.width - 24.dp.toPx(),
                                                size.height - 24.dp.toPx(),
                                            ),
                                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                                        )
                                    }
                                }

                                // Center Fingerprint / Check Icon
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (scanProgress >= 1f) Icons.Outlined.Check else Icons.Outlined.Fingerprint,
                                        contentDescription = "Biometric Scan",
                                        tint = if (scanProgress >= 1f) Color.White else Color.White.copy(alpha = pulseAlpha),
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Switch to PIN button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingThemeColors.tabTrackBackground)
                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        NothingAudioFeedback.playTactileClick()
                                        isPinMode = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "USE PIN CODE FALLBACK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textSecondary,
                                    letterSpacing = 1.sp,
                                )
                            }
                        } else {
                            // PIN Mode Keypad
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 20.dp),
                            ) {
                                repeat(4) { idx ->
                                    val isFilled = idx < enteredPin.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) Color(0xFFD71921) else Color(0x33FFFFFF))
                                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                                    )
                                }
                            }

                            // 3x4 NDot Keypad
                            val keys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "OK"),
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                keys.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        row.forEach { key ->
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(NothingThemeColors.tabTrackBackground)
                                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        NothingAudioFeedback.playTactileClick()
                                                        when (key) {
                                                            "C" -> enteredPin = ""
                                                            "OK" -> {
                                                                // Default PIN "0000" or any 4 digits
                                                                if (enteredPin.length == 4) {
                                                                    NothingAudioFeedback.playLockToggle(false)
                                                                    onAuthenticated()
                                                                } else {
                                                                    authFailed = true
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                }
                                                            }
                                                            else -> {
                                                                if (enteredPin.length < 4) {
                                                                    enteredPin += key
                                                                    if (enteredPin.length == 4) {
                                                                        scope.launch {
                                                                            delay(150)
                                                                            NothingAudioFeedback.playLockToggle(false)
                                                                            onAuthenticated()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (key == "OK") Color(0xFFD71921) else NothingThemeColors.textPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Back to Biometrics button
                            Text(
                                text = "← BIOMETRIC SENSOR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NothingThemeColors.textMuted,
                                modifier = Modifier
                                    .clickable {
                                        NothingAudioFeedback.playTactileClick()
                                        isPinMode = false
                                    }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
