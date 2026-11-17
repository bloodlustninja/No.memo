package com.example.nomemo.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nomemo.data.entity.Note
import com.example.nomemo.ui.anim.NothingAudioFeedback
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class GraphNode(
    val note: Note,
    val position: Offset,
    val radius: Float = 24f,
)

/**
 * Nothing OS 5.0 2D "Bento Knowledge Graph" Studio.
 *
 * Visualizes the interconnected neural graph of thoughts:
 * - 2D infinite zoom & pan canvas
 * - Glowing laser vector connections between notes sharing tags (#tags)
 * - Tactile node selection and zero-friction note expansion
 */
@Composable
fun BentoKnowledgeGraphScreen(
    notes: List<Note>,
    onOpenNote: (Note) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    // Position nodes organically in a circular constellation
    val nodes = remember(notes) {
        val total = notes.size
        if (total == 0) emptyList()
        else {
            val center = Offset(500f, 600f)
            val ringRadius = 280f
            notes.mapIndexed { index, note ->
                val angle = (index.toFloat() / total.toFloat()) * 2f * Math.PI
                val r = if (index % 2 == 0) ringRadius else ringRadius * 0.65f
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                GraphNode(note = note, position = Offset(x, y))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding(),
    ) {
        // Skia GPU Dot Matrix Canvas Background
        NothingDotMatrixBackground(modifier = Modifier.fillMaxSize())

        // 2D Pan / Zoom Graph Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(nodes, scale, panOffset) {
                    detectTapGestures { tapOffset ->
                        // Transform tap coordinate to graph space
                        val graphX = (tapOffset.x - panOffset.x) / scale
                        val graphY = (tapOffset.y - panOffset.y) / scale
                        val tapInGraph = Offset(graphX, graphY)

                        val hit = nodes.firstOrNull { node ->
                            hypot(node.position.x - tapInGraph.x, node.position.y - tapInGraph.y) <= (node.radius * 2f)
                        }

                        if (hit != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NothingAudioFeedback.playTactileClick()
                            selectedNote = hit.note
                        } else {
                            selectedNote = null
                        }
                    }
                },
        ) {
            // Draw Edges (Laser connections for shared tags)
            for (i in 0 until nodes.size) {
                for (j in i + 1 until nodes.size) {
                    val n1 = nodes[i]
                    val n2 = nodes[j]
                    val commonTags = n1.note.tags.intersect(n2.note.tags.toSet())

                    if (commonTags.isNotEmpty()) {
                        val isHighlighted = selectedNote?.id == n1.note.id || selectedNote?.id == n2.note.id
                        val lineColor = if (isHighlighted) Color(0xFFD71921) else Color(0x33FFFFFF)
                        val strokeWidth = if (isHighlighted) 2.5.dp.toPx() * scale else 1.dp.toPx() * scale

                        val start = Offset(
                            n1.position.x * scale + panOffset.x,
                            n1.position.y * scale + panOffset.y,
                        )
                        val end = Offset(
                            n2.position.x * scale + panOffset.x,
                            n2.position.y * scale + panOffset.y,
                        )
                        drawLine(lineColor, start, end, strokeWidth)
                    }
                }
            }

            // Draw Nodes
            for (node in nodes) {
                val isSelected = selectedNote?.id == node.note.id
                val screenPos = Offset(
                    node.position.x * scale + panOffset.x,
                    node.position.y * scale + panOffset.y,
                )
                val nodeRadius = (if (isSelected) 18.dp.toPx() else 12.dp.toPx()) * scale

                // Outer Ring
                drawCircle(
                    color = if (isSelected) Color(0xFFD71921) else Color(0x66FFFFFF),
                    radius = nodeRadius,
                    center = screenPos,
                    style = Stroke(width = 2.dp.toPx() * scale),
                )

                // Inner Dot
                drawCircle(
                    color = if (isSelected) Color(0xFFD71921) else Color.White,
                    radius = nodeRadius * 0.45f,
                    center = screenPos,
                )
            }
        }

        // Top HUD Header
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NothingAudioFeedback.playTactileClick()
                            onBackClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.width(10.dp))

                KineticDotMatrixText(
                    text = "BENTO // GRAPH",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                )
            }

            Text(
                text = "${notes.size} NODES // 2D SPACE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF8E8E93),
            )
        }

        // Bottom Selected Node Preview Bento Card
        if (selectedNote != null) {
            val sel = selectedNote!!
            FrostedBentoCard(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = sel.title.ifBlank { "UNTITLED" }.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )

                        // Open Note Action Capsule
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD71921))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    NothingAudioFeedback.playTactileClick()
                                    onOpenNote(sel)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "OPEN →",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    if (sel.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sel.body,
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            maxLines = 2,
                        )
                    }

                    if (sel.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            sel.tags.forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFFD71921),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
