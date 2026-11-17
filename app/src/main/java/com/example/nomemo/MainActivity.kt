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
            val availableTags by viewModel.availableTags.collectAsState()
            val activeTag by viewModel.activeTagFilter.collectAsState()

            MainNotesScreen(
                notesList = notes,
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
