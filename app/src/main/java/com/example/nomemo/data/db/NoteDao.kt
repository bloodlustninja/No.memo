package com.example.nomemo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nomemo.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND boardOnly = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND boardOnly = 0 AND type = :type ORDER BY isPinned DESC, timestamp DESC")
    fun getNotesByType(type: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND boardOnly = 0 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%') ORDER BY isPinned DESC, timestamp DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isOnBoard = 1 ORDER BY timestamp DESC")
    fun getBoardNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long): Int
}
