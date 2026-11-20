package com.example.nomemo.ui.compose

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType

/**
 * Frictionless "Flash Note" Quick-Draw Scratchpad for Nothing OS 5.0.
 *
 * Summons an instant specular frosted card directly over the dashboard with 0ms launch delay,
 * auto-detects tags (#tag), and persists directly into Room database on save.
 * Uses a dedicated Dialog with statusBarsPadding() and imePadding() to prevent system insets overlap.
 */
@Composable
fun FlashNoteOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSaveNote: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isChecklistMode by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
        }
    }

    val extractedTags = remember(body, title) {
        val regex = "#([a-zA-Z0-9_]+)".toRegex()
        val combined = "$title $body"
        regex.findAll(combined).map { it.groupValues[1].lowercase() }.distinct().toList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable { onDismiss() }
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            FrostedBentoCard(
                cornerRadius = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}, // Block clicks from dismissing
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    // Top Telemetry Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KineticDotMatrixText(
                            text = "FLASH // QUICK NOTE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.accentCrimson,
                            letterSpacing = 1.sp,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Checklist Mode Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecklistMode) NothingThemeColors.surface else NothingThemeColors.tabTrackBackground)
                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isChecklistMode = !isChecklistMode
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (isChecklistMode) "CHECKLIST" else "NOTE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecklistMode) Color.White else NothingThemeColors.textSecondary,
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Close Dismiss Button
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = NothingThemeColors.iconInactive,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDismiss() },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Input
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.textPrimary,
                        ),
                        cursorBrush = SolidColor(NothingThemeColors.iconActive),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "TITLE...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textMuted,
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Body Input
                    BasicTextField(
                        value = body,
                        onValueChange = { body = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = NothingThemeColors.textSecondary,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(NothingThemeColors.iconActive),
                        minLines = 4,
                        maxLines = 8,
                        decorationBox = { innerTextField ->
                            if (body.isEmpty()) {
                                Text(
                                    text = if (isChecklistMode) "Type items separated by new lines...\ne.g.\nMilk\nBread\n#groceries" else "Write your thoughts... tags like #idea or #urgent work automatically.",
                                    fontSize = 13.sp,
                                    color = NothingThemeColors.textMuted,
                                    lineHeight = 18.sp,
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detected Tags Preview
                    if (extractedTags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            extractedTags.forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NothingThemeColors.textSecondary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Bottom Bar: Clean White CAPTURE Pill aligned to bottom-right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Save Capsule
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(NothingThemeColors.iconActive)
                                .clickable {
                                    if (title.isNotBlank() || body.isNotBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val finalNote = if (isChecklistMode) {
                                            val items = body.lines()
                                                .filter { it.isNotBlank() }
                                                .map { com.example.nomemo.data.model.ChecklistItem(text = it.trim()) }
                                            Note(
                                                title = title.ifBlank { "CHECKLIST" },
                                                body = body,
                                                type = NoteType.CHECKLIST,
                                                checklistItems = items,
                                                tags = extractedTags,
                                                timestamp = System.currentTimeMillis(),
                                            )
                                        } else {
                                            Note(
                                                title = title.ifBlank { "NOTE" },
                                                body = body,
                                                type = NoteType.NOTE,
                                                tags = extractedTags,
                                                timestamp = System.currentTimeMillis(),
                                            )
                                        }
                                        onSaveNote(finalNote)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "CAPTURE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
