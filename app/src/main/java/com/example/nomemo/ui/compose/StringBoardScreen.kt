package com.example.nomemo.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.ui.anim.NothingAudioFeedback
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

data class StringConnection(
    val noteId1: Long,
    val noteId2: Long,
    val isManual: Boolean,
)

/**
 * Minimalist Nothing OS-Style "String Board" (Detective Pinboard) Canvas.
 *
 * - Pure #000000 canvas with subtle 24.dp dot-matrix grid (#1E1E1E dots)
 * - Two-finger pinch-to-zoom (0.5x to 1.6x) and pan gestures
 * - Persisted boardX and boardY coordinates with neat staggered auto-layout fallback
 * - Nothing Red (#D71921) cubic Bezier strings anchored to 6.dp red pin-dots
 * - Long-press & drag red pin-dots to connect notes with live elastic thread
 * - Double-tap red strings to sever / unlink with tactile haptic tick
 * - Spring tension physics during card dragging & faint 1.dp #262626 alignment guides
 * - Silky camera zoom & center animations with crisp spring physics
 * - Decoupled: only displays notes where isOnBoard == true
 * - Floating monochrome bottom dock: + CARD, + PHOTO, IMPORT
 * - Floating selection action pill: LINK THREAD, REMOVE, DELETE
 */
