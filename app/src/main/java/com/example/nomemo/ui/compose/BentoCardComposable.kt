package com.example.nomemo.ui.compose

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nomemo.R
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BentoCardComposable(
    note: Note,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onChecklistToggle: ((itemId: String) -> Unit)? = null,
    onTagClick: ((tag: String) -> Unit)? = null,
    onLockToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.US) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)).uppercase() }
    val wordCount = remember(note.body) {
        if (note.body.isBlank()) 0 else note.body.trim().split("\\s+".toRegex()).size
    }
    val firstImageUri = remember(note.imageUris) { note.imageUris.firstOrNull() }
    var isUnlockedLocally by remember(note.id) { mutableStateOf(!note.isLocked) }

    FrostedBentoCard(
        isSelected = isSelected,
        modifier = modifier
            .fillMaxWidth()
            .nothingBentoPress(
                onClick = {
                    if (note.isLocked && !isUnlockedLocally) {
                        isUnlockedLocally = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick,
            ),
    ) {
        Column {
            // Optional Media Thumbnail
            if (!firstImageUri.isNullOrBlank() && (!note.isLocked || isUnlockedLocally)) {
                AsyncImage(
                    model = Uri.parse(firstImageUri),
                    contentDescription = "Note Media Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Header: Title, Pin, Lock indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KineticDotMatrixText(
                        text = if (note.isLocked && !isUnlockedLocally) "LOCKED NOTE" else note.title.ifBlank { "UNTITLED" }.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.textPrimary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NothingThemeColors.accentCrimson else Color(0x33FFFFFF))
                                    .border(1.2.dp, if (isSelected) Color.White else Color(0x66FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (note.reminderTimestamp != null && note.reminderTimestamp > System.currentTimeMillis()) {
                                Text("⏰", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (note.isLocked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isUnlockedLocally) "🔓" else "🔒",
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable {
                                        com.example.nomemo.ui.anim.NothingAudioFeedback.playLockBolt()
                                        onLockToggle?.invoke()
                                    },
                                )
                            }
                            if (note.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pin),
                                    contentDescription = "Pinned",
                                    tint = NothingThemeColors.accentCrimson,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }

                // Locked Privacy Mask
                if (note.isLocked && !isUnlockedLocally) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingThemeColors.tabTrackBackground)
                            .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(10.dp))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "░░ GLYPH LOCKED // TAP TO REVEAL ░░",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NothingThemeColors.accentCrimson,
                            letterSpacing = 1.sp,
                        )
                    }
                } else {
                    // Body text preview
                    if (note.body.isNotBlank() && note.type != NoteType.CHECKLIST) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.body,
                            fontSize = 12.sp,
                            color = NothingThemeColors.textSecondary,
                            maxLines = 3,
                            lineHeight = 16.sp,
                        )
                    }

                    // Interactive Checklist items (direct in-card completion!)
                    if (note.type == NoteType.CHECKLIST && note.checklistItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            note.checklistItems.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            com.example.nomemo.ui.anim.NothingAudioFeedback.playCheckChime()
                                            onChecklistToggle?.invoke(item.id)
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (item.isChecked) NothingThemeColors.accentCrimson
                                                else Color.Transparent,
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (item.isChecked) NothingThemeColors.accentCrimson
                                                else NothingThemeColors.textSecondary,
                                                shape = RoundedCornerShape(3.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (item.isChecked) {
                                            Text(
                                                text = "✓",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = item.text,
                                        fontSize = 12.sp,
                                        color = if (item.isChecked) NothingThemeColors.textMuted else NothingThemeColors.textSecondary,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (note.checklistItems.size > 4) {
                                Text(
                                    text = "+${note.checklistItems.size - 4} MORE ITEMS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.accentCrimson,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }

                    // Audio memo waveform playback
                    if (!note.audioUri.isNullOrBlank() || note.type == NoteType.AUDIO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DotMatrixAudioPlayer(
                            audioUri = note.audioUri.orEmpty(),
                            durationMs = note.audioDurationMs,
                            waveform = note.audioWaveform,
                        )
                    }

                    // Hashtag Pills
                    if (note.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            note.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingThemeColors.tabTrackBackground)
                                        .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(10.dp))
                                        .clickable { onTagClick?.invoke(tag) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "#$tag",
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

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val footerText = when (note.type) {
                        NoteType.CHECKLIST -> "$formattedDate • ${note.checklistItems.size} ITEMS"
                        NoteType.AUDIO -> "$formattedDate • AUDIO MEMO"
                        else -> "$formattedDate • $wordCount WORDS"
                    }
                    Text(
                        text = footerText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NothingThemeColors.textMuted,
                        letterSpacing = 0.5.sp,
                    )

                    val typeIconRes = when (note.type) {
                        NoteType.CHECKLIST -> R.drawable.ic_checklist
                        NoteType.AUDIO -> R.drawable.ic_dock_voice
                        NoteType.MEDIA -> R.drawable.ic_dock_image
                        else -> R.drawable.ic_dock_text
                    }
                    Icon(
                        painter = painterResource(id = typeIconRes),
                        contentDescription = "Note Type",
                        tint = NothingThemeColors.textMuted,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}
