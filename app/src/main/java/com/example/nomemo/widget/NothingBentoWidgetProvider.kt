package com.example.nomemo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.nomemo.MainActivity
import com.example.nomemo.R
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.ui.editor.NoteEditorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Nothing OS 5.0 Physical "Glyph Matrix" Homescreen Widget Provider.
 *
 * Provides a 4x2 / 2x2 frosted dark bento card directly on the Android homescreen,
 * offering 1-tap quick capture for Notes, Checklists, and Voice Memos with zero friction.
 */
class NothingBentoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val notes = db.noteDao().getAllActiveNotes().first()
                val latestNote = notes.firstOrNull()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_nothing_bento)

                    // Update Note Preview
                    if (latestNote != null) {
                        views.setTextViewText(R.id.tvWidgetNoteTitle, latestNote.title.ifBlank { "UNTITLED NOTE" }.uppercase())
                        val snippet = when (latestNote.type) {
                            NoteType.CHECKLIST -> "${latestNote.checklistItems.size} items • ${latestNote.checklistItems.count { it.isChecked }} completed"
                            NoteType.AUDIO -> "Voice Memo • ${latestNote.body.ifBlank { "Audio recording" }}"
                            else -> latestNote.body.ifBlank { "Tap to open and edit..." }
                        }
                        views.setTextViewText(R.id.tvWidgetNoteSnippet, snippet)
                        views.setTextViewText(R.id.tvWidgetCount, "${notes.size} NOTES // STUDIO")
                    } else {
                        views.setTextViewText(R.id.tvWidgetNoteTitle, "ZERO NOTES")
                        views.setTextViewText(R.id.tvWidgetNoteSnippet, "Tap below to capture your first thought...")
                        views.setTextViewText(R.id.tvWidgetCount, "0 NOTES // EMPTY")
                    }

                    // Intent 1: Tap main card opens MainActivity
                    val mainIntent = Intent(context, MainActivity::class.java)
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.layoutWidgetContent, mainPendingIntent)

                    // Intent 2: New Note
                    val noteIntent = Intent(context, NoteEditorActivity::class.java).apply {
                        putExtra(NoteEditorActivity.EXTRA_NOTE_TYPE, NoteType.NOTE)
                    }
                    val notePendingIntent = PendingIntent.getActivity(
                        context,
                        1,
                        noteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.btnWidgetNewNote, notePendingIntent)

                    // Intent 3: New Checklist
                    val checklistIntent = Intent(context, NoteEditorActivity::class.java).apply {
                        putExtra(NoteEditorActivity.EXTRA_NOTE_TYPE, NoteType.CHECKLIST)
                    }
                    val checklistPendingIntent = PendingIntent.getActivity(
                        context,
                        2,
                        checklistIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.btnWidgetChecklist, checklistPendingIntent)

                    // Intent 4: Voice Note
                    val voiceIntent = Intent(context, MainActivity::class.java).apply {
                        action = ACTION_VOICE_CAPTURE
                    }
                    val voicePendingIntent = PendingIntent.getActivity(
                        context,
                        3,
                        voiceIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.btnWidgetVoice, voicePendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_VOICE_CAPTURE = "com.example.nomemo.ACTION_VOICE_CAPTURE"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, NothingBentoWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, NothingBentoWidgetProvider::class.java),
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
