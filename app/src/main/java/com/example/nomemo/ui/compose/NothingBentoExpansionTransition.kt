package com.example.nomemo.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sharp Industrial Easing for Nothing OS 5.0 Morphing (Cubic Bezier 0.2, 0.0, 0.0, 1.0).
 */
val SharpIndustrialEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Bento Note Data model for expansion preview.
 */
data class BentoNoteItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullText: String,
    val category: String = "NOTE",
    val cardSize: BentoCardSize = BentoCardSize.TWO_BY_ONE,
)

/**
 * Seamless Bento Card Expansion Transition in Nothing OS 5.0.
 *
 * Morphs a compact Bento Card (e.g. 1x1 or 2x1) into an expanded full-page view
 * maintaining 28dp rounded corners, crisp frosted borders, and sharp industrial easing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NothingBentoExpansionContainer(
    selectedNote: BentoNoteItem?,
    onSelectNote: (BentoNoteItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = selectedNote,
            transitionSpec = {
                fadeIn(animationSpec = tween(300, easing = SharpIndustrialEasing)) togetherWith
                        fadeOut(animationSpec = tween(250, easing = SharpIndustrialEasing))
            },
            label = "BentoExpansionTransition",
        ) { targetNote ->
            if (targetNote == null) {
                // Collapsed Compact Grid Item Preview
                CompactBentoCardView(
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                ) { note ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectNote(note)
                }
            } else {
                // Expanded Full-Page Note View
                ExpandedBentoNoteScreen(
                    note = targetNote,
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    onCollapse = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectNote(null)
                    },
                )
            }
        }
    }
}

/**
 * Compact Bento Widget Preview.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CompactBentoCardView(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onExpand: (BentoNoteItem) -> Unit,
) {
    val sampleNote = remember {
        BentoNoteItem(
            id = "note_01",
            title = "SYSTEM ARCHITECTURE",
            snippet = "Nothing OS 5.0 Bento Grid System & Hardware Springs",
            fullText = "Nothing OS 5.0 Bento Grid System Architecture Specs:\n\n" +
                    "1. Mechanical Spring Physics (StiffnessMedium = 1500f, DampingRatioNoBouncy = 1.0f)\n" +
                    "2. Sharp Industrial Cubic Bezier Easing (0.2, 0.0, 0.0, 1.0)\n" +
                    "3. 28dp Continuous Rounded Corners\n" +
                    "4. Frosted Glassmorphism Overlay (80% opacity translucent dark surface + 1dp white stroke)",
            cardSize = BentoCardSize.TWO_BY_ONE,
        )
    }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(130.dp)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "note_card_bounds_${sampleNote.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy,
                        )
                    },
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xCC141418))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .clickable { onExpand(sampleNote) }
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = sampleNote.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "note_title_${sampleNote.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = sampleNote.snippet,
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    modifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "note_snippet_${sampleNote.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                )
            }
        }
    }
}

/**
 * Expanded Full-Page Bento Note Screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ExpandedBentoNoteScreen(
    note: BentoNoteItem,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    onCollapse: () -> Unit,
) {
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "note_card_bounds_${note.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy,
                        )
                    },
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Expanded Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = note.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.sharedElement(
                            state = rememberSharedContentState(key = "note_title_${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { onCollapse() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full Text Content
                Text(
                    text = note.fullText,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .sharedElement(
                            state = rememberSharedContentState(key = "note_snippet_${note.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                )
            }
        }
    }
}