package com.example.nomemo.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * High Stiffness constant for Nothing OS 5.0 snappy spring entrance.
 */
const val StiffnessMediumHigh: Float = 5750f

/**
 * Asymmetric Bento Card Dimensions in the Nothing OS 5.0 Grid System.
 */
enum class BentoCardSize(val spanX: Int, val spanY: Int) {
    ONE_BY_ONE(spanX = 1, spanY = 1),
    TWO_BY_ONE(spanX = 2, spanY = 1),
    TWO_BY_TWO(spanX = 2, spanY = 2);

    fun heightDp(): Dp {
        return (spanX * spanY * 55).dp
    }
}

/**
 * Modifier for Nothing OS 5.0 Bento Grid Staggered Cascade Entrance.
 *
 * Entrance Motion:
 * - 35ms delay per index.
 * - Fades in (alpha: 0f -> 1f).
 * - Slides up 20.dp -> 0.dp.
 * - Uses spring(dampingRatio = 0.84f, stiffness = 460f) for 120Hz flagship tactile feel.
 */
fun Modifier.nothingBentoEntrance(
    index: Int,
    row: Int = 0,
    col: Int = 0,
    staggerDelayMs: Long = 35L,
    initialOffsetY: Dp = 20.dp,
): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember(initialOffsetY) { Animatable(initialOffsetY, Dp.VectorConverter) }

    LaunchedEffect(index) {
        val delayMs = index * staggerDelayMs
        delay(delayMs)

        val springSpecFloat = spring<Float>(
            dampingRatio = 0.84f,
            stiffness = 460f,
        )
        val springSpecDp = spring<Dp>(
            dampingRatio = 0.84f,
            stiffness = 460f,
        )

        // Launch staggered cascade entrance concurrently
        coroutineScope {
            launch {
                alphaAnim.animateTo(1f, animationSpec = springSpecFloat)
            }
            launch {
                offsetYAnim.animateTo(0.dp, animationSpec = springSpecDp)
            }
        }
    }

    this
        .alpha(alphaAnim.value)
        .offset(y = offsetYAnim.value)
}

/**
 * Wrapper composable to apply Nothing OS 5.0 Bento Staggered Entrance to any Composable card.
 */
@Composable
fun NothingBentoCardEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    row: Int = 0,
    col: Int = 0,
    cardSize: BentoCardSize = BentoCardSize.ONE_BY_ONE,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardSize.heightDp())
            .nothingBentoEntrance(
                index = index,
                row = row,
                col = col,
            ),
    ) {
        content()
    }
}

/**
 * Preview Composable demonstrating Nothing Bento Card Staggered Entrance.
 */
@Preview
@Composable
fun NothingBentoStaggeredGridPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NothingBentoCardEntrance(
            index = 0,
            row = 0,
            col = 0,
            cardSize = BentoCardSize.TWO_BY_ONE,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "BENTO 2x1 CARD",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }

        NothingBentoCardEntrance(
            index = 1,
            row = 1,
            col = 0,
            cardSize = BentoCardSize.TWO_BY_TWO,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "BENTO 2x2 CARD",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
    }
}