package com.example.nomemo.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.nomemo.MainActivity
import com.example.nomemo.R
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.ledger.ExpenseParser
import com.example.nomemo.ledger.LedgerAnalysis
import com.example.nomemo.ui.editor.NoteEditorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Data bundle passed to Glance widget composable.
 */
data class BentoWidgetData(
    val notesCount: Int,
    val note: Note?,
    val ledgerAnalysis: LedgerAnalysis?,
)

/**
 * Nothing OS 5.0 Physical "Glyph Matrix" Jetpack Glance AppWidget.
 *
 * Renders a frosted dark bento card directly on the home screen with:
 * - Real-time Room DB sync on Dispatchers.IO (no ViewModel, no Hilt crash)
 * - Safe actionStartActivity to MainActivity
 * - Live interactive checklist checkboxes via suspending ActionCallback
 * - Quick action capsule buttons (+ NOTE, ✓ LIST / + ITEM, 🎙 VOICE)
 */
class NothingBentoGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val allNotes = db.noteDao().getAllActiveNotes().first()
            val pinnedNote = allNotes.firstOrNull { it.isPinned }
            val targetNote = pinnedNote ?: allNotes.firstOrNull()
            val ledger = if (targetNote != null) {
                ExpenseParser.parse("${targetNote.title}\n${targetNote.body}")
            } else null

            BentoWidgetData(
                notesCount = allNotes.size,
                note = targetNote,
                ledgerAnalysis = ledger,
            )
        }

        provideContent {
            GlanceTheme {
                NothingBentoWidgetContent(
                    context = context,
                    data = widgetData,
                )
            }
        }
    }
}

/**
 * Root Composable for the Nothing Bento Glance Widget.
 */
