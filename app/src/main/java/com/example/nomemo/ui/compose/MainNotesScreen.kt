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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.nomemo.R
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.voice.VoiceToTextManager
import kotlin.math.absoluteValue

@Composable
fun MainNotesScreen(
    notesList: List<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteDelete: (Note) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDockTextClick: () -> Unit,
    onDockChecklistClick: () -> Unit,
    onDockImageClick: () -> Unit,
    onDockVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceToTextManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val liveSpokenText by voiceManager.spokenText.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) voiceManager.startListening()
    }

    NothingAppTheme(isDarkTheme = isDarkTheme) {
        val categories = remember { listOf("All", "Notes", "Media", "Checklists") }
        val pagerState = rememberPagerState { categories.size }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NothingThemeColors.background)
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NO.MEMO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.textPrimary,
                        letterSpacing = 2.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onSearchClick() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Search",
                                tint = NothingThemeColors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onSettingsClick() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Settings",
                                tint = NothingThemeColors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Synced Top Indicator Bar
                NothingSyncedTabs(
                    pagerState = pagerState,
                    categories = categories,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                // HorizontalPager without translation bleed
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .edgeVignette(topFade = 16.dp, bottomFade = 24.dp),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val filteredNotes = remember(notesList, page) {
                        when (page) {
                            0 -> notesList
                            1 -> notesList.filter { it.type == NoteType.NOTE }
                            2 -> notesList.filter { it.type == NoteType.MEDIA }
                            3 -> notesList.filter { it.type == NoteType.CHECKLIST }
                            else -> notesList
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                )
                                val absOffset = pageOffset.absoluteValue
                                val scale = 1f - (absOffset * 0.08f).coerceIn(0f, 0.08f)
                                scaleX = scale
                                scaleY = scale
                                alpha = (1f - (absOffset * 0.55f)).coerceIn(0f, 1f)
                            },
                    ) {
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
                                        modifier = Modifier.size(64.dp),
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "NO NOTES YET",
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
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(filteredNotes, key = { it.id }) { note ->
                                    BentoCardComposable(
                                        note = note,
                                        isSelected = false,
                                        onClick = { onNoteClick(note) },
                                        onLongClick = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Silent Audio Transcription Card Overlay
            AnimatedVisibility(
                visible = isListening || liveSpokenText.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .padding(horizontal = 24.dp),
            ) {
                FrostedBentoCard(cornerRadius = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isListening) "REC // LISTENING..." else "TRANSCRIPTION",
                                color = if (isListening) Color(0xFFD71921) else NothingThemeColors.textSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            )

                            NothingAudioWaveform(
                                isRecording = isListening,
                                barCount = 5,
                                maxHeight = 16.dp,
                            )
                        }

                        Text(
                            text = if (liveSpokenText.isEmpty()) "Speak now to record note..." else liveSpokenText,
                            color = NothingThemeColors.textPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Floating Capsule Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            ) {
                FloatingCapsuleDockView(
                    onTextClick = onDockTextClick,
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
    }
}

@Composable
fun FloatingCapsuleDockView(
    onTextClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    Row(
        modifier = Modifier
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
            Icon(painterResource(id = R.drawable.ic_dock_text), "Text Note", tint = NothingThemeColors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onChecklistClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(id = R.drawable.ic_dock_checklist), "Checklist", tint = NothingThemeColors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(id = R.drawable.ic_dock_image), "Image Note", tint = NothingThemeColors.textPrimary, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onVoiceClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(id = R.drawable.ic_dock_voice), "Voice Note", tint = NothingThemeColors.textPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Preview
@Composable
fun MainNotesScreenPreview() {
    MainNotesScreen(
        notesList = emptyList(),
        onNoteClick = {},
        onNoteDelete = {},
        onSearchClick = {},
        onSettingsClick = {},
        onDockTextClick = {},
        onDockChecklistClick = {},
        onDockImageClick = {},
        onDockVoiceClick = {},
    )
}