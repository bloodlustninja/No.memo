package com.example.nomemo.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Kinetic Dot-Matrix Text Scramble Component.
 *
 * Simulates Nothing OS 5.0's hardware dot-matrix display.
 * When text is set or altered, characters rapidly scramble through glyphs
 * ('•', '·', '0', '1', ':', '▓', '▒', '░', 'X') before settling into the destination glyph.
 */
private val MATRIX_GLYPHS = charArrayOf('•', '·', '0', '1', ':', '▓', '▒', '░', 'X', '-', '+', '=')

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
    scrambleDurationMs: Long = 350L,
    enabled: Boolean = true,
) {
    if (!enabled || text.isEmpty()) {
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
        return
    }

    var displayedText by remember(text) { mutableStateOf(text) }

    LaunchedEffect(text) {
        val target = text
        val length = target.length
        if (length == 0) return@LaunchedEffect

        val steps = 8
        val stepDelay = (scrambleDurationMs / steps).coerceAtLeast(20L)

        for (step in 1..steps) {
            val progress = step.toFloat() / steps.toFloat()
            val revealedChars = (length * progress).toInt().coerceIn(0, length)

            val builder = StringBuilder(length)
            for (i in 0 until length) {
                val targetChar = target[i]
                if (targetChar.isWhitespace()) {
                    builder.append(targetChar)
                } else if (i < revealedChars) {
                    builder.append(targetChar)
                } else {
                    val randomGlyph = MATRIX_GLYPHS[Random.nextInt(MATRIX_GLYPHS.size)]
                    builder.append(randomGlyph)
                }
            }
            displayedText = builder.toString()
            delay(stepDelay)
        }
        displayedText = target
    }

    Text(
        text = displayedText,
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