@androidx.compose.runtime.Composable
private fun NothingBentoWidgetContent(
    context: Context,
    data: BentoWidgetData,
) {
    val openMainAction = actionStartActivity<MainActivity>()
    val openNewNoteAction = actionStartActivity<NoteEditorActivity>()
    val openChecklistAction = actionStartActivity<NoteEditorActivity>()
    val openVoiceAction = actionStartActivity(ComponentName(context.packageName, MainActivity::class.java.name))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.bg_widget_dark_bento))
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(openMainAction),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            // Top Telemetry Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "NO.MEMO",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .size(6.dp)
                        .background(Color(0xFFD71921))
                        .cornerRadius(3.dp),
                ) {}
                Spacer(modifier = GlanceModifier.defaultWeight())

                val telemetryText = when {
                    data.ledgerAnalysis != null && data.ledgerAnalysis.isLedgerMode ->
                        "LEDGER // ${ExpenseParser.formatCurrency(data.ledgerAnalysis.totalExpense, data.ledgerAnalysis.currencySymbol)}"
                    data.note?.type == NoteType.CHECKLIST ->
                        "${data.note.checklistItems.count { it.isChecked }}/${data.note.checklistItems.size} DONE // LIST"
                    data.note?.isPinned == true ->
                        "PINNED // NOTE"
                    else ->
                        "${data.notesCount} NOTES // STUDIO"
                }

                Text(
                    text = telemetryText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E8E93)),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Middle Note Card Area
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
            ) {
                if (data.note != null) {
                    Text(
                        text = data.note.title.ifBlank { "UNTITLED" }.uppercase(),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    if (data.note.type == NoteType.CHECKLIST && data.note.checklistItems.isNotEmpty()) {
                        val displayItems = data.note.checklistItems.take(2)
                        displayItems.forEach { item ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.Vertical.CenterVertically,
                            ) {
                                CheckBox(
                                    checked = item.isChecked,
                                    onCheckedChange = actionRunCallback<ToggleChecklistItemActionCallback>(
                                        actionParametersOf(
                                            ToggleChecklistItemActionCallback.NoteIdKey to data.note.id,
                                            ToggleChecklistItemActionCallback.ItemIdKey to item.id,
                                        ),
                                    ),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ColorProvider(Color.White),
                                        uncheckedColor = ColorProvider(Color(0xFF8E8E93)),
                                    ),
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = item.text.ifBlank { "Task item" },
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(if (item.isChecked) Color(0xFF666666) else Color(0xFFCCCCCC)),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                )
                            }
                        }
                    } else if (data.ledgerAnalysis != null && data.ledgerAnalysis.isLedgerMode) {
                        Text(
                            text = data.note.body.ifBlank { "Expense Ledger Active" },
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF8E8E93)),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "TOTAL: ${ExpenseParser.formatCurrency(data.ledgerAnalysis.totalExpense, data.ledgerAnalysis.currencySymbol)}",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(if (data.ledgerAnalysis.totalExpense < 0) Color(0xFFD71921) else Color.White),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    } else {
                        Text(
                            text = data.note.body.ifBlank { "Tap to open and capture thoughts..." },
                            maxLines = 2,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF8E8E93)),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                } else {
                    Text(
                        text = "ZERO NOTES",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap below to capture your first thought...",
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8E8E93)),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Bottom Quick Action Buttons
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                // Button 1: + NOTE
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(30.dp)
                        .background(Color(0xFF222226))
                        .cornerRadius(8.dp)
                        .clickable(openNewNoteAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+ NOTE",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Button 2: ✓ LIST or + ITEM
                val isChecklistNote = data.note?.type == NoteType.CHECKLIST
                val button2Action = if (isChecklistNote && data.note.id > 0) {
                    actionRunCallback<QuickAddChecklistItemActionCallback>(
                        actionParametersOf(
                            QuickAddChecklistItemActionCallback.NoteIdKey to data.note.id,
                        ),
                    )
                } else {
                    openChecklistAction
                }
                val button2Text = if (isChecklistNote) "+ ITEM" else "✓ LIST"

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(30.dp)
                        .background(Color(0xFF222226))
                        .cornerRadius(8.dp)
                        .clickable(button2Action),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = button2Text,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Button 3: 🎙 VOICE
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(30.dp)
                        .background(Color(0xFFD71921))
                        .cornerRadius(8.dp)
                        .clickable(openVoiceAction),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🎙 VOICE",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * ActionCallback that toggles a checklist item's checked state in Room DB
 * and immediately triggers widget update for instantaneous visual feedback.
 */
class ToggleChecklistItemActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val itemId = parameters[ItemIdKey] ?: return

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val note = db.noteDao().getNoteById(noteId) ?: return@withContext
            val updatedItems = note.checklistItems.map { item ->
                if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
            }
            db.noteDao().updateNote(note.copy(checklistItems = updatedItems))
        }

        NothingBentoGlanceWidget().update(context, glanceId)
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<Long>("note_id")
        val ItemIdKey = ActionParameters.Key<String>("item_id")
    }
}

/**
 * ActionCallback that appends a quick checklist item to the active checklist note
 * and immediately triggers widget update for instantaneous visual feedback.
 */
class QuickAddChecklistItemActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val noteId = parameters[NoteIdKey]

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val targetNote = if (noteId != null && noteId > 0) {
                db.noteDao().getNoteById(noteId)
            } else {
                val activeNotes = db.noteDao().getAllActiveNotes().first()
                activeNotes.firstOrNull { it.type == NoteType.CHECKLIST }
            }

            if (targetNote != null) {
                val newItems = targetNote.checklistItems.toMutableList()
                newItems.add(ChecklistItem(text = "New item"))
                db.noteDao().updateNote(targetNote.copy(checklistItems = newItems))
            } else {
                val newNote = Note(
                    title = "QUICK LIST",
                    type = NoteType.CHECKLIST,
                    checklistItems = listOf(ChecklistItem(text = "New item")),
                    timestamp = System.currentTimeMillis(),
                )
                db.noteDao().insertNote(newNote)
            }
        }

        NothingBentoGlanceWidget().update(context, glanceId)
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<Long>("note_id")
    }
}

/**
 * Modern Jetpack Glance Widget Receiver.
 */
class NothingBentoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingBentoGlanceWidget()
}
