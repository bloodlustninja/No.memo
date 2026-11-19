package com.example.nomemo.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.data.model.NoteRevision
import com.example.nomemo.export.NothingReceiptExporter
import com.example.nomemo.ledger.ExpenseParser
import com.example.nomemo.voice.VoiceToTextManager
import kotlinx.coroutines.delay

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

    var title by remember { mutableStateOf(initialNote.title) }
    var body by remember { mutableStateOf(initialNote.body) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }
    var isLocked by remember { mutableStateOf(initialNote.isLocked) }
    var isBurn by remember { mutableStateOf(initialNote.isBurn) }
    var burnExpiresAt by remember { mutableStateOf(initialNote.burnExpiresAt) }
    var noteType by remember { mutableStateOf(initialNote.type) }
    val imageUris = remember { mutableStateListOf<String>().apply { addAll(initialNote.imageUris) } }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>().apply { addAll(initialNote.checklistItems) } }

    var audioUri by remember { mutableStateOf(initialNote.audioUri) }
    var audioDurationMs by remember { mutableStateOf(initialNote.audioDurationMs) }
    var audioWaveform by remember { mutableStateOf(initialNote.audioWaveform) }
    val revisions = remember { mutableStateListOf<NoteRevision>().apply { addAll(initialNote.revisions) } }

    // Dropdown overflow menu and delete dialog states
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Text-driven Auto-Ledger state and layout tracking
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ledgerAnalysis = remember(title, body, checklistItems.toList()) {
        val fullText = buildString {
            append(title)
            append("\n")
            append(body)
            if (checklistItems.isNotEmpty()) {
                append("\n")
                append(checklistItems.joinToString("\n") { it.text })
            }
        }
        ExpenseParser.parse(fullText)
    }

    // Voice Dictation (SpeechRecognizer via VoiceToTextManager)
    val voiceManager = remember { VoiceToTextManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val spokenText by voiceManager.spokenText.collectAsState()
    var lastVoiceLength by remember { mutableStateOf(0) }

    LaunchedEffect(spokenText) {
        if (isListening && spokenText.isNotBlank()) {
            val delta = spokenText.substring(lastVoiceLength).trim()
            if (delta.isNotEmpty()) {
                body = if (body.isBlank()) delta else "$body $delta"
                lastVoiceLength = spokenText.length
            }
        }
    }

    LaunchedEffect(isListening) {
        if (!isListening) {
            lastVoiceLength = 0
            voiceManager.clearText()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopListening()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) voiceManager.startListening()
    }

    // Undo / Redo Stacks
    val undoStack = remember { ArrayDeque<Pair<String, String>>() }
    val redoStack = remember { ArrayDeque<Pair<String, String>>() }
    var isUndoRedoAction by remember { mutableStateOf(false) }

    // Auto-save debounced effect
    LaunchedEffect(title, body, isPinned, isLocked, isBurn, burnExpiresAt, noteType, imageUris.size, checklistItems.toList(), audioUri) {
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
            isBurn = isBurn,
            burnExpiresAt = burnExpiresAt,
            type = if (checklistItems.isNotEmpty() && noteType != NoteType.AUDIO) NoteType.CHECKLIST else noteType,
            imageUris = imageUris.toList(),
            checklistItems = checklistItems.toList(),
            audioUri = audioUri,
            audioDurationMs = audioDurationMs,
            audioWaveform = audioWaveform,
            tags = extractedTags,
            revisions = revisions.toList(),
            timestamp = System.currentTimeMillis(),
        )
        onSaveNote(updatedNote)
    }

    NothingAppTheme(isDarkTheme = isDarkTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NothingThemeColors.background)
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP ACTION BAR: Back (left), Burn 24h toggle, Pin toggle, More (⋮) (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Back button (left) with 120Hz tactile press
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .tactilePress {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = NothingThemeColors.iconActive,
                        )
                    }

                    // Right action cluster: Burn 24h, Pin, More (⋮) with tactile press
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Burn 24h Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isBurn = !isBurn
                                    burnExpiresAt = if (isBurn) System.currentTimeMillis() + 24 * 3600 * 1000L else null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = "Burn 24h",
                                modifier = Modifier.size(20.dp),
                                tint = if (isBurn) NothingThemeColors.accentCrimson else NothingThemeColors.iconInactive,
                            )
                        }

                        // Pin Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isPinned = !isPinned
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                modifier = Modifier.size(20.dp),
                                tint = if (isPinned) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                        }

                        // More (⋮) Overflow Menu
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .tactilePress {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showMoreMenu = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options",
                                    modifier = Modifier.size(20.dp),
                                    tint = NothingThemeColors.iconInactive,
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier
                                    .background(NothingThemeColors.surface)
                                    .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(12.dp)),
                            ) {
                                // Lock / Unlock Note
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isLocked) "UNLOCK NOTE" else "LOCK NOTE",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = NothingThemeColors.textPrimary,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                            contentDescription = "Lock Note",
                                            modifier = Modifier.size(20.dp),
                                            tint = NothingThemeColors.iconInactive,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isLocked = !isLocked
                                    },
                                )

                                // Export Receipt
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "EXPORT RECEIPT",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = NothingThemeColors.textPrimary,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                            contentDescription = "Receipt",
                                            modifier = Modifier.size(20.dp),
                                            tint = NothingThemeColors.iconInactive,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    },
                                )

                                // Export CSV (Ledger)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "EXPORT CSV",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = NothingThemeColors.textPrimary,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FileDownload,
                                            contentDescription = "Export CSV",
                                            modifier = Modifier.size(20.dp),
                                            tint = NothingThemeColors.iconInactive,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        ExpenseParser.exportCsv(context, title, body)
                                    },
                                )

                                // Reset Ledger
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "RESET LEDGER",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = NothingThemeColors.textSecondary,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = "Reset Ledger",
                                            modifier = Modifier.size(20.dp),
                                            tint = NothingThemeColors.iconInactive,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (!isUndoRedoAction) {
                                            undoStack.addLast(Pair(title, body))
                                            redoStack.clear()
                                        }
                                        body = ExpenseParser.resetLedger(body)
                                    },
                                )

                                // Delete Note
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "DELETE NOTE",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = NothingThemeColors.accentCrimson,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete Note",
                                            modifier = Modifier.size(20.dp),
                                            tint = NothingThemeColors.accentCrimson,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showDeleteConfirmDialog = true
                                    },
                                )
                            }
                        }
                    }
                }

                // SCROLLABLE EDITOR CONTENT
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
                            cursorBrush = SolidColor(NothingThemeColors.iconActive),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = "UNTITLED",
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

                    // Attached Images Strip (if any)
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
                                            .background(NothingThemeColors.surface)
                                            .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(16.dp)),
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(uriStr),
                                            contentDescription = "Attached Media",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                imageUris.remove(uriStr)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f)),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove Media",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Audio Voice Memo Player (if present)
                    if (!audioUri.isNullOrBlank()) {
                        item {
                            DotMatrixAudioPlayer(
                                audioUri = audioUri!!,
                                durationMs = audioDurationMs,
                                waveform = audioWaveform,
                            )
                        }
                    }

                    // Interactive Checklist Items
                    if (checklistItems.isNotEmpty()) {
                        itemsIndexed(checklistItems) { index, item ->
                            EditorChecklistItemRow(
                                item = item,
                                onCheckedChange = { isChecked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    checklistItems[index] = item.copy(isChecked = isChecked)
                                },
                                onTextChange = { newText ->
                                    if (newText.contains("\n")) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val clean = newText.replace("\n", "").trim()
                                        if (clean.isEmpty() && item.text.trim().isEmpty()) {
                                            // Pressing Enter on an empty item reverts to normal text!
                                            checklistItems.removeAt(index)
                                            if (checklistItems.isEmpty()) {
                                                noteType = NoteType.NOTE
                                            }
                                        } else {
                                            // Pressing Enter continues the checklist!
                                            checklistItems[index] = item.copy(text = clean)
                                            checklistItems.add(index + 1, ChecklistItem(text = ""))
                                        }
                                    } else {
                                        checklistItems[index] = item.copy(text = newText)
                                    }
                                },
                                onEnterNext = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (item.text.trim().isEmpty()) {
                                        checklistItems.removeAt(index)
                                        if (checklistItems.isEmpty()) {
                                            noteType = NoteType.NOTE
                                        }
                                    } else {
                                        checklistItems.add(index + 1, ChecklistItem(text = ""))
                                    }
                                },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    checklistItems.removeAt(index)
                                    if (checklistItems.isEmpty()) {
                                        noteType = NoteType.NOTE
                                    }
                                },
                            )
                        }
                    }

                    // Main Note Body with Inline Smart Checklist Detection and Auto-Ledger Line Badges
                    item {
                        BasicTextField(
                            value = body,
                            onValueChange = { newBody ->
                                // Inline Smart Checklists: typing "[] " or "- " at start of line converts that line
                                val lines = newBody.split("\n")
                                val triggerIndex = lines.indexOfFirst { line ->
                                    if (line.startsWith("[] ")) {
                                        true
                                    } else if (line.startsWith("- ")) {
                                        val rest = line.removePrefix("- ").trimStart()
                                        // If line starts with a number, math, or currency symbol (e.g. "- 180" or "- ₹50"),
                                        // it is an expense deduction, NOT a checklist item!
                                        val isExpenseNumber = rest.matches("^[+-]?\\s*[₹$€£RsINR]*\\s*[\\d,]+.*".toRegex(RegexOption.IGNORE_CASE))
                                        !isExpenseNumber
                                    } else {
                                        false
                                    }
                                }
                                if (triggerIndex != -1) {
                                    val triggerLine = lines[triggerIndex]
                                    val itemText = if (triggerLine.startsWith("[] ")) {
                                        triggerLine.removePrefix("[] ")
                                    } else {
                                        triggerLine.removePrefix("- ")
                                    }
                                    val remainingLines = lines.toMutableList()
                                    remainingLines.removeAt(triggerIndex)
                                    body = remainingLines.joinToString("\n")

                                    if (!isUndoRedoAction) {
                                        undoStack.addLast(Pair(title, body))
                                        redoStack.clear()
                                    }
                                    checklistItems.add(ChecklistItem(text = itemText))
                                    noteType = NoteType.CHECKLIST
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else {
                                    body = newBody
                                    if (!isUndoRedoAction) {
                                        undoStack.addLast(Pair(title, newBody))
                                        redoStack.clear()
                                    }
                                }
                            },
                            onTextLayout = { textLayoutResult = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = NothingThemeColors.textSecondary,
                                lineHeight = 24.sp,
                            ),
                            cursorBrush = SolidColor(NothingThemeColors.iconActive),
                            minLines = 8,
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (body.isEmpty() && checklistItems.isEmpty()) {
                                        Text(
                                            text = "Write something...",
                                            fontSize = 15.sp,
                                            color = NothingThemeColors.textMuted,
                                            lineHeight = 24.sp,
                                        )
                                    }
                                    innerTextField()

                                    // Right-align parsed currency amounts on each expense line in muted monospace (#8E8E93) as user types
                                    if (ledgerAnalysis.isLedgerMode) {
                                        val layout = textLayoutResult
                                        val lines = body.split("\n")
                                        var charOffset = 0
                                        lines.forEach { line ->
                                            val parsed = ExpenseParser.parseLine(line, ledgerAnalysis.currencySymbol)
                                            if (parsed != null && parsed.formattedBadge != null) {
                                                val lineTop = if (layout != null && charOffset <= body.length) {
                                                    val visualLine = layout.getLineForOffset(charOffset)
                                                    with(LocalDensity.current) { layout.getLineTop(visualLine).toDp() }
                                                } else null

                                                if (lineTop != null) {
                                                    Text(
                                                        text = parsed.formattedBadge,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF8E8E93),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(y = lineTop),
                                                    )
                                                }
                                            }
                                            charOffset += line.length + 1
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // BOTTOM AREA: Minimalist Ledger Summary Strip + Floating Keyboard Capsule Pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Minimalist Non-intrusive Monospace Summary Strip (Active strictly in Ledger Mode)
                    AnimatedVisibility(
                        visible = ledgerAnalysis.isLedgerMode,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingThemeColors.surface)
                                    .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "TOTAL: ${ExpenseParser.formatCurrency(ledgerAnalysis.totalExpense, ledgerAnalysis.currencySymbol)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ledgerAnalysis.totalExpense < 0) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                                    letterSpacing = 0.5.sp,
                                )

                                if (ledgerAnalysis.splitCount != null && ledgerAnalysis.perPersonSplit != null) {
                                    Text(
                                        text = "•",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textMuted,
                                    )
                                    Text(
                                        text = "${ledgerAnalysis.splitCount} WAYS: ${ExpenseParser.formatCurrency(ledgerAnalysis.perPersonSplit!!, ledgerAnalysis.currencySymbol)}/ea",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textSecondary,
                                        letterSpacing = 0.5.sp,
                                    )
                                }

                                if (ledgerAnalysis.capAmount != null && ledgerAnalysis.remainingBudget != null) {
                                    Text(
                                        text = "•",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = NothingThemeColors.textMuted,
                                    )
                                    val isOver = ledgerAnalysis.remainingBudget!! < 0
                                    val remText = if (!isOver) {
                                        "REM: ${ledgerAnalysis.currencySymbol}${ExpenseParser.formatAmountWithCommas(ledgerAnalysis.remainingBudget!!)}"
                                    } else {
                                        "OVER: ${ledgerAnalysis.currencySymbol}${ExpenseParser.formatAmountWithCommas(kotlin.math.abs(ledgerAnalysis.remainingBudget!!))}"
                                    }
                                    Text(
                                        text = remText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ledgerAnalysis.isCapExceededOrNearLimit) NothingThemeColors.accentCrimson else NothingThemeColors.textSecondary,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Floating Keyboard Capsule Pill Dock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .height(58.dp)
                                .clip(RoundedCornerShape(29.dp))
                                .background(NothingThemeColors.surface)
                                .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(29.dp))
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 1. Checklist Toggle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .tactilePress {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        checklistItems.add(ChecklistItem(text = ""))
                                        noteType = NoteType.CHECKLIST
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckBox,
                                    contentDescription = "Checklist Toggle",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (checklistItems.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                                )
                            }

                            // 2. Voice Dictation (SpeechRecognizer)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .tactilePress {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isListening) {
                                            voiceManager.stopListening()
                                        } else {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO,
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                voiceManager.startListening()
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Voice Dictation",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isListening) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                                )
                            }

                            // 3. Undo
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .tactilePress(enabled = undoStack.size > 1) {
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
                                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                                    contentDescription = "Undo",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (undoStack.size > 1) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                                )
                            }

                            // 4. Redo
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .tactilePress(enabled = redoStack.isNotEmpty()) {
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
                                    imageVector = Icons.AutoMirrored.Outlined.Redo,
                                    contentDescription = "Redo",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (redoStack.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                                )
                            }
                        }
                    }
                }
            }

            // Delete Confirmation Dialog (Strict OLED with Nothing Red #D71921 confirmation)
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    containerColor = NothingThemeColors.surface,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "DELETE MEMO?",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingThemeColors.textPrimary,
                            letterSpacing = 1.sp,
                        )
                    },
                    text = {
                        Text(
                            text = "This action is permanent and cannot be undone.",
                            fontSize = 13.sp,
                            color = NothingThemeColors.textSecondary,
                            lineHeight = 18.sp,
                        )
                    },
                    confirmButton = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingThemeColors.accentCrimson)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showDeleteConfirmDialog = false
                                    if (initialNote.id != 0L) {
                                        onDeleteNote(initialNote.id)
                                    }
                                    onBackClick()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "DELETE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    },
                    dismissButton = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showDeleteConfirmDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "CANCEL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = NothingThemeColors.textSecondary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorChecklistItemRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onEnterNext: () -> Unit,
    onDelete: () -> Unit,
) {
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
            .clip(RoundedCornerShape(8.dp))
            .background(NothingThemeColors.surface)
            .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Monochrome Checkbox with 120Hz tactile press
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (item.isChecked) NothingThemeColors.iconActive else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (item.isChecked) NothingThemeColors.iconActive else NothingThemeColors.surfaceBorder,
                    shape = RoundedCornerShape(4.dp),
                )
                .tactilePress { onCheckedChange(!item.isChecked) },
            contentAlignment = Alignment.Center,
        ) {
            if (item.isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Checklist text input with dynamic alpha dimming
        val resolvedTextColor = NothingThemeColors.textPrimary.copy(alpha = textAlpha)
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = resolvedTextColor,
            ),
            cursorBrush = SolidColor(NothingThemeColors.iconActive),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEnterNext() }),
            modifier = Modifier
                .weight(1f)
                .drawWithContent {
                    drawContent()
                    if (progress > 0.001f) {
                        val strokeW = 1.2.dp.toPx()
                        val y = size.height / 2f
                        drawLine(
                            color = resolvedTextColor,
                            start = Offset(0f, y),
                            end = Offset(size.width * progress, y),
                            strokeWidth = strokeW,
                        )
                    }
                },
        )

        // Delete Row Button with tactile press
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .tactilePress { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove Item",
                modifier = Modifier.size(16.dp),
                tint = NothingThemeColors.iconInactive,
            )
        }
    }
}
