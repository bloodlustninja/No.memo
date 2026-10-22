package com.example.nomemo.ui.compose

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nomemo.R
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoCardComposable(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.US) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)).uppercase() }
    val wordCount = remember(note.body) {
        if (note.body.isBlank()) 0 else note.body.trim().split("\\s+".toRegex()).size
    }
    val firstImageUri = remember(note.imageUris) { note.imageUris.firstOrNull() }

    FrostedBentoCard(
        isSelected = isSelected,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column {
            if (!firstImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(firstImageUri),
                    contentDescription = "Note Media Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Title and Pin Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = note.title.ifBlank { "UNTITLED" }.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingThemeColors.textPrimary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = NothingThemeColors.accentCrimson,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (note.body.isNotBlank() && note.type != NoteType.CHECKLIST) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.body,
                        fontSize = 13.sp,
                        color = NothingThemeColors.textSecondary,
                        maxLines = 3,
                    )
                }

                // Checklist Items Preview
                if (note.type == NoteType.CHECKLIST && note.checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column {
                        note.checklistItems.take(3).forEach { item ->
                            Text(
                                text = if (item.isChecked) "✓ ${item.text}" else "• ${item.text}",
                                fontSize = 12.sp,
                                color = if (item.isChecked) NothingThemeColors.textMuted else NothingThemeColors.textSecondary,
                                maxLines = 1,
                            )
                        }
                        if (note.checklistItems.size > 3) {
                            Text(
                                text = "+${note.checklistItems.size - 3} MORE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingThemeColors.accentCrimson,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val footerText = when (note.type) {
                        NoteType.CHECKLIST -> "$formattedDate • ${note.checklistItems.size} ITEMS"
                        else -> "$formattedDate • $wordCount WORDS"
                    }
                    Text(
                        text = footerText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingThemeColors.textMuted,
                    )
                    if (note.type == NoteType.CHECKLIST) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_checklist),
                            contentDescription = "Checklist Icon",
                            tint = NothingThemeColors.textMuted,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}