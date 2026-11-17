package com.example.nomemo.ui.compose

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.R
import com.example.nomemo.data.pref.PreferencesManager

@Composable
fun NothingSettingsScreen(
    prefsManager: PreferencesManager,
    onBackClick: () -> Unit,
    onClearAllNotes: () -> Unit,
    onExportNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isPitchBlack by remember { mutableStateOf(prefsManager.isPitchBlackEnabled) }
    var isGlassmorphism by remember { mutableStateOf(prefsManager.isGlassmorphismEnabled) }
    var isHaptics by remember { mutableStateOf(prefsManager.isHapticsEnabled) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    NothingAppTheme(isDarkTheme = isPitchBlack) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NothingThemeColors.background)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBackClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "←",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.textPrimary,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    KineticDotMatrixText(
                        text = "SETTINGS // PREFERENCES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.textPrimary,
                        letterSpacing = 1.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Section: Appearance
                    Text(
                        text = "APPEARANCE & THEME",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.accentCrimson,
                        letterSpacing = 1.sp,
                    )

                    FrostedBentoCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Dark / Light Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Pitch Black (OLED)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textPrimary,
                                    )
                                    Text(
                                        text = if (isPitchBlack) "0% Battery drain pure black" else "Nothing Light Gray mode",
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textSecondary,
                                    )
                                }
                                Switch(
                                    checked = isPitchBlack,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isPitchBlack = it
                                        prefsManager.isPitchBlackEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NothingThemeColors.accentCrimson,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = NothingThemeColors.tabTrackBackground,
                                    ),
                                )
                            }

                            // Frosted Glass
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Frosted Glass Refraction",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textPrimary,
                                    )
                                    Text(
                                        text = "3-layer hardware blur & specular borders",
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textSecondary,
                                    )
                                }
                                Switch(
                                    checked = isGlassmorphism,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isGlassmorphism = it
                                        prefsManager.isGlassmorphismEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NothingThemeColors.accentCrimson,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = NothingThemeColors.tabTrackBackground,
                                    ),
                                )
                            }
                        }
                    }

                    // Section: Haptics & Feedback
                    Text(
                        text = "TACTILE HAPTICS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.accentCrimson,
                        letterSpacing = 1.sp,
                    )

                    FrostedBentoCard(cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Mechanical Feedback",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textPrimary,
                                )
                                Text(
                                    text = "Crisp Nothing OS click and glide vibrations",
                                    fontSize = 11.sp,
                                    color = NothingThemeColors.textSecondary,
                                )
                            }
                            Switch(
                                checked = isHaptics,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isHaptics = it
                                    prefsManager.isHapticsEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NothingThemeColors.accentCrimson,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = NothingThemeColors.tabTrackBackground,
                                ),
                            )
                        }
                    }

                    // Section: Data & Backup
                    Text(
                        text = "STUDIO DATA & ARCHIVES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.accentCrimson,
                        letterSpacing = 1.sp,
                    )

                    FrostedBentoCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Export Notes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onExportNotes()
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Export Workspace",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textPrimary,
                                    )
                                    Text(
                                        text = "Package all notes into Markdown / JSON",
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textSecondary,
                                    )
                                }
                                Text("EXPORT →", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingThemeColors.accentCrimson)
                            }

                            // Clear All Data
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showConfirmDeleteDialog = true
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Purge All Notes",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.accentCrimson,
                                    )
                                    Text(
                                        text = "Permanently wipe Room SQLite database",
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textMuted,
                                    )
                                }
                                Text("PURGE ✕", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingThemeColors.accentCrimson)
                            }
                        }
                    }

                    // Architecture Telemetry
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO.MEMO // NOTHING OS 5.0 STUDIO",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NothingThemeColors.textMuted,
                                letterSpacing = 1.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "120 FPS PURE COMPOSE • SKIA GPU • ZERO-FRICTION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NothingThemeColors.accentCrimson,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
            }

            // Confirm Dialog
            if (showConfirmDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDeleteDialog = false },
                    title = {
                        Text(
                            text = "PURGE ALL DATA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.accentCrimson,
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to permanently erase all notes, checklists, and voice memos?",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NothingThemeColors.textSecondary,
                        )
                    },
                    confirmButton = {
                        Text(
                            text = "PURGE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.accentCrimson,
                            modifier = Modifier
                                .clickable {
                                    onClearAllNotes()
                                    showConfirmDeleteDialog = false
                                }
                                .padding(12.dp),
                        )
                    },
                    dismissButton = {
                        Text(
                            text = "CANCEL",
                            fontFamily = FontFamily.Monospace,
                            color = NothingThemeColors.textPrimary,
                            modifier = Modifier
                                .clickable { showConfirmDeleteDialog = false }
                                .padding(12.dp),
                        )
                    },
                    containerColor = NothingThemeColors.surface,
                )
            }
        }
    }
}
