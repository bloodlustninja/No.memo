package com.example.nomemo.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.nomemo.sensor.NothingShakeDetector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.nomemo.R
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.voice.VoiceToTextManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun MainNotesScreen(
    notesList: List<Note>,
    archivedNotes: List<Note> = emptyList(),
    availableTags: List<String> = emptyList(),
    activeTag: String? = null,
    onNoteClick: (Note) -> Unit,
    onNoteDelete: (Note) -> Unit,
    onChecklistToggle: (Note, String) -> Unit = { _, _ -> },
    onLockToggle: (Note) -> Unit = {},
    onTagSelect: (String?) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onRestoreNote: (Note) -> Unit = {},
    onPermanentDelete: (Note) -> Unit = {},
    onBulkArchive: ((List<Note>) -> Unit)? = null,
    onBulkDelete: ((List<Note>) -> Unit)? = null,
    onBulkPin: ((List<Note>, Boolean) -> Unit)? = null,
    onBulkLock: ((List<Note>, Boolean) -> Unit)? = null,
    onSettingsClick: () -> Unit,
    onDockTextClick: () -> Unit,
    onDockChecklistClick: () -> Unit,
    onDockImageClick: () -> Unit,
    onDockVoiceClick: () -> Unit,
    onSaveFlashNote: (Note) -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val voiceManager = remember { VoiceToTextManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val liveSpokenText by voiceManager.spokenText.collectAsState()
    val rmsVolume by voiceManager.rmsVolume.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showFlashNote by remember { mutableStateOf(false) }
    var showFocusTimer by remember { mutableStateOf(false) }
    var showArchiveVault by remember { mutableStateOf(false) }
    var showKnowledgeGraph by remember { mutableStateOf(false) }
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    var lockedNoteForAuth by remember { mutableStateOf<com.example.nomemo.data.entity.Note?>(null) }

    DisposableEffect(Unit) {
        val detector = NothingShakeDetector(context) {
            showFlashNote = true
            com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
        }
        detector.start()
        onDispose {
            detector.stop()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) voiceManager.startListening()
    }

    NothingAppTheme(isDarkTheme = isDarkTheme) {
        val categories = remember { listOf("ALL", "NOTES", "CHECKLISTS", "MEDIA", "AUDIO") }
        val pagerState = rememberPagerState { categories.size }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NothingThemeColors.background)
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KineticDotMatrixText(
                            text = if (isBatchMode) "${selectedNoteIds.size} SELECTED" else "NO.MEMO",
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (isBatchMode) 17.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBatchMode) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                            letterSpacing = if (isBatchMode) 1.sp else 2.sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingThemeColors.accentCrimson),
                        )
                        if (isBatchMode) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NothingThemeColors.tabTrackBackground)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (selectedNoteIds.size == notesList.size) {
                                            selectedNoteIds.clear()
                                        } else {
                                            selectedNoteIds.clear()
                                            selectedNoteIds.addAll(notesList.map { it.id })
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (selectedNoteIds.size == notesList.size) "NONE" else "ALL",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingThemeColors.textSecondary,
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSearchActive) NothingThemeColors.accentCrimson.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) {
                                        searchQuery = ""
                                        onSearchQueryChange("")
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Search",
                                tint = if (isSearchActive) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Focus Mode Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showFocusTimer = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⏱", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Vault / Archives Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showArchiveVault = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("📦", fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Bento Knowledge Graph Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showKnowledgeGraph = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🕸", fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Batch Select Mode Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    isBatchMode = !isBatchMode
                                    if (!isBatchMode) selectedNoteIds.clear()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "☑",
                                fontSize = 16.sp,
                                color = if (isBatchMode) NothingThemeColors.accentCrimson else NothingThemeColors.textPrimary,
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Settings Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSettingsClick()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Settings",
                                tint = NothingThemeColors.textPrimary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NothingThemeColors.tabTrackBackground)
                            .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearchQueryChange(it)
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = NothingThemeColors.textPrimary,
                            ),
                            cursorBrush = SolidColor(NothingThemeColors.accentCrimson),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "SEARCH NOTES // TAGS // TEXT...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = NothingThemeColors.textMuted,
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Synced Top Category Tab Indicator Bar
                NothingSyncedTabs(
                    pagerState = pagerState,
                    categories = categories,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )

                // Dynamic Hashtag Filter Pills
                if (availableTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // "All Tags" pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeTag == null) NothingThemeColors.accentCrimson else NothingThemeColors.tabTrackBackground)
                                .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTagSelect(null)
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "#ALL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTag == null) Color.White else NothingThemeColors.textSecondary,
                            )
                        }

                        availableTags.forEach { tag ->
                            val isSelected = activeTag == tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NothingThemeColors.accentCrimson else NothingThemeColors.tabTrackBackground)
                                    .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTagSelect(if (isSelected) null else tag)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else NothingThemeColors.textSecondary,
                                )
                            }
                        }
                    }
                }

                // HorizontalPager with 120 FPS hardware acceleration
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .edgeVignette(topFade = 12.dp, bottomFade = 24.dp),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val filteredNotes = remember(notesList, page) {
                        when (page) {
                            0 -> notesList
                            1 -> notesList.filter { it.type == NoteType.NOTE }
                            2 -> notesList.filter { it.type == NoteType.CHECKLIST }
                            3 -> notesList.filter { it.type == NoteType.MEDIA }
                            4 -> notesList.filter { it.type == NoteType.AUDIO || !it.audioUri.isNullOrBlank() }
                            else -> notesList
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                val absOffset = pageOffset.absoluteValue
                                val scale = 1f - (absOffset * 0.06f).coerceIn(0f, 0.06f)
                                scaleX = scale
                                scaleY = scale
                                alpha = (1f - (absOffset * 0.50f)).coerceIn(0f, 1f)
                            },
                    ) {
                        // Procedural Skia GPU Dot Matrix Canvas
                        NothingDotMatrixBackground(modifier = Modifier.fillMaxSize())

                        if (filteredNotes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_empty_notes),
                                        contentDescription = "No Notes",
                                        tint = NothingThemeColors.textSecondary,
                                        modifier = Modifier.size(56.dp),
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "ZERO NOTES FOUND",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingThemeColors.textSecondary,
                                        letterSpacing = 2.sp,
                                    )
                                }
                            }
                        } else {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(filteredNotes, key = { _, note -> note.id }) { index, note ->
                                    NothingThrowCard(
                                        onDismiss = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNoteDelete(note)
                                        },
                                        modifier = Modifier.nothingBentoEntrance(index = index),
                                    ) {
                                        BentoCardComposable(
                                            note = note,
                                            isSelected = selectedNoteIds.contains(note.id),
                                            isSelectionMode = isBatchMode,
                                            onClick = {
                                                if (isBatchMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                                    if (selectedNoteIds.contains(note.id)) {
                                                        selectedNoteIds.remove(note.id)
                                                    } else {
                                                        selectedNoteIds.add(note.id)
                                                    }
                                                } else if (note.isLocked) {
                                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                                    lockedNoteForAuth = note
                                                } else {
                                                    onNoteClick(note)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                                if (!isBatchMode) {
                                                    isBatchMode = true
                                                    selectedNoteIds.add(note.id)
                                                } else {
                                                    onLockToggle(note)
                                                }
                                            },
                                            onChecklistToggle = { itemId ->
                                                onChecklistToggle(note, itemId)
                                            },
                                            onTagClick = { tag ->
                                                onTagSelect(tag)
                                            },
                                            onLockToggle = {
                                                onLockToggle(note)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Silent Speech-to-Text Overlay
            NothingVoiceOverlay(
                isListening = isListening,
                spokenText = liveSpokenText,
                rmsVolume = rmsVolume,
                onSaveClick = { parsedNote ->
                    voiceManager.stopListening()
                    voiceManager.clearText()
                    val newNote = Note(
                        title = parsedNote.title.ifBlank { "VOICE MEMO" },
                        body = parsedNote.body,
                        type = NoteType.NOTE,
                        timestamp = System.currentTimeMillis(),
                    )
                    onSaveFlashNote(newNote)
                },
                onDismissClick = {
                    voiceManager.stopListening()
                    voiceManager.clearText()
                },
            )

            // Flash Note Quick-Draw Overlay
            FlashNoteOverlay(
                isVisible = showFlashNote,
                onDismiss = { showFlashNote = false },
                onSaveNote = { note ->
                    onSaveFlashNote(note)
                },
            )

            // Floating Capsule Dock at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                if (isBatchMode) {
                    val selectedNotes = remember(selectedNoteIds.toList(), notesList) {
                        notesList.filter { selectedNoteIds.contains(it.id) }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xE6141418))
                            .border(1.2.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "📌 PIN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNotes.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.clickable(enabled = selectedNotes.isNotEmpty()) {
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                val anyUnpinned = selectedNotes.any { !it.isPinned }
                                onBulkPin?.invoke(selectedNotes, anyUnpinned)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        )
                        Text(
                            text = "🔒 LOCK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNotes.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.clickable(enabled = selectedNotes.isNotEmpty()) {
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playLockToggle(true)
                                val anyUnlocked = selectedNotes.any { !it.isLocked }
                                onBulkLock?.invoke(selectedNotes, anyUnlocked)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        )
                        Text(
                            text = "📦 VAULT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNotes.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.clickable(enabled = selectedNotes.isNotEmpty()) {
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                onBulkArchive?.invoke(selectedNotes)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        )
                        Text(
                            text = "🗑 SHRED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedNotes.isNotEmpty()) NothingThemeColors.accentCrimson else Color.Gray,
                            modifier = Modifier.clickable(enabled = selectedNotes.isNotEmpty()) {
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                onBulkDelete?.invoke(selectedNotes)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        )
                    }
                } else {
                    FloatingCapsuleDockView(
                        onTextClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showFlashNote = true
                        },
                        onChecklistClick = onDockChecklistClick,
                        onImageClick = onDockImageClick,
                        onVoiceClick = {
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    voiceManager.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            onDockVoiceClick()
                        },
                    )
                }
            }

            // Focus Timer Pomodoro Studio Overlay
            if (showFocusTimer) {
                NothingFocusTimerScreen(
                    onDismiss = { showFocusTimer = false },
                )
            }

            // Archive & Recovery Vault Overlay
            if (showArchiveVault) {
                NothingArchiveVaultScreen(
                    archivedNotes = archivedNotes,
                    onRestoreNote = { note ->
                        onRestoreNote(note)
                    },
                    onPermanentDelete = { note ->
                        onPermanentDelete(note)
                    },
                    onBackClick = { showArchiveVault = false },
                )
            }

            // Bento Knowledge Graph 2D Constellation Overlay
            if (showKnowledgeGraph) {
                BentoKnowledgeGraphScreen(
                    notes = notesList,
                    onOpenNote = { note ->
                        showKnowledgeGraph = false
                        if (note.isLocked) {
                            lockedNoteForAuth = note
                        } else {
                            onNoteClick(note)
                        }
                    },
                    onBackClick = { showKnowledgeGraph = false },
                )
            }

            // Physical Biometric & Glyph Hardware Lock Overlay
            NothingBiometricLockOverlay(
                isVisible = lockedNoteForAuth != null,
                noteTitle = lockedNoteForAuth?.title ?: "LOCKED MEMO",
                onAuthenticated = {
                    val target = lockedNoteForAuth
                    lockedNoteForAuth = null
                    if (target != null) onNoteClick(target)
                },
                onDismiss = {
                    lockedNoteForAuth = null
                },
            )
        }
    }
}

@Composable
fun FloatingCapsuleDockView(
    onTextClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(260.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(NothingThemeColors.dockBackground)
            .border(1.dp, NothingThemeColors.dockBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onTextClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dock_text),
                contentDescription = "Flash Note",
                tint = NothingThemeColors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onChecklistClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dock_checklist),
                contentDescription = "Checklist",
                tint = NothingThemeColors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dock_image),
                contentDescription = "Image Note",
                tint = NothingThemeColors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onVoiceClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dock_voice),
                contentDescription = "Voice Memo",
                tint = NothingThemeColors.accentCrimson,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

