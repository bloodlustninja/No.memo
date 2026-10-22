package com.example.nomemo.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Nothing OS 5.0 Synced Top Category Tab Indicator.
 *
 * Glides the Nothing Crimson Red (#D71921) indicator capsule continuously across tabs in exact
 * 1:1 real-time sync with HorizontalPager swipe fraction and emits haptic ticks on whole-page landing.
 */
@Composable
fun NothingSyncedTabs(
    pagerState: PagerState,
    categories: List<String>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val tabPositions = remember { mutableStateListOf<Float>() }
    val tabWidths = remember { mutableStateListOf<Float>() }

    LaunchedEffect(categories.size) {
        tabPositions.clear()
        tabWidths.clear()
        repeat(categories.size) {
            tabPositions.add(0f)
            tabWidths.add(0f)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val fromIndex = pagerState.currentPage
    val offsetFraction = pagerState.currentPageOffsetFraction
    val toIndex = if (offsetFraction > 0) fromIndex + 1 else fromIndex - 1

    val fromX = tabPositions.getOrNull(fromIndex) ?: 0f
    val fromWidth = tabWidths.getOrNull(fromIndex) ?: 0f

    val toX = tabPositions.getOrNull(toIndex) ?: fromX
    val toWidth = tabWidths.getOrNull(toIndex) ?: fromWidth

    val fraction = abs(offsetFraction)
    val indicatorX = fromX + ((toX - fromX) * fraction)
    val indicatorWidth = fromWidth + ((toWidth - fromWidth) * fraction)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(NothingThemeColors.tabTrackBackground)
            .border(1.dp, NothingThemeColors.tabTrackBorder, RoundedCornerShape(22.dp))
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Continuous Real-Time Gliding Nothing Crimson Red Capsule (#D71921)
        if (indicatorWidth > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = indicatorX
                    }
                    .width(with(density) { indicatorWidth.toDp() })
                    .clip(RoundedCornerShape(18.dp))
                    .background(NothingThemeColors.accentCrimson),
            )
        }

        // Tab Labels Row
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            categories.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            if (index < tabPositions.size) {
                                tabPositions[index] = coords.positionInParent().x
                                tabWidths[index] = coords.size.width.toFloat()
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else NothingThemeColors.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Preview Composable demonstrating Nothing Synced Tabs.
 */
@Preview
@Composable
fun NothingSyncedTabsPreview() {
    val categories = remember { listOf("All", "Notes", "Media", "Checklists") }
    val pagerState = rememberPagerState { categories.size }

    NothingSyncedTabs(
        pagerState = pagerState,
        categories = categories,
        modifier = Modifier.padding(16.dp),
    )
}