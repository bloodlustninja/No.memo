package com.example.nomemo.ui.editor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.data.repository.NoteRepository
import com.example.nomemo.ui.compose.NothingNoteEditorScreen
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var repository: NoteRepository
    private lateinit var prefsManager: PreferencesManager

    companion object {
        const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"
        const val EXTRA_NOTE_TYPE = "EXTRA_NOTE_TYPE"
        const val EXTRA_ACTION_PICK_IMAGE = "EXTRA_ACTION_PICK_IMAGE"
        const val EXTRA_INITIAL_IMAGE_URI = "EXTRA_INITIAL_IMAGE_URI"
        const val EXTRA_INITIAL_TITLE = "EXTRA_INITIAL_TITLE"
        const val EXTRA_INITIAL_BODY = "EXTRA_INITIAL_BODY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefsManager = PreferencesManager(this)
        if (prefsManager.isPitchBlackEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        super.onCreate(savedInstanceState)

        repository = NoteRepository(AppDatabase.getDatabase(applicationContext).noteDao())

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        val initialType = intent.getStringExtra(EXTRA_NOTE_TYPE) ?: NoteType.NOTE
        val initialTitle = intent.getStringExtra(EXTRA_INITIAL_TITLE).orEmpty()
        val initialBody = intent.getStringExtra(EXTRA_INITIAL_BODY).orEmpty()
        val initialImageUri = intent.getStringExtra(EXTRA_INITIAL_IMAGE_URI)

        setContent {
            var activeNote by remember {
                mutableStateOf(
                    Note(
                        id = if (noteId > 0) noteId else 0L,
                        title = initialTitle,
                        body = initialBody,
                        type = initialType,
                        imageUris = if (!initialImageUri.isNullOrBlank()) listOf(initialImageUri) else emptyList(),
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }

            LaunchedEffect(noteId) {
                if (noteId > 0L) {
                    repository.getNoteById(noteId)?.let { loaded ->
                        activeNote = loaded
                    }
                }
            }

            NothingNoteEditorScreen(
                initialNote = activeNote,
                onSaveNote = { noteToSave ->
                    lifecycleScope.launch {
                        val savedId = repository.insertNote(noteToSave)
                        if (activeNote.id == 0L && savedId > 0L) {
                            activeNote = noteToSave.copy(id = savedId)
                        }
                    }
                },
                onDeleteNote = { idToDelete ->
                    lifecycleScope.launch {
                        repository.deleteNoteById(idToDelete)
                    }
                },
                onBackClick = {
                    finish()
                },
                isDarkTheme = prefsManager.isPitchBlackEnabled,
            )
        }
    }
}
