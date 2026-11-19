package com.example.nomemo.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.db.NoteDao
import com.example.nomemo.data.entity.Note
import com.example.nomemo.widget.NothingBentoGlanceWidget
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val context: Context? = null,
) {

    private suspend fun syncWidgets() {
        val ctx = context ?: AppDatabase.appContext
        if (ctx != null) {
            try {
                NothingBentoGlanceWidget().updateAll(ctx)
            } catch (_: Exception) {}
        }
    }

    fun getAllActiveNotes(): Flow<List<Note>> = noteDao.getAllActiveNotes()

    fun getNotesByType(type: String): Flow<List<Note>> = noteDao.getNotesByType(type)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    suspend fun insertNote(note: Note): Long {
        val id = noteDao.insertNote(note)
        syncWidgets()
        return id
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        syncWidgets()
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        syncWidgets()
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
        syncWidgets()
    }

    suspend fun togglePinStatus(note: Note) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned))
        syncWidgets()
    }

    suspend fun toggleArchiveStatus(note: Note) {
        noteDao.updateNote(note.copy(isArchived = !note.isArchived))
        syncWidgets()
    }

    suspend fun toggleLockStatus(note: Note) {
        noteDao.updateNote(note.copy(isLocked = !note.isLocked))
        syncWidgets()
    }

    suspend fun toggleChecklistItem(note: Note, itemId: String) {
        val updatedItems = note.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        noteDao.updateNote(note.copy(checklistItems = updatedItems))
        syncWidgets()
    }
}
