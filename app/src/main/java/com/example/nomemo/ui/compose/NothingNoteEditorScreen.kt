package com.example.nomemo.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nomemo.R
import com.example.nomemo.data.SmartNoteIntelligence
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.data.model.NoteRevision
import com.example.nomemo.export.NothingReceiptExporter
import com.example.nomemo.reminder.NothingReminderManager
import com.example.nomemo.voice.AudioRecorderManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NothingNoteEditorScreen(
    initialNote: Note,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialNote.title) }
    var body by remember { mutableStateOf(initialNote.body) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }
    var isLocked by remember { mutableStateOf(initialNote.isLocked) }
    var noteType by remember { mutableStateOf(initialNote.type) }
    val imageUris = remember { mutableStateListOf<String>().apply { addAll(initialNote.imageUris) } }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>().apply { addAll(initialNote.checklistItems) } }

    var audioUri by remember { mutableStateOf(initialNote.audioUri) }
    var audioDurationMs by remember { mutableStateOf(initialNote.audioDurationMs) }
    var audioWaveform by remember { mutableStateOf(initialNote.audioWaveform) }
    var showDoodleSheet by remember { mutableStateOf(false) }
    var isMarkdownPreviewMode by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderTimestamp by remember { mutableStateOf(initialNote.reminderTimestamp) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val revisions = remember { mutableStateListOf<NoteRevision>().apply { addAll(initialNote.revisions) } }

    // Audio Recorder
    val audioRecorder = remember { AudioRecorderManager(context) }
    val isRecordingAudio by audioRecorder.isRecording.collectAsState()
    val recordElapsedTime by audioRecorder.elapsedTimeMs.collectAsState()
    val liveAmplitudeWaveform by audioRecorder.amplitudeWaveform.collectAsState()

    // Undo / Redo Stacks
    val undoStack = remember { ArrayDeque<Pair<String, String>>() }
    val redoStack = remember { ArrayDeque<Pair<String, String>>() }
    var isUndoRedoAction by remember { mutableStateOf(false) }

    // Typing speed calculation
    var lastTypedTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var typedCharCountInWindow by remember { mutableStateOf(0) }
    var calculatedWpm by remember { mutableStateOf(0) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            imageUris.add(it.toString())
        }
    }

    // Auto-save debounced effect
    LaunchedEffect(title, body, isPinned, isLocked, noteType, imageUris.size, checklistItems.toList(), audioUri) {
        delay(350L)
        val extractedTags = run {
            val regex = "#([a-zA-Z0-9_]+)".toRegex()
            val combined = "$title $body"
            regex.findAll(combined).map { it.groupValues[1].lowercase() }.distinct().toList()
        }

        val updatedNote = initialNote.copy(
            title = title,
            body = body,
            isPinned = isPinned,
            isLocked = isLocked,
            type = if (checklistItems.isNotEmpty() && noteType != NoteType.AUDIO) NoteType.CHECKLIST else noteType,
            imageUris = imageUris.toList(),
            checklistItems = checklistItems.toList(),
            audioUri = audioUri,
            audioDurationMs = audioDurationMs,
            audioWaveform = audioWaveform,
            tags = extractedTags,
            revisions = revisions.toList(),
            reminderTimestamp = reminderTimestamp,
            timestamp = System.currentTimeMillis(),
        )
        onSaveNote(updatedNote)
    }

    // WPM telemetry tracker
    LaunchedEffect(body) {
        if (!isUndoRedoAction) {
            typedCharCountInWindow++
            val now = System.currentTimeMillis()
            val timeDiffSec = (now - lastTypedTime) / 1000f
            if (timeDiffSec >= 2f) {
                val wordsInWindow = typedCharCountInWindow / 5f
                val minutes = timeDiffSec / 60f
                calculatedWpm = (wordsInWindow / minutes).toInt().coerceIn(0, 180)
                lastTypedTime = now
                typedCharCountInWindow = 0
            }
        }
    }

    val wordCount = remember(body, title) {
        val full = "$title $body".trim()
        if (full.isBlank()) 0 else full.split("\\s+".toRegex()).size
    }
    val charCount = remember(body, title) { "$title $body".trim().length }
    val readTimeMin = remember(wordCount) { (wordCount / 200).coerceAtLeast(1) }

    NothingAppTheme(isDarkTheme = isDarkTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NothingThemeColors.background)
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Back Button
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Undo
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (undoStack.size > 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val cur = undoStack.removeLast()
                                        redoStack.addLast(cur)
                                        val prev = undoStack.last()
                                        isUndoRedoAction = true
                                        title = prev.first
                                        body = prev.second
                                        isUndoRedoAction = false
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_undo),
                                contentDescription = "Undo",
                                tint = if (undoStack.size > 1) NothingThemeColors.textPrimary else NothingThemeColors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        // Redo
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (redoStack.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val nxt = redoStack.removeLast()
                                        undoStack.addLast(nxt)
                                        isUndoRedoAction = true
                                        title = nxt.first
                                        body = nxt.second
                                        isUndoRedoAction = false
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_redo),
                                contentDescription = "Redo",
                                tint = if (redoStack.isNotEmpty()) NothingThemeColors.textPrimary else NothingThemeColors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Preview / Edit Markdown Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMarkdownPreviewMode) NothingThemeColors.accentCrimson else NothingThemeColors.tabTrackBackground)
                                .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    isMarkdownPreviewMode = !isMarkdownPreviewMode
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (isMarkdownPreviewMode) "PREVIEW" else "EDIT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Receipt Share Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingThemeColors.tabTrackBackground)
                                .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    val currentNote = initialNote.copy(
                                        title = title,
                                        body = body,
                                        checklistItems = checklistItems.toList(),
                                        tags = run {
                                            val regex = "#([a-zA-Z0-9_]+)".toRegex()
                                            regex.findAll("$title $body").map { it.groupValues[1].lowercase() }.distinct().toList()
                                        },
                                    )
                                    NothingReceiptExporter.shareReceipt(context, currentNote)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "RECEIPT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.accentCrimson,
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Reminder Schedule Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (reminderTimestamp != null) NothingThemeColors.accentCrimson.copy(alpha = 0.25f) else NothingThemeColors.tabTrackBackground)
                                .border(1.dp, if (reminderTimestamp != null) NothingThemeColors.accentCrimson else NothingThemeColors.tabTrackBorder, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showReminderDialog = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⏰",
                                fontSize = 12.sp,
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Time Machine History
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NothingThemeColors.tabTrackBackground)
                                .border(1.dp, NothingThemeColors.tabTrackBorder, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showHistorySheet = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⌛",
                                fontSize = 12.sp,
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Glyph Lock Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isLocked = !isLocked
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isLocked) "🔒" else "🔓",
                                fontSize = 14.sp,
                            )
                        }

                        // Pin Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isPinned = !isPinned
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pin),
                                contentDescription = "Pin Note",
                                tint = if (isPinned) NothingThemeColors.accentCrimson else NothingThemeColors.textSecondary,
                                modifier = Modifier.size(17.dp),
                            )
                        }

                        // Delete Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (initialNote.id != 0L) {
                                        onDeleteNote(initialNote.id)
                                    }
                                    onBackClick()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Delete Note",
                                tint = NothingThemeColors.accentCrimson,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }

                // Scrollable Editor Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Title Input
                    item {
                        BasicTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                if (!isUndoRedoAction) {
                                    undoStack.addLast(Pair(it, body))
                                    redoStack.clear()
                                }
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.textPrimary,
                                letterSpacing = 1.sp,
                            ),
                            cursorBrush = SolidColor(NothingThemeColors.accentCrimson),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = "UNTITLED...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textMuted,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Attached Media Strip
                    if (imageUris.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                imageUris.forEach { uriStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(16.dp)),
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(uriStr),
                                            contentDescription = "Attached Media",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        // Remove Image Button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    imageUris.remove(uriStr)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("✕", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Audio Voice Memo Player
                    if (!audioUri.isNullOrBlank()) {
                        item {
                            DotMatrixAudioPlayer(
                                audioUri = audioUri!!,
                                durationMs = audioDurationMs,
                                waveform = audioWaveform,
                            )
                        }
                    }

                    // Live Audio Recording Card
                    if (isRecordingAudio) {
                        item {
                            FrostedBentoCard(cornerRadius = 16.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(NothingThemeColors.accentCrimson),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val sec = (recordElapsedTime / 1000).toInt()
                                        Text(
                                            text = String.format(Locale.US, "REC // %02d:%02d", sec / 60, sec % 60),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = NothingThemeColors.accentCrimson,
                                        )
                                    }

                                    NothingAudioWaveform(
                                        isRecording = true,
                                        barCount = 8,
                                        maxHeight = 20.dp,
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NothingThemeColors.accentCrimson)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val result = audioRecorder.stopRecording()
                                                if (result != null) {
                                                    audioUri = result.file.absolutePath
                                                    audioDurationMs = result.durationMs
                                                    audioWaveform = result.waveform
                                                    noteType = NoteType.AUDIO
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "DONE",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Checklist Items
                    if (checklistItems.isNotEmpty() || noteType == NoteType.CHECKLIST) {
                        itemsIndexed(checklistItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingThemeColors.tabTrackBackground.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Checkbox Box
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (item.isChecked) NothingThemeColors.accentCrimson else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (item.isChecked) NothingThemeColors.accentCrimson else NothingThemeColors.textSecondary,
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            checklistItems[index] = item.copy(isChecked = !item.isChecked)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (item.isChecked) {
                                        Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Item Text Input
                                BasicTextField(
                                    value = item.text,
                                    onValueChange = { newTxt ->
                                        checklistItems[index] = item.copy(text = newTxt)
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = if (item.isChecked) NothingThemeColors.textMuted else NothingThemeColors.textPrimary,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    ),
                                    cursorBrush = SolidColor(NothingThemeColors.accentCrimson),
                                    modifier = Modifier.weight(1f),
                                )

                                // Delete checklist row
                                Text(
                                    text = "✕",
                                    fontSize = 12.sp,
                                    color = NothingThemeColors.textMuted,
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            checklistItems.removeAt(index)
                                        }
                                        .padding(4.dp),
                                )
                            }
                        }

                        item {
                            // Add Checklist Item Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        checklistItems.add(ChecklistItem(text = ""))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+ ADD ITEM",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.accentCrimson,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }
                    }

                    // Main Note Body
                    item {
                        if (isMarkdownPreviewMode) {
                            NothingMarkdownRenderer(
                                content = if (body.isBlank()) "*No content to preview yet...*" else body,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            BasicTextField(
                                value = body,
                                onValueChange = {
                                    body = it
                                    if (!isUndoRedoAction) {
                                        undoStack.addLast(Pair(title, it))
                                        redoStack.clear()
                                    }
                                },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = NothingThemeColors.textSecondary,
                                    lineHeight = 24.sp,
                                ),
                                cursorBrush = SolidColor(NothingThemeColors.accentCrimson),
                                minLines = 8,
                                decorationBox = { innerTextField ->
                                    if (body.isEmpty() && checklistItems.isEmpty()) {
                                        Text(
                                            text = "Start writing your note here...\n\nTips:\n• Use #headers or - [ ] for checklists\n• Use #tags for smart organization\n• Record voice memos from the toolbar below",
                                            fontSize = 14.sp,
                                            color = NothingThemeColors.textMuted,
                                            lineHeight = 22.sp,
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }

                // Markdown & Media Quick Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NothingThemeColors.surface)
                        .border(1.dp, NothingThemeColors.surfaceBorder)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Header #
                    ToolbarChip(label = "H#") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        body = if (body.isEmpty()) "# " else "$body\n# "
                    }

                    // Checklist
                    ToolbarChip(label = "[ ] LIST") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        noteType = NoteType.CHECKLIST
                        checklistItems.add(ChecklistItem(text = ""))
                    }

                    // Bold *
                    ToolbarChip(label = "** BOLD") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        body = "$body **word** "
                    }

                    // Code </>
                    ToolbarChip(label = "</> CODE") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        body = "$body\n```\ncode\n```\n"
                    }

                    // Tag #
                    ToolbarChip(label = "# TAG") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        body = "$body #idea "
                    }

                    // Image Attachment
                    ToolbarChip(label = "📷 PHOTO") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pickImageLauncher.launch("image/*")
                    }

                    // 1-Bit Dot Matrix Doodle
                    ToolbarChip(label = "✏️ DOODLE") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDoodleSheet = true
                    }

                    // Audio Record Memo
                    ToolbarChip(label = if (isRecordingAudio) "⏹ STOP" else "🎙 AUDIO") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRecordingAudio) {
                            val result = audioRecorder.stopRecording()
                            if (result != null) {
                                audioUri = result.file.absolutePath
                                audioDurationMs = result.durationMs
                                audioWaveform = result.waveform
                                noteType = NoteType.AUDIO
                            }
                        } else {
                            audioRecorder.startRecording()
                        }
                    }

                    ToolbarChip(label = "✨ TIDY") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val (cleanedBody, extractedChecklist) = SmartNoteIntelligence.formatAndTidy(body)
                        body = cleanedBody
                        if (extractedChecklist.isNotEmpty()) {
                            checklistItems.addAll(extractedChecklist)
                            noteType = NoteType.CHECKLIST
                        }
                        com.example.nomemo.ui.anim.NothingAudioFeedback.playCheckChime()
                    }
                }

                // Bottom Real-Time Telemetry HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NothingThemeColors.background)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KineticTelemetryValue(
                        value = if (calculatedWpm > 0) calculatedWpm.toString() else "--",
                        label = "WPM",
                    )
                    KineticTelemetryValue(
                        value = wordCount.toString(),
                        label = "WORDS",
                    )
                    KineticTelemetryValue(
                        value = charCount.toString(),
                        label = "CHARS",
                    )
                    KineticTelemetryValue(
                        value = "~$readTimeMin MIN",
                        label = "READ",
                    )
                }
            }

            // 1-Bit Dot Matrix Doodle Sheet Overlay
            if (showDoodleSheet) {
                DotMatrixDoodleSheet(
                    onSaveSketch = { sketchPath ->
                        imageUris.add(sketchPath)
                        showDoodleSheet = false
                    },
                    onDismiss = {
                        showDoodleSheet = false
                    },
                )
            }

            // Scheduled Reminder Dialog
            NothingReminderDialog(
                isVisible = showReminderDialog,
                currentReminder = reminderTimestamp,
                onSetReminder = { timestamp ->
                    reminderTimestamp = timestamp
                    NothingReminderManager.setReminder(context, initialNote.id, title, body, timestamp)
                },
                onClearReminder = {
                    reminderTimestamp = null
                    NothingReminderManager.cancelReminder(context, initialNote.id)
                },
                onDismiss = { showReminderDialog = false },
            )

            // Time Machine Version History Drawer
            NothingVersionHistorySheet(
                isVisible = showHistorySheet,
                revisions = revisions.toList(),
                onRestoreRevision = { rev ->
                    title = rev.title
                    body = rev.body
                    showHistorySheet = false
                },
                onDismiss = { showHistorySheet = false },
            )
        }
    }
}

@Composable
private fun ToolbarChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NothingThemeColors.tabTrackBackground)
            .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NothingThemeColors.textPrimary,
        )
    }
}
