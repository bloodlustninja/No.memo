package com.example.nomemo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: NoteRepository = NoteRepository(
        AppDatabase.getDatabase(application).noteDao(),
    ),
) : AndroidViewModel(application) {

    val activeFilter = MutableStateFlow("ALL")
    val searchQuery = MutableStateFlow("")
    val activeTagFilter = MutableStateFlow<String?>(null)

    private val allActiveNotes = repository.getAllActiveNotes()

    val archivedNotes: StateFlow<List<Note>> = repository.getArchivedNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    // Extracted dynamic hashtags from all existing notes
    val availableTags: StateFlow<List<String>> = allActiveNotes.map { notes ->
        notes.flatMap { it.tags }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val notesList: StateFlow<List<Note>> = combine(
        allActiveNotes,
        activeFilter,
        searchQuery,
        activeTagFilter,
    ) { notes, filter, query, tagFilter ->
        var list = notes

        // 1. Tag filter
        if (!tagFilter.isNullOrBlank()) {
            list = list.filter { it.tags.contains(tagFilter) }
        }

        // 2. Search query filter
        if (query.isNotBlank()) {
            val q = query.trim()
            list = list.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.body.contains(q, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
            }
        }

        // 3. Category Tab filter
        val filtered = when (filter) {
            "NOTES", NoteType.NOTE -> list.filter { it.type == NoteType.NOTE }
            "CHECKLISTS", NoteType.CHECKLIST -> list.filter { it.type == NoteType.CHECKLIST }
            "MEDIA" -> list.filter { it.type == NoteType.MEDIA }
            "AUDIO", NoteType.AUDIO -> list.filter { it.type == NoteType.AUDIO }
            else -> list
        }
        filtered.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    fun setFilter(filter: String) {
        activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setTagFilter(tag: String?) {
        activeTagFilter.value = if (activeTagFilter.value == tag) null else tag
    }

    fun insertNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.togglePinStatus(note)
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.toggleArchiveStatus(note)
        }
    }

    fun toggleLock(note: Note) {
        viewModelScope.launch {
            repository.toggleLockStatus(note)
        }
    }

    fun toggleChecklistItem(note: Note, itemId: String) {
        viewModelScope.launch {
            repository.toggleChecklistItem(note, itemId)
        }
    }
}
