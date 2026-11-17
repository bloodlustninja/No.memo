package com.example.nomemo.data.repository

import com.example.nomemo.data.db.NoteDao
import com.example.nomemo.data.entity.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getAllActiveNotes(): Flow<List<Note>> = noteDao.getAllActiveNotes()

    fun getNotesByType(type: String): Flow<List<Note>> = noteDao.getNotesByType(type)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun togglePinStatus(note: Note) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned))
    }

    suspend fun toggleArchiveStatus(note: Note) {
        noteDao.updateNote(note.copy(isArchived = !note.isArchived))
    }

    suspend fun toggleLockStatus(note: Note) {
        noteDao.updateNote(note.copy(isLocked = !note.isLocked))
    }

    suspend fun toggleChecklistItem(note: Note, itemId: String) {
        val updatedItems = note.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        noteDao.updateNote(note.copy(checklistItems = updatedItems))
    }
}
