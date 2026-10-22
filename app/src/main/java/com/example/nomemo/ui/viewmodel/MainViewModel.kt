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

    private val allActiveNotes = repository.getAllActiveNotes()

    val notesList: StateFlow<List<Note>> = combine(
        allActiveNotes,
        activeFilter,
        searchQuery,
    ) { notes, filter, query ->
        var list = notes
        if (query.isNotBlank()) {
            val q = query.trim()
            list = list.filter {
                it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true)
            }
        }
        val filtered = when (filter) {
            "NOTES", NoteType.NOTE -> list.filter { it.type == NoteType.NOTE }
            "CHECKLISTS", NoteType.CHECKLIST -> list.filter { it.type == NoteType.CHECKLIST }
            "MEDIA" -> list.filter { it.type == NoteType.MEDIA }
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
}