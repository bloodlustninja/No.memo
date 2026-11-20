package com.example.nomemo.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.nomemo.sensor.NothingShakeDetector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
    onMirrorFolderClick: () -> Unit = {},
    onUpdateNotePosition: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onLinkNotes: (Long, Long) -> Unit = { _, _ -> },
    onUnlinkNotes: (Long, Long) -> Unit = { _, _ -> },
    boardNotes: List<Note> = emptyList(),
    hudMessage: String? = null,
    onCreateBoardCard: (String, String) -> Unit = { _, _ -> },
    onCreateBoardPhoto: (android.net.Uri) -> Unit = {},
    onToggleNoteOnBoard: (Long, Boolean) -> Unit = { _, _ -> },
    onRemoveFromBoard: (Long) -> Unit = {},
    onPermanentDeleteBoardNote: (Note) -> Unit = {},
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
    var isBoardView by remember { mutableStateOf(false) }
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    val dissolvingNoteIds = remember { mutableStateListOf<Long>() }
    var lockedNoteForAuth by remember { mutableStateOf<com.example.nomemo.data.entity.Note?>(null) }

    // "NO MEMO" Header Entrance Animation: 0f -> 1f alpha, 5.sp -> 1.5.sp letter-spacing over 280ms, gentle dot pulse
    val headerTitleAlpha = remember { Animatable(0f) }
    val headerLetterSpacing = remember { Animatable(5f) }
    val headerDotAlpha = remember { Animatable(0f) }
    val headerDotScale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        launch {
            headerTitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
        }
        launch {
            headerLetterSpacing.animateTo(
                targetValue = 1.5f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
        }
        launch {
            headerDotAlpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            headerDotScale.animateTo(1.35f, tween(180, easing = FastOutSlowInEasing))
            headerDotScale.animateTo(1.0f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    // Real-time check for active Burn notes reaching expiration -> triggers 300ms AGSL dissolve
    LaunchedEffect(notesList) {
        val now = System.currentTimeMillis()
        notesList.forEach { note ->
            if (note.isBurn) {
                val expireTime = note.burnExpiresAt ?: (note.timestamp + 24 * 3600 * 1000L)
                if (now >= expireTime && !dissolvingNoteIds.contains(note.id)) {
                    dissolvingNoteIds.add(note.id)
                }
            }
        }
    }

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
                        if (isBatchMode) {
                            Text(
                                text = "${selectedNoteIds.size} SELECTED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.accentCrimson,
                                letterSpacing = 1.sp,
                            )
                        } else {
                            Text(
                                text = "NO MEMO",
                                modifier = Modifier.graphicsLayer { alpha = headerTitleAlpha.value },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.textPrimary,
                                letterSpacing = headerLetterSpacing.value.sp,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .graphicsLayer {
                                    scaleX = headerDotScale.value
                                    scaleY = headerDotScale.value
                                    alpha = headerDotAlpha.value
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFD71921)),
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
                        // Search Button with tactile press
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSearchActive) NothingThemeColors.accentCrimson.copy(alpha = 0.2f) else Color.Transparent)
                                .tactilePress {
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

                        // Focus Mode Button with tactile press
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showFocusTimer = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Focus Timer",
                                tint = NothingThemeColors.iconInactive,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Vault / Archives Button with tactile press
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    showArchiveVault = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = "Vault",
                                tint = NothingThemeColors.iconInactive,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Secondary View Toggle Button (GRID / BOARD) with tactile press
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    isBoardView = !isBoardView
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isBoardView) Icons.Outlined.GridView else Icons.Outlined.Hub,
                                contentDescription = if (isBoardView) "Switch to Grid View" else "Switch to String Board",
                                tint = if (isBoardView) NothingThemeColors.accentCrimson else NothingThemeColors.iconInactive,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Batch Select Mode Button with tactile press
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .tactilePress {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                    isBatchMode = !isBatchMode
                                    if (!isBatchMode) selectedNoteIds.clear()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = "Batch Select",
                                tint = if (isBatchMode) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // More (⋮) Overflow Menu with tactile press
                        var showMoreMenu by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                tint = NothingThemeColors.iconInactive,
                                modifier = Modifier.size(20.dp),
                            )

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier
                                    .background(NothingThemeColors.surface)
                                    .border(1.dp, NothingThemeColors.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "MIRROR TO FOLDER (.MD)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NothingThemeColors.textPrimary,
                                            letterSpacing = 1.sp,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onMirrorFolderClick()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "SETTINGS",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NothingThemeColors.textPrimary,
                                            letterSpacing = 1.sp,
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onSettingsClick()
                                    },
                                )
                            }
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
                        // "All Tags" pill with tactile press
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeTag == null) NothingThemeColors.accentCrimson else NothingThemeColors.tabTrackBackground)
                                .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(12.dp))
                                .tactilePress {
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
                                    .tactilePress {
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

                    val gridState = rememberLazyStaggeredGridState()
                    var lastIndex by remember { mutableIntStateOf(0) }
                    var lastOffset by remember { mutableIntStateOf(0) }
                    var targetTilt by remember { mutableFloatStateOf(0f) }

                    // Tie LazyVerticalStaggeredGrid scroll velocity to subtle graphicsLayer tilt/parallax on cards
                    // Clamped to ±1.2f degrees, smoothly returning to 0f on scroll settle using spring(0.84f, 460f)
                    val animatedTilt by animateFloatAsState(
                        targetValue = if (gridState.isScrollInProgress) targetTilt else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.84f,
                            stiffness = 460f,
                        ),
                        label = "scrollInertiaTilt",
                    )

                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
                        }.collectLatest { (index, offset) ->
                            val delta = ((index - lastIndex) * 200 + (offset - lastOffset)).toFloat()
                            if (delta.absoluteValue > 1f) {
                                targetTilt = (delta * 0.04f).coerceIn(-1.2f, 1.2f)
                            }
                            lastIndex = index
                            lastOffset = offset
                            delay(60L)
                            targetTilt = 0f
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
                                state = gridState,
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(filteredNotes, key = { _, note -> note.id }) { index, note ->
                                    val isDissolving = dissolvingNoteIds.contains(note.id)
                                    NothingThrowCard(
                                        onDismiss = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNoteDelete(note)
                                        },
                                        modifier = Modifier
                                            .animateItem()
                                            .nothingAgslDissolve(
                                                isDissolving = isDissolving,
                                                onDissolveComplete = {
                                                    dissolvingNoteIds.remove(note.id)
                                                    onNoteDelete(note)
                                                },
                                            )
                                            .nothingBentoEntrance(index = index)
                                            .graphicsLayer {
                                                rotationZ = animatedTilt
                                                translationY = animatedTilt * 1.5f
                                            },
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
                            .background(NothingThemeColors.surface)
                            .border(1.dp, NothingThemeColors.surfaceBorder, RoundedCornerShape(28.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.tactilePress(enabled = selectedNotes.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                val anyUnpinned = selectedNotes.any { !it.isPinned }
                                onBulkPin?.invoke(selectedNotes, anyUnpinned)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PIN",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.tactilePress(enabled = selectedNotes.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playLockToggle(true)
                                val anyUnlocked = selectedNotes.any { !it.isLocked }
                                onBulkLock?.invoke(selectedNotes, anyUnlocked)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Lock",
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOCK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.tactilePress(enabled = selectedNotes.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                onBulkArchive?.invoke(selectedNotes)
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = "Vault",
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VAULT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNotes.isNotEmpty()) NothingThemeColors.iconActive else NothingThemeColors.iconInactive,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.tactilePress(enabled = selectedNotes.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                com.example.nomemo.ui.anim.NothingAudioFeedback.playTactileClick()
                                selectedNotes.forEach { note ->
                                    if (!dissolvingNoteIds.contains(note.id)) {
                                        dissolvingNoteIds.add(note.id)
                                    }
                                }
                                isBatchMode = false
                                selectedNoteIds.clear()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Shred",
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedNotes.isNotEmpty()) NothingThemeColors.accentCrimson else NothingThemeColors.iconInactive,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHRED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNotes.isNotEmpty()) NothingThemeColors.accentCrimson else NothingThemeColors.iconInactive,
                            )
                        }
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

            // Tactile Nothing OS String Board (Detective Pinboard) Canvas
            AnimatedVisibility(
                visible = isBoardView,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
                exit = fadeOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)) +
                        scaleOut(targetScale = 0.92f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
            ) {
                StringBoardScreen(
                    notes = boardNotes,
                    homeNotes = notesList,
                    onOpenNote = { note ->
                        if (note.isLocked) {
                            lockedNoteForAuth = note
                        } else {
                            onNoteClick(note)
                        }
                    },
                    onBackClick = { isBoardView = false },
                    onUpdateNotePosition = onUpdateNotePosition,
                    onLinkNotes = onLinkNotes,
                    onUnlinkNotes = onUnlinkNotes,
                    onCreateBoardCard = onCreateBoardCard,
                    onCreateBoardPhoto = onCreateBoardPhoto,
                    onToggleNoteOnBoard = onToggleNoteOnBoard,
                    onRemoveFromBoard = onRemoveFromBoard,
                    onPermanentDelete = onPermanentDeleteBoardNote,
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

            // Custom Nothing HUD Notification Pill
            AnimatedVisibility(
                visible = hudMessage != null,
                enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .zIndex(100f),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFFD71921), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD71921)),
                        )
                        Text(
                            text = hudMessage ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
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
                .tactilePress { onTextClick() },
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
                .tactilePress { onChecklistClick() },
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
                .tactilePress { onImageClick() },
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
                .tactilePress { onVoiceClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dock_voice),
                contentDescription = "Voice Memo",
                tint = NothingThemeColors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

