package com.example.nomemo.ui.compose

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

data class DoodlePath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
)

/**
 * Nothing OS 5.0 1-Bit "Dot-Matrix Doodle" Canvas.
 *
 * Allows high-speed mechanical sketches and handwritten diagrams in Nothing's
 * iconic monochrome + crimson palette, rendering directly to disk as PNG attachments.
 */
@Composable
fun DotMatrixDoodleSheet(
    onSaveSketch: (filePath: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val paths = remember { mutableStateListOf<DoodlePath>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedStrokeWidth by remember { mutableStateOf(6f) }

    val palette = listOf(
        Color.White,
        Color(0xFFD71921),
        Color(0xFF8E8E93),
    )

    fun exportBitmapAndSave() {
        val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK)

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        for (dPath in paths) {
            if (dPath.points.size < 2) continue
            paint.color = dPath.color.toArgb()
            paint.strokeWidth = dPath.strokeWidth * 1.5f

            for (i in 0 until dPath.points.size - 1) {
                val p1 = dPath.points[i]
                val p2 = dPath.points[i + 1]
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
            }
        }

        val sketchesDir = File(context.filesDir, "sketches").apply { if (!exists()) mkdirs() }
        val sketchFile = File(sketchesDir, "sketch_${System.currentTimeMillis()}.png")

        FileOutputStream(sketchFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        onSaveSketch(sketchFile.absolutePath)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    KineticDotMatrixText(
                        text = "STUDIO // DOODLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Undo
                    Text(
                        text = "UNDO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (paths.isNotEmpty()) Color.White else Color.Gray,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = paths.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                paths.removeLastOrNull()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Clear
                    Text(
                        text = "CLEAR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFD71921),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                paths.clear()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Done / Save
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFD71921))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                exportBitmapAndSave()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("SAVE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Drawing Canvas Area with Dot-Matrix grid backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0x33FFFFFF)),
            ) {
                // Procedural dot matrix grid background
                NothingDotMatrixBackground(modifier = Modifier.fillMaxSize())

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedColor, selectedStrokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = mutableStateListOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        paths.add(
                                            DoodlePath(
                                                points = currentPoints.toList(),
                                                color = selectedColor,
                                                strokeWidth = selectedStrokeWidth,
                                            ),
                                        )
                                    }
                                    currentPoints = mutableStateListOf()
                                },
                            )
                        },
                ) {
                    // Draw committed paths
                    for (dPath in paths) {
                        if (dPath.points.size < 2) continue
                        for (i in 0 until dPath.points.size - 1) {
                            drawLine(
                                color = dPath.color,
                                start = dPath.points[i],
                                end = dPath.points[i + 1],
                                strokeWidth = dPath.strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    // Draw active dragging path
                    if (currentPoints.size > 1) {
                        for (i in 0 until currentPoints.size - 1) {
                            drawLine(
                                color = selectedColor,
                                start = currentPoints[i],
                                end = currentPoints[i + 1],
                                strokeWidth = selectedStrokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            // Bottom Palette & Brush Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Color palette
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { col ->
                        val isSelected = selectedColor == col
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColor = col
                                },
                        )
                    }
                }

                // Stroke width buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3f to "FINE", 6f to "MED", 12f to "BOLD").forEach { (widthVal, label) ->
                        val isSelected = selectedStrokeWidth == widthVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFD71921) else Color(0xFF141416))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedStrokeWidth = widthVal
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
