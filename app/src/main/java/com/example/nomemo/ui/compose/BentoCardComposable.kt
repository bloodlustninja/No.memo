package com.example.nomemo.ui.compose

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.ledger.ExpenseParser
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
    val ledgerAnalysis = remember(note.body, note.title, note.checklistItems) {
        val fullText = buildString {
            append(note.title)
            append("\n")
            append(note.body)
            if (note.checklistItems.isNotEmpty()) {
                append("\n")
                append(note.checklistItems.joinToString("\n") { it.text })
            }
        }
        ExpenseParser.parse(fullText)
    }

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
                        // Burn Countdown Badge (e.g., "23H") with pulsing red dot indicator
                        if (note.isBurn) {
                            val remainingMs = (note.burnExpiresAt ?: (note.timestamp + 24 * 3600 * 1000L)) - System.currentTimeMillis()
                            val hours = maxOf(0L, remainingMs / (1000 * 3600))
                            val countdownText = if (hours > 0) "${hours}H" else "${maxOf(1L, remainingMs / (1000 * 60))}M"

                            val burnTransition = rememberInfiniteTransition(label = "burnDotPulse")
                            val burnDotAlpha by burnTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "burnDotAlpha",
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NothingThemeColors.accentCrimson.copy(alpha = 0.15f))
                                    .border(1.dp, NothingThemeColors.accentCrimson, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .graphicsLayer { alpha = burnDotAlpha }
                                        .clip(CircleShape)
                                        .background(NothingThemeColors.accentCrimson),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = countdownText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.accentCrimson,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NothingThemeColors.iconActive else Color.Transparent)
                                    .border(1.dp, if (isSelected) NothingThemeColors.iconActive else NothingThemeColors.surfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        } else {
                            if (note.reminderTimestamp != null && note.reminderTimestamp > System.currentTimeMillis()) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = "Reminder",
                                    tint = NothingThemeColors.iconInactive,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (note.isLocked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isUnlockedLocally) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                    contentDescription = "Lock",
                                    tint = NothingThemeColors.iconInactive,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            com.example.nomemo.ui.anim.NothingAudioFeedback.playLockBolt()
                                            onLockToggle?.invoke()
                                        },
                                )
                            }
                            if (note.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = "Pinned",
                                    tint = NothingThemeColors.iconActive,
                                    modifier = Modifier.size(14.dp),
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
                            color = NothingThemeColors.textSecondary,
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

                    // Interactive Checklist items (direct in-card completion with smooth strikethrough animation)
                    if (note.type == NoteType.CHECKLIST && note.checklistItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            note.checklistItems.take(4).forEach { item ->
                                BentoChecklistItemRow(
                                    item = item,
                                    onToggle = { onChecklistToggle?.invoke(item.id) },
                                )
                            }
                            if (note.checklistItems.size > 4) {
                                Text(
                                    text = "+${note.checklistItems.size - 4} MORE ITEMS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.iconInactive,
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
                                        .tactilePress { onTagClick?.invoke(tag) }
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
                    val footerText = when {
                        ledgerAnalysis.isLedgerMode -> "$formattedDate • ${ledgerAnalysis.entries.size} EXP"
                        note.type == NoteType.CHECKLIST -> "$formattedDate • ${note.checklistItems.size} ITEMS"
                        note.type == NoteType.AUDIO -> "$formattedDate • AUDIO MEMO"
                        else -> "$formattedDate • $wordCount WORDS"
                    }
                    Text(
                        text = footerText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NothingThemeColors.textMuted,
                        letterSpacing = 0.5.sp,
                    )

                    if (ledgerAnalysis.isLedgerMode) {
                        RollingOdometerBadge(
                            total = ledgerAnalysis.totalExpense,
                            currencySymbol = ledgerAnalysis.currencySymbol,
                        )
                    } else {
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

            // Razor-thin 2.dp horizontal progress bar along the bottom edge for spending limit /cap
            if (ledgerAnalysis.isLedgerMode && ledgerAnalysis.capAmount != null && ledgerAnalysis.capAmount > 0) {
                val targetFraction = ledgerAnalysis.capProgressFraction ?: 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "ledgerCapProgressBar",
                )
                val barColor = if (ledgerAnalysis.isCapExceededOrNearLimit) {
                    NothingThemeColors.accentCrimson
                } else {
                    Color(0xFFFFFFFF)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(NothingThemeColors.surfaceBorder),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .height(2.dp)
                            .background(barColor),
                    )
                }
            }
        }
    }
}

/**
 * Clean dot-matrix/monospace total badge (e.g., "TOTAL ₹1,450") using rolling odometer digit animation.
 */
@Composable
fun RollingOdometerBadge(
    total: Double,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier,
) {
    val isNegative = total < 0
    val formatted = remember(total) {
        ExpenseParser.formatAmountWithCommas(kotlin.math.abs(total))
    }
    val prefix = if (isNegative) "TOTAL -$currencySymbol" else "TOTAL $currencySymbol"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NothingThemeColors.tabTrackBackground)
            .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prefix,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.width(2.dp))
        formatted.forEachIndexed { idx, char ->
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        if (targetState >= initialState) {
                            (slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> -height } + fadeIn())
                                .togetherWith(slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { height -> height } + fadeOut())
                        }
                    },
                    label = "odometer_digit_$idx",
                ) { digitChar ->
                    Text(
                        text = digitChar.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNegative) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNegative) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun BentoChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        targetValue = if (item.isChecked) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "strikethroughProgress",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (item.isChecked) 0.45f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "textAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .tactilePress {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                com.example.nomemo.ui.anim.NothingAudioFeedback.playCheckChime()
                onToggle()
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    if (item.isChecked) NothingThemeColors.iconActive
                    else Color.Transparent,
                )
                .border(
                    width = 1.dp,
                    color = if (item.isChecked) NothingThemeColors.iconActive
                    else NothingThemeColors.iconInactive,
                    shape = RoundedCornerShape(3.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        val resolvedColor = NothingThemeColors.textSecondary.copy(alpha = textAlpha)
        Text(
            text = item.text,
            fontSize = 12.sp,
            color = resolvedColor,
            maxLines = 1,
            modifier = Modifier.drawWithContent {
                drawContent()
                if (progress > 0.001f) {
                    val strokeW = 1.2.dp.toPx()
                    val y = size.height / 2f
                    drawLine(
                        color = resolvedColor,
                        start = Offset(0f, y),
                        end = Offset(size.width * progress, y),
                        strokeWidth = strokeW,
                    )
                }
            },
        )
    }
}
