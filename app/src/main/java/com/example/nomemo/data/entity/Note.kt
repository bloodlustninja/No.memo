package com.example.nomemo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nomemo.data.model.ChecklistItem

object NoteType {
    const val NOTE = "NOTE"
    const val CHECKLIST = "CHECKLIST"
    const val MEDIA = "MEDIA"
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val type: String = NoteType.NOTE,
    val imageUris: List<String> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList()
)