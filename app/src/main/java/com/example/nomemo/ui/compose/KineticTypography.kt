package com.example.nomemo.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Clean Dot-Matrix Text Component.
 *
 * Renders text cleanly in Nothing OS monospace typography with zero gibberish or character-scrambling artifacts.
 */
@Composable
fun KineticDotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NothingThemeColors.textPrimary,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    letterSpacing: TextUnit = 1.sp,
    maxLines: Int = 1,
    style: TextStyle = TextStyle.Default,
    scrambleDurationMs: Long = 0L,
    enabled: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        style = style,
    )
}

/**
 * Animated Kinetic Telemetry Counter for WPM / WORDS / CHARS.
 * Uses AnimatedContent to vertically roll individual telemetry numbers like a mechanical counter.
 */
@Composable
fun KineticTelemetryValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = NothingThemeColors.textSecondary,
    accentColor: Color = NothingThemeColors.textPrimary,
) {
    Row(modifier = modifier) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "telemetry_roll",
        ) { targetVal ->
            Text(
                text = targetVal,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.5.sp,
            )
        }
        Text(
            text = " $label",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = color,
            letterSpacing = 0.5.sp,
        )
    }
}
