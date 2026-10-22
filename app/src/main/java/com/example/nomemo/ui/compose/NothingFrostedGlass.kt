package com.example.nomemo.ui.compose

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted Bento Box Card applying background hardware blur and specular rim gradient
 * while keeping all foreground text, images, and icons 100% razor-sharp.
 */
@Composable
fun FrostedBentoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    isSelected: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDark = LocalIsDarkTheme.current
    val tint = if (isDark) {
        Color(0xFF141416).copy(alpha = 0.65f) // 0.65f acrylic dark tint
    } else {
        Color(0xFFF5F5F8).copy(alpha = 0.70f) // 0.70f frosted light tint
    }
    val borderAlpha = if (isSelected) 0.85f else (if (isDark) 0.35f else 0.25f)

    // Specular highlight: Light hits from top-left, fades to bottom-right
    val specularRim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlpha),
            Color.White.copy(alpha = borderAlpha * 0.25f),
            Color.Transparent,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
    ) {
        // LAYER 1: Background Acrylic Tint with Hardware Blur (Behind Content)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    },
                )
                .background(tint, shape),
        )

        // LAYER 2: Specular Rim Border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.2.dp, brush = specularRim, shape = shape),
        )

        // LAYER 3: Razor-Sharp Unblurred Foreground Content
        content()
    }
}