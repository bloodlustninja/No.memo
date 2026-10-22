package com.example.nomemo.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal providing whether the app is currently rendering in Pitch Black or Light Mode.
 */
val LocalIsDarkTheme = compositionLocalOf { true }

/**
 * Nothing OS 5.0 Dynamic Light / Dark Theme Color Palette.
 */
object NothingThemeColors {
    val background: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF000000) else Color(0xFFEBEBEF)

    val surface: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF141416) else Color(0xFFF5F5F8)

    val surfaceBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0x33FFFFFF) else Color(0x33000000)

    val textPrimary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF121214)

    val textSecondary: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8E8E93) else Color(0xFF727278)

    val textMuted: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF666666) else Color(0xFF999999)

    val tabTrackBackground: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0C0C0C) else Color(0xFFE0E0E6)

    val tabTrackBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF222222) else Color(0xFFCCCCCC)

    val dockBackground: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0xE61C1C1E) else Color(0xE6F5F5F8)

    val dockBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current) Color(0x4DFFFFFF) else Color(0x33000000)

    val accentCrimson = Color(0xFFD71921)
}

/**
 * Immutable Note Item for 120 FPS Recomposition Stability.
 */
@Immutable
data class ImmutableNoteItem(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val type: String = "NOTE",
)

/**
 * No-Op Indication to kill muddy Material ink ripples.
 */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : DelegatableNode, Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Hardware Vignette Edge Fade Modifier (Top 16dp and Bottom 24dp vertical gradients).
 */
fun Modifier.edgeVignette(
    topFade: Dp = 16.dp,
    bottomFade: Dp = 24.dp,
): Modifier = this
    .graphicsLayer { alpha = 0.99f } // Required for BlendMode.DstIn
    .drawWithContent {
        drawContent()
        // Top edge fade
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = topFade.toPx(),
            ),
            blendMode = BlendMode.DstIn,
        )
        // Bottom edge fade (above floating bar)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - bottomFade.toPx(),
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

/**
 * Root Nothing OS 5.0 Theme Provider.
 *
 * 1. Provides LocalIsDarkTheme context.
 * 2. Kills default muddy Material ink ripples across the board.
 * 3. Kills organic gummy stretch overscroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NothingAppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalIsDarkTheme provides isDarkTheme,
        LocalIndication provides NoIndication,
        LocalOverscrollConfiguration provides null,
    ) {
        content()
    }
}