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
    val borderColor = if (isSelected) NothingThemeColors.iconActive else NothingThemeColors.cardBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NothingThemeColors.cardSurface, shape)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        content()
    }
}