@Composable
fun StringBoardScreen(
    notes: List<Note>,
    homeNotes: List<Note> = emptyList(),
    onOpenNote: (Note) -> Unit,
    onBackClick: () -> Unit,
    onUpdateNotePosition: (Long, Float, Float) -> Unit,
    onLinkNotes: (Long, Long) -> Unit,
    onUnlinkNotes: (Long, Long) -> Unit,
    onCreateBoardCard: (String, String) -> Unit = { _, _ -> },
    onCreateBoardPhoto: (Uri) -> Unit = {},
    onToggleNoteOnBoard: (Long, Boolean) -> Unit = { _, _ -> },
    onRemoveFromBoard: (Long) -> Unit = {},
    onPermanentDelete: (Note) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val springSpec = spring<Float>(dampingRatio = 0.85f, stiffness = 420f)
    val offsetSpringSpec = spring<Offset>(dampingRatio = 0.85f, stiffness = 420f)

    // Camera scale and pan offsets
    val cameraScale = remember { Animatable(0.85f) }
    val cameraPan = remember { Animatable(Offset(60f, 120f), Offset.VectorConverter) }

    // Card dimensions in canvas space
    val cardWidthDp = 220.dp
    val cardHeightDp = 150.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }
    val cardHeightPx = with(density) { cardHeightDp.toPx() }

    // Local mutable state for card positions
    val cardPositions = remember { mutableStateMapOf<Long, Offset>() }

    // Initialize or update positions with staggered auto-layout fallback
    LaunchedEffect(notes) {
        notes.forEachIndexed { index, note ->
            if (!cardPositions.containsKey(note.id)) {
                val initialX = if (note.boardX != 0f) note.boardX else run {
                    val col = index % 3
                    val row = index / 3
                    140f + col * 280f + (row % 2) * 50f
                }
                val initialY = if (note.boardY != 0f) note.boardY else run {
                    val row = index / 3
                    160f + row * 220f
                }
                cardPositions[note.id] = Offset(initialX, initialY)
            }
        }
    }

    var draggingCardId by remember { mutableStateOf<Long?>(null) }
    var selectedBoardCardId by remember { mutableStateOf<Long?>(null) }
    var linkingSourceNoteId by remember { mutableStateOf<Long?>(null) }

    var threadDragSourceId by remember { mutableStateOf<Long?>(null) }
    var threadDragCurrentPos by remember { mutableStateOf<Offset?>(null) }

    // Alignment guide lines while dragging
    var horizontalGuideY by remember { mutableStateOf<Float?>(null) }
    var verticalGuideX by remember { mutableStateOf<Float?>(null) }

    // Dialog & Sheet States
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            onCreateBoardPhoto(uri)
            NothingAudioFeedback.playTactileClick()
        }
    }

    // Animate newly created red strings with PathMeasure
    val newStringAnimProgress = remember { Animatable(1f) }
    var newlyCreatedConnection by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    // Collect all connections (shared #tags or manual links)
    val connections = remember(notes) {
        val list = mutableListOf<StringConnection>()
        for (i in 0 until notes.size) {
            for (j in i + 1 until notes.size) {
                val n1 = notes[i]
                val n2 = notes[j]
                val hasSharedTag = n1.tags.intersect(n2.tags.toSet()).isNotEmpty()
                val isManual = n1.linkedNoteIds.contains(n2.id) || n2.linkedNoteIds.contains(n1.id)
                if (hasSharedTag || isManual) {
                    list.add(StringConnection(n1.id, n2.id, isManual = isManual))
                }
            }
        }
        list
    }

    val selectedCard = remember(selectedBoardCardId, notes) {
        notes.firstOrNull { it.id == selectedBoardCardId }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding(),
    ) {
        // Dot-matrix grid background and connection strings
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Transform gestures: 2-finger pinch-to-zoom (0.5x to 1.6x) and pan
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scope.launch {
                            val newScale = (cameraScale.value * zoom).coerceIn(0.5f, 1.6f)
                            cameraScale.snapTo(newScale)
                            cameraPan.snapTo(cameraPan.value + pan)
                        }
                    }
                }
                // Tap on empty board zooms out to overview & deselects card; double-tap on red thread severs it
                .pointerInput(connections, cardPositions) {
                    detectTapGestures(
                        onTap = {
                            selectedBoardCardId = null
                            linkingSourceNoteId = null
                            scope.launch {
                                cameraScale.animateTo(0.75f, springSpec)
                                cameraPan.animateTo(Offset(80f, 140f), offsetSpringSpec)
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            val worldX = (tapOffset.x - cameraPan.value.x) / cameraScale.value
                            val worldY = (tapOffset.y - cameraPan.value.y) / cameraScale.value
                            val tapWorld = Offset(worldX, worldY)

                            var severed = false
                            for (conn in connections) {
                                val p1 = cardPositions[conn.noteId1] ?: continue
                                val p2 = cardPositions[conn.noteId2] ?: continue
                                val anchor1 = Offset(p1.x + cardWidthPx / 2f, p1.y + 12f)
                                val anchor2 = Offset(p2.x + cardWidthPx / 2f, p2.y + 12f)

                                val dx = anchor2.x - anchor1.x
                                val dy = anchor2.y - anchor1.y
                                val dist = hypot(dx, dy)
                                val sag = (dist * 0.16f).coerceIn(18f, 75f)
                                val c1 = Offset(anchor1.x + dx * 0.25f, anchor1.y + dy * 0.25f + sag)
                                val c2 = Offset(anchor1.x + dx * 0.75f, anchor1.y + dy * 0.75f + sag)

                                for (step in 1..10) {
                                    val t = step / 10f
                                    val oneMinusT = 1f - t
                                    val bx = (oneMinusT * oneMinusT * oneMinusT * anchor1.x) +
                                            (3 * oneMinusT * oneMinusT * t * c1.x) +
                                            (3 * oneMinusT * t * t * c2.x) +
                                            (t * t * t * anchor2.x)
                                    val by = (oneMinusT * oneMinusT * oneMinusT * anchor1.y) +
                                            (3 * oneMinusT * oneMinusT * t * c1.y) +
                                            (3 * oneMinusT * t * t * c2.y) +
                                            (t * t * t * anchor2.y)

                                    if (hypot(tapWorld.x - bx, tapWorld.y - by) <= 32f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        NothingAudioFeedback.playTactileClick()
                                        onUnlinkNotes(conn.noteId1, conn.noteId2)
                                        severed = true
                                        break
                                    }
                                }
                                if (severed) break
                            }
                        },
                    )
                },
        ) {
            val scale = cameraScale.value
            val pan = cameraPan.value

            // 1. Draw 24.dp Dot-Matrix Grid (#1E1E1E)
            val gridSpacing = 24.dp.toPx() * scale
            val startX = (pan.x % gridSpacing)
            val startY = (pan.y % gridSpacing)
            val dotRadius = 1.4f * scale

            var gx = startX - gridSpacing
            while (gx < size.width + gridSpacing) {
                var gy = startY - gridSpacing
                while (gy < size.height + gridSpacing) {
                    drawCircle(
                        color = Color(0xFF1E1E1E),
                        radius = dotRadius,
                        center = Offset(gx, gy),
                    )
                    gy += gridSpacing
                }
                gx += gridSpacing
            }

            // 2. Draw Faint Alignment Guide Lines (1.dp #262626)
            verticalGuideX?.let { gxWorld ->
                val sx = gxWorld * scale + pan.x
                drawLine(
                    color = Color(0xFF262626),
                    start = Offset(sx, 0f),
                    end = Offset(sx, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            horizontalGuideY?.let { gyWorld ->
                val sy = gyWorld * scale + pan.y
                drawLine(
                    color = Color(0xFF262626),
                    start = Offset(0f, sy),
                    end = Offset(size.width, sy),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // 3. Draw Nothing Red (#D71921) String Connections (1.5.dp stroke)
            for (conn in connections) {
                val pos1 = cardPositions[conn.noteId1] ?: continue
                val pos2 = cardPositions[conn.noteId2] ?: continue

                val a1World = Offset(pos1.x + cardWidthPx / 2f, pos1.y + 12f)
                val a2World = Offset(pos2.x + cardWidthPx / 2f, pos2.y + 12f)

                val s1 = Offset(a1World.x * scale + pan.x, a1World.y * scale + pan.y)
                val s2 = Offset(a2World.x * scale + pan.x, a2World.y * scale + pan.y)

                val dx = s2.x - s1.x
                val dy = s2.y - s1.y
                val dist = hypot(dx, dy)

                val isUnderTension = draggingCardId == conn.noteId1 || draggingCardId == conn.noteId2
                val baseSag = (dist * 0.16f).coerceIn(12f, 65f)
                val sag = if (isUnderTension) baseSag * 0.35f else baseSag

                val sc1 = Offset(s1.x + dx * 0.25f, s1.y + dy * 0.25f + sag)
                val sc2 = Offset(s1.x + dx * 0.75f, s1.y + dy * 0.75f + sag)

                val fullPath = Path().apply {
                    moveTo(s1.x, s1.y)
                    cubicTo(sc1.x, sc1.y, sc2.x, sc2.y, s2.x, s2.y)
                }

                val strokeWidth = 1.5.dp.toPx() * scale

                val isNewlyCreated = newlyCreatedConnection?.let {
                    (it.first == conn.noteId1 && it.second == conn.noteId2) ||
                            (it.first == conn.noteId2 && it.second == conn.noteId1)
                } ?: false

                if (isNewlyCreated && newStringAnimProgress.value < 1f) {
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)
                    val segmentPath = Path()
                    pathMeasure.getSegment(0f, pathMeasure.length * newStringAnimProgress.value, segmentPath, true)
                    drawPath(
                        path = segmentPath,
                        color = Color(0xFFD71921),
                        style = Stroke(width = strokeWidth),
                    )
                } else {
                    drawPath(
                        path = fullPath,
                        color = Color(0xFFD71921),
                        style = Stroke(width = strokeWidth),
                    )
                }
            }

            // 4. Live Elastic Red Thread while dragging from pin-dot
            val liveSourceId = threadDragSourceId
            val liveTouch = threadDragCurrentPos
            if (liveSourceId != null && liveTouch != null) {
                val pos = cardPositions[liveSourceId]
                if (pos != null) {
                    val aWorld = Offset(pos.x + cardWidthPx / 2f, pos.y + 12f)
                    val sStart = Offset(aWorld.x * scale + pan.x, aWorld.y * scale + pan.y)
                    val sEnd = liveTouch

                    val dx = sEnd.x - sStart.x
                    val dy = sEnd.y - sStart.y
                    val dist = hypot(dx, dy)
                    val sag = (dist * 0.12f).coerceIn(10f, 40f)

                    val sc1 = Offset(sStart.x + dx * 0.3f, sStart.y + dy * 0.3f + sag)
                    val sc2 = Offset(sStart.x + dx * 0.7f, sStart.y + dy * 0.7f + sag)

                    val threadPath = Path().apply {
                        moveTo(sStart.x, sStart.y)
                        cubicTo(sc1.x, sc1.y, sc2.x, sc2.y, sEnd.x, sEnd.y)
                    }

                    drawPath(
                        path = threadPath,
                        color = Color(0xFFD71921),
                        style = Stroke(width = 2.dp.toPx() * scale),
                    )
                }
            }
        }

        // Render Bento Cards on Canvas
        notes.forEach { note ->
            val pos = cardPositions[note.id] ?: Offset.Zero
            val scale = cameraScale.value
            val pan = cameraPan.value

            val screenX = pos.x * scale + pan.x
            val screenY = pos.y * scale + pan.y

            val isSelected = selectedBoardCardId == note.id
            val isLinkingSource = linkingSourceNoteId == note.id

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                    .size(cardWidthDp * scale, cardHeightDp * scale)
                    .graphicsLayer {
                        shadowElevation = if (draggingCardId == note.id || isSelected) 12.dp.toPx() else 2.dp.toPx()
                    }
                    .clip(RoundedCornerShape(16.dp * scale))
                    .background(Color(0xFF121212))
                    .border(
                        width = if (isSelected || isLinkingSource) 1.5.dp * scale else 1.dp * scale,
                        color = if (isLinkingSource) Color(0xFFD71921) else if (isSelected) Color(0xFFD71921) else Color(0xFF262626),
                        shape = RoundedCornerShape(16.dp * scale),
                    )
                    // Drag gestures for repositioning card
                    .pointerInput(note.id) {
                        detectDragGestures(
                            onDragStart = {
                                draggingCardId = note.id
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = {
                                draggingCardId = null
                                verticalGuideX = null
                                horizontalGuideY = null
                                val currentPos = cardPositions[note.id]
                                if (currentPos != null) {
                                    onUpdateNotePosition(note.id, currentPos.x, currentPos.y)
                                }
                                NothingAudioFeedback.playTactileClick()
                            },
                            onDragCancel = {
                                draggingCardId = null
                                verticalGuideX = null
                                horizontalGuideY = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val current = cardPositions[note.id] ?: Offset.Zero
                                val newX = current.x + (dragAmount.x / cameraScale.value)
                                val newY = current.y + (dragAmount.y / cameraScale.value)
                                cardPositions[note.id] = Offset(newX, newY)

                                var matchedX: Float? = null
                                var matchedY: Float? = null
                                for (other in notes) {
                                    if (other.id == note.id) continue
                                    val oPos = cardPositions[other.id] ?: continue

                                    if (kotlin.math.abs(oPos.y - newY) < 8f) matchedY = oPos.y
                                    else if (kotlin.math.abs((oPos.y + cardHeightPx) - (newY + cardHeightPx)) < 8f) matchedY = oPos.y + cardHeightPx

                                    if (kotlin.math.abs(oPos.x - newX) < 8f) matchedX = oPos.x
                                    else if (kotlin.math.abs((oPos.x + cardWidthPx) - (newX + cardWidthPx)) < 8f) matchedX = oPos.x + cardWidthPx
                                }
                                verticalGuideX = matchedX
                                horizontalGuideY = matchedY
                            },
                        )
                    }
                    // Tap gestures: select card or link thread; double-tap opens note
                    .pointerInput(note, linkingSourceNoteId) {
                        detectTapGestures(
                            onTap = {
                                val sourceId = linkingSourceNoteId
                                if (sourceId != null) {
                                    if (sourceId != note.id) {
                                        onLinkNotes(sourceId, note.id)
                                        newlyCreatedConnection = Pair(sourceId, note.id)
                                        scope.launch {
                                            newStringAnimProgress.snapTo(0f)
                                            newStringAnimProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        NothingAudioFeedback.playTactileClick()
                                    }
                                    linkingSourceNoteId = null
                                    selectedBoardCardId = null
                                } else {
                                    selectedBoardCardId = if (selectedBoardCardId == note.id) null else note.id
                                    scope.launch {
                                        cameraScale.animateTo(1.0f, springSpec)
                                        val targetPanX = 140f - pos.x
                                        val targetPanY = 320f - pos.y
                                        cameraPan.animateTo(Offset(targetPanX, targetPanY), offsetSpringSpec)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDoubleTap = {
                                onOpenNote(note)
                                NothingAudioFeedback.playTactileClick()
                            },
                        )
                    }
                    .padding(10.dp * scale),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top row: Pin Dot & Note Type Telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 6.dp Nothing Red Pin-Dot (Long-press & drag to create live elastic red thread)
                        Box(
                            modifier = Modifier
                                .size(20.dp * scale)
                                .pointerInput(note.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            threadDragSourceId = note.id
                                            threadDragCurrentPos = Offset(screenX + (cardWidthPx / 2f) * scale, screenY + 12f * scale)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            NothingAudioFeedback.playTactileClick()
                                        },
                                        onDragEnd = {
                                            val sourceId = threadDragSourceId
                                            val dropScreen = threadDragCurrentPos
                                            if (sourceId != null && dropScreen != null) {
                                                val target = notes.firstOrNull { candidate ->
                                                    if (candidate.id == sourceId) false
                                                    else {
                                                        val cPos = cardPositions[candidate.id]
                                                        if (cPos != null) {
                                                            val csX = cPos.x * cameraScale.value + cameraPan.value.x
                                                            val csY = cPos.y * cameraScale.value + cameraPan.value.y
                                                            val cw = cardWidthPx * cameraScale.value
                                                            val ch = cardHeightPx * cameraScale.value
                                                            dropScreen.x in csX..(csX + cw) && dropScreen.y in csY..(csY + ch)
                                                        } else false
                                                    }
                                                }

                                                if (target != null) {
                                                    onLinkNotes(sourceId, target.id)
                                                    newlyCreatedConnection = Pair(sourceId, target.id)
                                                    scope.launch {
                                                        newStringAnimProgress.snapTo(0f)
                                                        newStringAnimProgress.animateTo(
                                                            1f,
                                                            tween(260, easing = FastOutSlowInEasing),
                                                        )
                                                    }
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    NothingAudioFeedback.playTactileClick()
                                                }
                                            }
                                            threadDragSourceId = null
                                            threadDragCurrentPos = null
                                        },
                                        onDragCancel = {
                                            threadDragSourceId = null
                                            threadDragCurrentPos = null
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            threadDragCurrentPos = change.position + Offset(screenX, screenY)
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp * scale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD71921)),
                            )
                        }

                        Text(
                            text = note.type,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp * scale,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6E6E73),
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp * scale))

                    // Optional Image Preview if note is MEDIA or has imageUri
                    val previewImage = note.imageUri ?: note.imageUris.firstOrNull()
                    if (!previewImage.isNullOrBlank()) {
                        AsyncImage(
                            model = previewImage,
                            contentDescription = "Pinned Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp * scale)
                                .clip(RoundedCornerShape(6.dp * scale))
                                .border(0.5.dp * scale, Color(0xFF262626), RoundedCornerShape(6.dp * scale)),
                        )
                        Spacer(modifier = Modifier.height(4.dp * scale))
                    }

                    // Title
                    Text(
                        text = note.title.ifBlank { "UNTITLED" }.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp * scale,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(2.dp * scale))

                    // Body snippet
                    Text(
                        text = if (note.isLocked) "[ENCRYPTED BIOMETRIC NOTE]" else note.body.ifBlank { "No content" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp * scale,
                        color = Color(0xFF8E8E93),
                        maxLines = if (previewImage.isNullOrBlank()) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Hashtags
                    if (note.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp * scale)) {
                            note.tags.take(2).forEach { tag ->
                                Text(
                                    text = "#$tag".uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp * scale,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD71921),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Minimalist Top Strip Header: "STRING // BOARD" + GRID Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161616))
                        .border(1.dp, Color(0xFF262626), CircleShape)
                        .clickable {
                            onBackClick()
                            NothingAudioFeedback.playTactileClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to Grid",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "STRING // BOARD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = "${notes.size} PINNED NODES // ${connections.size} STRINGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        color = Color(0xFF6E6E73),
                        letterSpacing = 1.sp,
                    )
                }
            }

            // View toggle button (GRID / BOARD)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161616))
                    .border(1.dp, Color(0xFFD71921), RoundedCornerShape(14.dp))
                    .clickable {
                        onBackClick()
                        NothingAudioFeedback.playTactileClick()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD71921)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GRID VIEW",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }

        // Active Linking Mode Banner
        AnimatedVisibility(
            visible = linkingSourceNoteId != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD71921)),
                    )
                    Text(
                        text = "TAP TARGET CARD TO LINK THREAD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel linking",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                linkingSourceNoteId = null
                            },
                    )
                }
            }
        }

        // Selected Card Action Pill (LINK THREAD, REMOVE, DELETE)
        AnimatedVisibility(
            visible = selectedCard != null && linkingSourceNoteId == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 78.dp),
        ) {
            val card = selectedCard
            if (card != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, Color(0xFFD71921), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // LINK THREAD Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    linkingSourceNoteId = card.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playTactileClick()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = "Link Thread",
                                tint = Color(0xFFD71921),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LINK THREAD",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.8.sp,
                            )
                        }

                        // REMOVE Action (sets isOnBoard = false)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onRemoveFromBoard(card.id)
                                    selectedBoardCardId = null
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    NothingAudioFeedback.playTactileClick()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RemoveCircleOutline,
                                contentDescription = "Remove From Board",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REMOVE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93),
                                letterSpacing = 0.8.sp,
                            )
                        }

                        // DELETE Action (permanently deletes note)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onPermanentDelete(card)
                                    selectedBoardCardId = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playTactileClick()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Permanently Delete",
                                tint = Color(0xFFD71921),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DELETE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD71921),
                                letterSpacing = 0.8.sp,
                            )
                        }

                        // Dismiss button
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Deselect",
                            tint = Color(0xFF6E6E73),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    selectedBoardCardId = null
                                },
                        )
                    }
                }
            }
        }

        // Clean Floating Monochrome Bottom Dock: [+ CARD] [+ PHOTO] [IMPORT]
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Action 1: + CARD (creates a board-only text card)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddCardDialog = true
                                NothingAudioFeedback.playTactileClick()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "+ Card",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ CARD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                    }

                    // Subtle divider dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333)),
                    )

                    // Action 2: + PHOTO (launches PickVisualMedia)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                                NothingAudioFeedback.playTactileClick()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "+ Photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ PHOTO",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                        )
                    }

                    // Subtle divider dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333)),
                    )

                    // Action 3: IMPORT (opens checklist sheet of Home Screen notes)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showImportSheet = true
                                NothingAudioFeedback.playTactileClick()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Import",
                            tint = Color(0xFFD71921),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "IMPORT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD71921),
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }

        // Add Board Card Modal Dialog Overlay
        if (showAddCardDialog) {
            var newCardTitle by remember { mutableStateOf("") }
            var newCardBody by remember { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { showAddCardDialog = false },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF121212))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp),
                ) {
                    Column {
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
                                    text = "NEW BOARD CARD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showAddCardDialog = false },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title Field
                        Text(
                            text = "TITLE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF6E6E73),
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicTextField(
                            value = newCardTitle,
                            onValueChange = { newCardTitle = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(Color(0xFFD71921)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(10.dp),
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Body Field
                        Text(
                            text = "CONTENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF6E6E73),
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicTextField(
                            value = newCardBody,
                            onValueChange = { newCardBody = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                            ),
                            cursorBrush = SolidColor(Color(0xFFD71921)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(10.dp),
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD71921))
                                    .clickable {
                                        if (newCardTitle.isNotBlank() || newCardBody.isNotBlank()) {
                                            onCreateBoardCard(newCardTitle, newCardBody)
                                            NothingAudioFeedback.playTactileClick()
                                        }
                                        showAddCardDialog = false
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "PIN TO BOARD",
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

        // Import Existing Home Notes Checklist Sheet Overlay
        if (showImportSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { showImportSheet = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF121212))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "IMPORT TO STRING BOARD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.5.sp,
                                )
                                Text(
                                    text = "SELECT NOTES TO PIN ONTO CANVAS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFF6E6E73),
                                    letterSpacing = 1.sp,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showImportSheet = false },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val availableNotes = remember(homeNotes) {
                            homeNotes.filter { !it.boardOnly }
                        }

                        if (availableNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "NO HOME NOTES AVAILABLE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6E6E73),
                                    letterSpacing = 1.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(availableNotes, key = { it.id }) { hNote ->
                                    val isPinnedOnBoard = hNote.isOnBoard
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isPinnedOnBoard) Color(0xFF1E1E1E) else Color(0xFF161616))
                                            .border(
                                                1.dp,
                                                if (isPinnedOnBoard) Color(0xFFD71921) else Color(0xFF262626),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .clickable {
                                                onToggleNoteOnBoard(hNote.id, !isPinnedOnBoard)
                                                NothingAudioFeedback.playTactileClick()
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Checkbox box
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isPinnedOnBoard) Color(0xFFD71921) else Color(0xFF222222))
                                                .border(1.dp, if (isPinnedOnBoard) Color(0xFFD71921) else Color(0xFF444444), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (isPinnedOnBoard) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Checked",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = hNote.title.ifBlank { "UNTITLED" }.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${hNote.type} // ${hNote.body.take(30)}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                color = Color(0xFF8E8E93),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF262626))
                                .clickable { showImportSheet = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "DONE",
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
}
