package com.example.nomemo.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.db.NoteDao
import com.example.nomemo.data.entity.Note
import com.example.nomemo.media.InternalMediaManager
import com.example.nomemo.mirror.MarkdownMirrorManager
import com.example.nomemo.preview.LinkPreviewResolver
import com.example.nomemo.widget.NothingBentoGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteRepository(
    private val noteDao: NoteDao,
    private val context: Context? = null,
) {

    private fun triggerAsyncLinkPreview(note: Note) {
        if (note.linkThumbnailUrl != null) return
        val rawText = "${note.title} ${note.body}"
        val url = LinkPreviewResolver.extractFirstUrl(rawText) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preview = LinkPreviewResolver.resolve(url)
                if (preview != null && (!preview.thumbnailUrl.isNullOrBlank() || !preview.title.isNullOrBlank())) {
                    val current = noteDao.getNoteById(note.id) ?: return@launch
                    val updated = current.copy(
                        linkTitle = current.linkTitle ?: preview.title,
                        linkThumbnailUrl = current.linkThumbnailUrl ?: preview.thumbnailUrl,
                    )
                    noteDao.updateNote(updated)
                    syncWidgetsAndMirror(updated)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun syncWidgetsAndMirror(note: Note? = null, isDelete: Boolean = false) {
        val ctx = context ?: AppDatabase.appContext
        if (ctx != null) {
            try {
                NothingBentoGlanceWidget().updateAll(ctx)
            } catch (_: Exception) {}

            if (note != null) {
                if (isDelete) {
                    MarkdownMirrorManager.deleteNoteFile(ctx, note)
                    InternalMediaManager.deleteInternalImage(ctx, note.imageUri)
                    note.imageUris.forEach { InternalMediaManager.deleteInternalImage(ctx, it) }
                } else {
                    MarkdownMirrorManager.syncNote(ctx, note)
                }
            }
        }
    }

    fun getAllActiveNotes(): Flow<List<Note>> = noteDao.getAllActiveNotes()

    fun getBoardNotes(): Flow<List<Note>> = noteDao.getBoardNotes()

    fun getNotesByType(type: String): Flow<List<Note>> = noteDao.getNotesByType(type)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    suspend fun insertNote(note: Note): Long {
        val id = noteDao.insertNote(note)
        val inserted = note.copy(id = id)
        syncWidgetsAndMirror(inserted)
        triggerAsyncLinkPreview(inserted)
        return id
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        syncWidgetsAndMirror(note)
        triggerAsyncLinkPreview(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        syncWidgetsAndMirror(note, isDelete = true)
    }

    suspend fun deleteNoteById(id: Long) {
        val note = noteDao.getNoteById(id)
        noteDao.deleteNoteById(id)
        if (note != null) {
            syncWidgetsAndMirror(note, isDelete = true)
        } else {
            syncWidgetsAndMirror()
        }
    }

    suspend fun togglePinStatus(note: Note) {
        val updated = note.copy(isPinned = !note.isPinned)
        noteDao.updateNote(updated)
        syncWidgetsAndMirror(updated)
    }

    suspend fun toggleArchiveStatus(note: Note) {
        val updated = note.copy(isArchived = !note.isArchived)
        noteDao.updateNote(updated)
        syncWidgetsAndMirror(updated)
    }

    suspend fun toggleLockStatus(note: Note) {
        val updated = note.copy(isLocked = !note.isLocked)
        noteDao.updateNote(updated)
        syncWidgetsAndMirror(updated)
    }

    suspend fun toggleChecklistItem(note: Note, itemId: String) {
        val updatedItems = note.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        val updated = note.copy(checklistItems = updatedItems)
        noteDao.updateNote(updated)
        syncWidgetsAndMirror(updated)
    }
}
