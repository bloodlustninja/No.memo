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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.ui.anim.NothingAudioFeedback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Nothing OS 5.0 Minimalist Scheduled Reminder Picker.
 *
 * Fast 1-tap presets and custom scheduling for native Android notifications.
 */
@Composable
fun NothingReminderDialog(
    isVisible: Boolean,
    currentReminder: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM // HH:mm", Locale.US) }

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
            // Backdrop
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
                    .background(Color(0xD9000000))
                    .clickable { onDismiss() },
            )

            // Centered Bento Card
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                FrostedBentoCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
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
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD71921)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REMINDER // SCHEDULE",
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
                                        NothingAudioFeedback.playTactileClick()
                                        onDismiss()
                                    }
                                    .padding(4.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (currentReminder != null && currentReminder > System.currentTimeMillis()) {
                            Text(
                                text = "ACTIVE: ${dateFormat.format(Date(currentReminder)).uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.accentCrimson,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Presets
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Preset 1: In 1 Hour
                            ReminderOptionRow(
                                label = "IN 1 HOUR",
                                subLabel = "Quick reminder",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playCheckChime()
                                    onSetReminder(System.currentTimeMillis() + (60 * 60 * 1000L))
                                    onDismiss()
                                },
                            )

                            // Preset 2: Tonight 20:00
                            ReminderOptionRow(
                                label = "TONIGHT // 20:00",
                                subLabel = "Evening review",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playCheckChime()
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 20)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        if (timeInMillis <= System.currentTimeMillis()) {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                    }
                                    onSetReminder(cal.timeInMillis)
                                    onDismiss()
                                },
                            )

                            // Preset 3: Tomorrow Morning 09:00
                            ReminderOptionRow(
                                label = "TOMORROW // 09:00",
                                subLabel = "Morning briefing",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playCheckChime()
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    onSetReminder(cal.timeInMillis)
                                    onDismiss()
                                },
                            )

                            // Preset 4: In 2 Days
                            ReminderOptionRow(
                                label = "IN 2 DAYS",
                                subLabel = "Follow-up",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playCheckChime()
                                    onSetReminder(System.currentTimeMillis() + (48 * 60 * 60 * 1000L))
                                    onDismiss()
                                },
                            )
                        }

                        // Clear reminder button
                        if (currentReminder != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x33D71921))
                                    .border(1.dp, Color(0x66D71921), RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        NothingAudioFeedback.playTactileClick()
                                        onClearReminder()
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "REMOVE ACTIVE REMINDER",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF4444),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderOptionRow(label: String, subLabel: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NothingThemeColors.tabTrackBackground)
            .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingThemeColors.textPrimary,
                )
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = NothingThemeColors.textMuted,
                )
            }
            Text(
                text = "SET →",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingThemeColors.accentCrimson,
            )
        }
    }
}
