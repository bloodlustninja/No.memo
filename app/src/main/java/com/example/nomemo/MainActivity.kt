package com.example.nomemo

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.ui.compose.MainNotesScreen
import com.example.nomemo.ui.editor.NoteEditorActivity
import com.example.nomemo.ui.settings.SettingsActivity
import com.example.nomemo.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var prefsManager: PreferencesManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {}

            openEditor(type = NoteType.MEDIA, initialImageUri = it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefsManager = PreferencesManager(this)
        applyThemeFromPreferences()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        super.onCreate(savedInstanceState)

        setContent {
            val notes by viewModel.notesList.collectAsState()
            val archivedNotes by viewModel.archivedNotes.collectAsState()
            val availableTags by viewModel.availableTags.collectAsState()
            val activeTag by viewModel.activeTagFilter.collectAsState()

            MainNotesScreen(
                notesList = notes,
                archivedNotes = archivedNotes,
                availableTags = availableTags,
                activeTag = activeTag,
                onNoteClick = { note ->
                    openEditor(type = note.type, noteId = note.id)
                },
                onNoteDelete = { note ->
                    viewModel.deleteNote(note)
                    Toast.makeText(this, "NOTE REMOVED", Toast.LENGTH_SHORT).show()
                },
                onChecklistToggle = { note, itemId ->
                    viewModel.toggleChecklistItem(note, itemId)
                },
                onLockToggle = { note ->
                    viewModel.toggleLock(note)
                    val status = if (note.isLocked) "NOTE UNLOCKED" else "NOTE GLYPH-LOCKED"
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                },
                onTagSelect = { tag ->
                    viewModel.setTagFilter(tag)
                },
                onSearchQueryChange = { query ->
                    viewModel.setSearchQuery(query)
                },
                onRestoreNote = { note ->
                    viewModel.toggleArchive(note)
                    Toast.makeText(this, "NOTE RESTORED FROM VAULT", Toast.LENGTH_SHORT).show()
                },
                onPermanentDelete = { note ->
                    viewModel.deleteNote(note)
                    Toast.makeText(this, "NOTE PERMANENTLY SHREDDED", Toast.LENGTH_SHORT).show()
                },
                onSettingsClick = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                onDockTextClick = {
                    // Handled inside MainNotesScreen via Flash Note overlay
                },
                onDockChecklistClick = {
                    openEditor(type = NoteType.CHECKLIST)
                },
                onDockImageClick = {
                    pickImageLauncher.launch("image/*")
                },
                onDockVoiceClick = {
                    // Handled inside MainNotesScreen voice manager
                },
                onSaveFlashNote = { note ->
                    viewModel.insertNote(note)
                    Toast.makeText(this, "NOTE PERSISTED", Toast.LENGTH_SHORT).show()
                },
                onBulkArchive = { notesToArchive ->
                    viewModel.bulkArchive(notesToArchive)
                    Toast.makeText(this, "${notesToArchive.size} NOTES ARCHIVED", Toast.LENGTH_SHORT).show()
                },
                onBulkDelete = { notesToDelete ->
                    viewModel.bulkDelete(notesToDelete)
                    Toast.makeText(this, "${notesToDelete.size} NOTES SHREDDED", Toast.LENGTH_SHORT).show()
                },
                onBulkPin = { notesToPin, pin ->
                    viewModel.bulkTogglePin(notesToPin, pin)
                    val msg = if (pin) "${notesToPin.size} NOTES PINNED" else "${notesToPin.size} NOTES UNPINNED"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                },
                onBulkLock = { notesToLock, lock ->
                    viewModel.bulkToggleLock(notesToLock, lock)
                    val msg = if (lock) "${notesToLock.size} NOTES LOCKED" else "${notesToLock.size} NOTES UNLOCKED"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                },
                isDarkTheme = prefsManager.isPitchBlackEnabled,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        applyThemeFromPreferences()
    }

    private fun applyThemeFromPreferences() {
        if (prefsManager.isPitchBlackEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun openEditor(
        type: String,
        noteId: Long = -1L,
        initialImageUri: String? = null,
        initialTitle: String = "",
        initialBody: String = "",
    ) {
        val intent = Intent(this, NoteEditorActivity::class.java).apply {
            putExtra(NoteEditorActivity.EXTRA_NOTE_TYPE, type)
            putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
            if (!initialImageUri.isNullOrBlank()) {
                putExtra(NoteEditorActivity.EXTRA_INITIAL_IMAGE_URI, initialImageUri)
            }
            if (initialTitle.isNotBlank()) {
                putExtra(NoteEditorActivity.EXTRA_INITIAL_TITLE, initialTitle)
            }
            if (initialBody.isNotBlank()) {
                putExtra(NoteEditorActivity.EXTRA_INITIAL_BODY, initialBody)
            }
        }
        startActivity(intent)
    }
}
