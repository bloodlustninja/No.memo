package com.example.nomemo.ui.editor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
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
            var activeNote by remember { mutableStateOf<Note?>(null) }
            var isLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(noteId) {
                if (noteId > 0L) {
                    val loaded = repository.getNoteById(noteId)
                    if (loaded != null) {
                        activeNote = loaded
                    } else {
                        activeNote = Note(
                            id = noteId,
                            title = initialTitle,
                            body = initialBody,
                            type = initialType,
                            imageUris = if (!initialImageUri.isNullOrBlank()) listOf(initialImageUri) else emptyList(),
                            imageUri = initialImageUri,
                            timestamp = System.currentTimeMillis(),
                        )
                    }
                } else {
                    activeNote = Note(
                        id = 0L,
                        title = initialTitle,
                        body = initialBody,
                        type = initialType,
                        imageUris = if (!initialImageUri.isNullOrBlank()) listOf(initialImageUri) else emptyList(),
                        imageUri = initialImageUri,
                        timestamp = System.currentTimeMillis(),
                    )
                }
                isLoaded = true
            }

            if (!isLoaded || activeNote == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor(0xFF000000)),
                )
                return@setContent
            }

            val note = activeNote!!
            NothingNoteEditorScreen(
                initialNote = note,
                onSaveNote = { noteToSave ->
                    lifecycleScope.launch {
                        val savedId = repository.insertNote(noteToSave)
                        if (activeNote?.id == 0L && savedId > 0L) {
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
