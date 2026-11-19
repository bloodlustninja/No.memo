package com.example.nomemo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.repository.NoteRepository
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.share.DirectShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    init {
        viewModelScope.launch {
            allActiveNotes.collect { notes ->
                val pinned = notes.filter { it.isPinned }
                DirectShareManager.updatePinnedDirectShareTargets(getApplication(), pinned)
            }
        }
    }

    fun appendSharedTextToNote(targetNoteId: Long, sharedText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = repository.getAllActiveNotes().first()
            val target = if (targetNoteId > 0) {
                notes.firstOrNull { it.id == targetNoteId }
            } else {
                notes.firstOrNull { it.isPinned } ?: notes.firstOrNull()
            }

            if (target != null) {
                if (target.type == NoteType.CHECKLIST) {
                    val updatedItems = target.checklistItems.toMutableList()
                    updatedItems.add(ChecklistItem(text = sharedText))
                    repository.updateNote(target.copy(checklistItems = updatedItems, timestamp = System.currentTimeMillis()))
                } else {
                    val newBody = if (target.body.isBlank()) sharedText else "${target.body}\n$sharedText"
                    repository.updateNote(target.copy(body = newBody, timestamp = System.currentTimeMillis()))
                }
            } else {
                val newNote = Note(
                    title = "SHARED NOTE",
                    body = sharedText,
                    timestamp = System.currentTimeMillis(),
                )
                repository.insertNote(newNote)
            }
        }
    }

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
        val now = System.currentTimeMillis()
        val expiredNotes = notes.filter { it.isBurn && it.burnExpiresAt != null && it.burnExpiresAt <= now }
        if (expiredNotes.isNotEmpty()) {
            viewModelScope.launch {
                expiredNotes.forEach { repository.deleteNote(it) }
            }
        }
        var list = notes.filter { !it.isBurn || it.burnExpiresAt == null || it.burnExpiresAt > now }

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

    fun updateNotePosition(noteId: Long, x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getNoteById(noteId) ?: return@launch
            repository.updateNote(note.copy(boardX = x, boardY = y))
        }
    }

    fun linkNotes(sourceId: Long, targetId: Long) {
        if (sourceId == targetId) return
        viewModelScope.launch(Dispatchers.IO) {
            val source = repository.getNoteById(sourceId) ?: return@launch
            if (!source.linkedNoteIds.contains(targetId)) {
                repository.updateNote(source.copy(linkedNoteIds = source.linkedNoteIds + targetId))
            }
        }
    }

    fun unlinkNotes(id1: Long, id2: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val n1 = repository.getNoteById(id1)
            if (n1 != null && n1.linkedNoteIds.contains(id2)) {
                repository.updateNote(n1.copy(linkedNoteIds = n1.linkedNoteIds - id2))
            }
            val n2 = repository.getNoteById(id2)
            if (n2 != null && n2.linkedNoteIds.contains(id1)) {
                repository.updateNote(n2.copy(linkedNoteIds = n2.linkedNoteIds - id1))
            }
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

    fun bulkArchive(notes: List<Note>, isArchived: Boolean = true) {
        viewModelScope.launch {
            notes.forEach { note ->
                repository.updateNote(note.copy(isArchived = isArchived))
            }
        }
    }

    fun bulkDelete(notes: List<Note>) {
        viewModelScope.launch {
            notes.forEach { note ->
                repository.deleteNote(note)
            }
        }
    }

    fun bulkTogglePin(notes: List<Note>, pin: Boolean) {
        viewModelScope.launch {
            notes.forEach { note ->
                repository.updateNote(note.copy(isPinned = pin))
            }
        }
    }

    fun bulkToggleLock(notes: List<Note>, lock: Boolean) {
        viewModelScope.launch {
            notes.forEach { note ->
                repository.updateNote(note.copy(isLocked = lock))
            }
        }
    }
}
