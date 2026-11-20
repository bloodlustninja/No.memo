package com.example.nomemo

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.ui.compose.MainNotesScreen
import com.example.nomemo.ui.editor.NoteEditorActivity
import com.example.nomemo.ui.settings.SettingsActivity
import com.example.nomemo.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var prefsManager: PreferencesManager
    private val hudMessage = MutableStateFlow<String?>(null)

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val persistentUri = try {
                    com.example.nomemo.media.InternalMediaManager.persistImageToInternalStorage(this@MainActivity, it)
                } catch (_: Exception) {
                    it.toString()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    openEditor(type = NoteType.MEDIA, initialImageUri = persistentUri)
                }
            }
        }
    }

    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { treeUri ->
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: Exception) {}

            prefsManager.markdownMirrorUri = treeUri.toString()
            Toast.makeText(this, "MARKDOWN FOLDER MIRROR ACTIVE", Toast.LENGTH_SHORT).show()
            com.example.nomemo.mirror.MarkdownMirrorManager.mirrorAllNotes(this, viewModel.notesList.value)
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
        handleIncomingIntent(intent)

        setContent {
            val notes by viewModel.notesList.collectAsState()
            val boardNotes by viewModel.boardNotes.collectAsState()
            val archivedNotes by viewModel.archivedNotes.collectAsState()
            val availableTags by viewModel.availableTags.collectAsState()
            val activeTag by viewModel.activeTagFilter.collectAsState()
            val hudMsg by hudMessage.collectAsState()

            MainNotesScreen(
                notesList = notes,
                boardNotes = boardNotes,
                hudMessage = hudMsg,
                archivedNotes = archivedNotes,
                availableTags = availableTags,
                activeTag = activeTag,
                onNoteClick = { note ->
                    openEditor(
                        type = note.type,
                        noteId = note.id,
                        initialTitle = note.title,
                        initialBody = note.body,
                        initialImageUri = note.imageUri ?: note.imageUris.firstOrNull(),
                    )
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
                onMirrorFolderClick = {
                    openDocumentTreeLauncher.launch(null)
                },
                onUpdateNotePosition = { noteId, x, y ->
                    viewModel.updateNotePosition(noteId, x, y)
                },
                onLinkNotes = { sourceId, targetId ->
                    viewModel.linkNotes(sourceId, targetId)
                },
                onUnlinkNotes = { id1, id2 ->
                    viewModel.unlinkNotes(id1, id2)
                },
                onCreateBoardCard = { title, body ->
                    viewModel.createBoardTextCard(title, body)
                    showHudNotification("CARD PINNED // BOARD")
                    Toast.makeText(this, "PINNED TO BOARD", Toast.LENGTH_SHORT).show()
                },
                onCreateBoardPhoto = { uri ->
                    viewModel.createBoardPhotoCard(uri.toString())
                    showHudNotification("PHOTO PINNED // BOARD")
                    Toast.makeText(this, "PHOTO PINNED TO BOARD", Toast.LENGTH_SHORT).show()
                },
                onToggleNoteOnBoard = { id, onBoard ->
                    viewModel.toggleNoteOnBoard(id, onBoard)
                },
                onRemoveFromBoard = { id ->
                    viewModel.removeNoteFromBoard(id)
                    showHudNotification("REMOVED FROM BOARD")
                    Toast.makeText(this, "REMOVED FROM BOARD", Toast.LENGTH_SHORT).show()
                },
                onPermanentDeleteBoardNote = { note ->
                    viewModel.permanentlyDeleteBoardNote(note)
                    showHudNotification("NOTE SHREDDED")
                    Toast.makeText(this, "BOARD ITEM PERMANENTLY DELETED", Toast.LENGTH_SHORT).show()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun showHudNotification(msg: String) {
        lifecycleScope.launch {
            hudMessage.value = msg
            delay(2400)
            if (hudMessage.value == msg) {
                hudMessage.value = null
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == com.example.nomemo.widget.NothingBentoWidgetProvider.ACTION_VOICE_CAPTURE) {
            intent.action = null
            setIntent(intent)
            openEditor(type = NoteType.AUDIO)
            return
        }

        if (action == Intent.ACTION_SEND) {
            val mimeType = intent.type ?: ""
            val targetNoteId = intent.getLongExtra(com.example.nomemo.share.DirectShareManager.EXTRA_TARGET_NOTE_ID, -1L)
            val extraText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val extraSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

            // Handle image/* or EXTRA_STREAM
            val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }

            if (imageUri != null || mimeType.startsWith("image/")) {
                val uriToPersist = imageUri ?: intent.data
                if (uriToPersist != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uriToPersist,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: Exception) {}

                    viewModel.persistSharedMedia(
                        uri = uriToPersist.toString(),
                        caption = extraText.ifBlank { extraSubject },
                        targetNoteId = targetNoteId,
                    )
                    showHudNotification("SHARED MEDIA PERSISTED")
                    Toast.makeText(this, "SHARED MEDIA PERSISTED", Toast.LENGTH_SHORT).show()
                }
            } else if (extraText.isNotBlank() || extraSubject.isNotBlank()) {
                viewModel.persistSharedText(targetNoteId = targetNoteId, subject = extraSubject, text = extraText)
                showHudNotification("SHARED LINK PERSISTED")
                Toast.makeText(this, "SHARED LINK PERSISTED", Toast.LENGTH_SHORT).show()
            }

            // Clear intent action so it never disappears when opened or duplicates on rotation
            intent.action = null
            setIntent(intent)
        }
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
