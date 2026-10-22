package com.example.nomemo.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.data.repository.NoteRepository
import com.example.nomemo.databinding.ActivityNoteEditorBinding
import com.example.nomemo.ui.adapter.AttachedMediaAdapter
import com.example.nomemo.ui.adapter.ChecklistAdapter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.ArrayDeque

@OptIn(FlowPreview::class)
class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repository: NoteRepository
    private lateinit var mediaAdapter: AttachedMediaAdapter
    private lateinit var checklistAdapter: ChecklistAdapter

    private var currentNoteId: Long = 0L
    private var noteType: String = NoteType.NOTE
    private var isPinned: Boolean = false

    private val titleState = MutableStateFlow("")
    private val bodyState = MutableStateFlow("")
    private val imageUrisState = MutableStateFlow<List<String>>(emptyList())
    private val checklistItemsState = MutableStateFlow<List<ChecklistItem>>(emptyList())

    private val undoStack = ArrayDeque<Pair<String, String>>()
    private val redoStack = ArrayDeque<Pair<String, String>>()
    private var isUndoRedoAction = false

    companion object {
        const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"
        const val EXTRA_NOTE_TYPE = "EXTRA_NOTE_TYPE"
        const val EXTRA_ACTION_PICK_IMAGE = "EXTRA_ACTION_PICK_IMAGE"
        const val EXTRA_INITIAL_TITLE = "EXTRA_INITIAL_TITLE"
        const val EXTRA_INITIAL_BODY = "EXTRA_INITIAL_BODY"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) { }

            val updatedList = imageUrisState.value.toMutableList().apply {
                add(it.toString())
            }
            imageUrisState.value = updatedList
            mediaAdapter.submitList(updatedList)
            binding.rvAttachedMedia.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteRepository(AppDatabase.getDatabase(applicationContext).noteDao())

        currentNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (currentNoteId == -1L) currentNoteId = 0L

        noteType = intent.getStringExtra(EXTRA_NOTE_TYPE) ?: NoteType.NOTE
        val pickImage = intent.getBooleanExtra(EXTRA_ACTION_PICK_IMAGE, false)
        val initialTitle = intent.getStringExtra(EXTRA_INITIAL_TITLE)
        val initialBody = intent.getStringExtra(EXTRA_INITIAL_BODY)

        setupMediaRecyclerView()
        setupChecklistRecyclerView()
        setupTopBarListeners()
        setupTextListeners()
        setupAutoSave()

        if (!initialTitle.isNullOrBlank()) {
            binding.etTitle.setText(initialTitle)
            titleState.value = initialTitle
        }

        if (!initialBody.isNullOrBlank()) {
            binding.etBody.setText(initialBody)
            bodyState.value = initialBody
            updateWordAndCharCount(initialTitle.orEmpty(), initialBody)
        }

        if (noteType == NoteType.CHECKLIST) {
            binding.layoutChecklistContainer.visibility = View.VISIBLE
        }

        if (currentNoteId != 0L) {
            loadExistingNote(currentNoteId)
        } else if (pickImage) {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupMediaRecyclerView() {
        mediaAdapter = AttachedMediaAdapter(
            onRemoveImage = { uriToRemove ->
                binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val updatedList = imageUrisState.value.toMutableList().apply {
                    remove(uriToRemove)
                }
                imageUrisState.value = updatedList
                mediaAdapter.submitList(updatedList)
                if (updatedList.isEmpty()) {
                    binding.rvAttachedMedia.visibility = View.GONE
                }
            },
        )

        binding.rvAttachedMedia.layoutManager = GridLayoutManager(this, 2)
        binding.rvAttachedMedia.adapter = mediaAdapter
    }

    private fun setupChecklistRecyclerView() {
        checklistAdapter = ChecklistAdapter(
            onItemChanged = {
                checklistItemsState.value = checklistAdapter.getChecklistItems()
            },
            onItemRemoved = { itemToRemove ->
                val updatedList = checklistItemsState.value.toMutableList().apply {
                    remove(itemToRemove)
                }
                checklistItemsState.value = updatedList
                checklistAdapter.setChecklistItems(updatedList)
            },
        )

        binding.rvChecklist.layoutManager = LinearLayoutManager(this)
        binding.rvChecklist.adapter = checklistAdapter

        binding.btnAddChecklistItem.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val updatedList = checklistItemsState.value.toMutableList().apply {
                add(ChecklistItem(text = ""))
            }
            checklistItemsState.value = updatedList
            checklistAdapter.setChecklistItems(updatedList)
        }
    }

    private fun setupTopBarListeners() {
        binding.btnBack.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            finish()
        }

        binding.btnUndo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            performUndo()
        }

        binding.btnRedo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            performRedo()
        }

        binding.btnPin.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            isPinned = !isPinned
            val pinStatusText = if (isPinned) "Note Pinned" else "Note Unpinned"
            Toast.makeText(this, pinStatusText, Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            deleteNote()
        }
    }

    private fun setupTextListeners() {
        binding.etTitle.doAfterTextChanged { text ->
            val titleText = text?.toString().orEmpty()
            titleState.value = titleText
            updateWordAndCharCount(titleText, bodyState.value)
            pushHistory(titleText, bodyState.value)
        }

        binding.etBody.doAfterTextChanged { text ->
            val bodyText = text?.toString().orEmpty()
            bodyState.value = bodyText
            updateWordAndCharCount(titleState.value, bodyText)
            pushHistory(titleState.value, bodyText)
        }
    }

    private fun setupAutoSave() {
        combine(titleState, bodyState, imageUrisState, checklistItemsState) { title, body, uris, checklists ->
            NoteData(title, body, uris, checklists)
        }
            .debounce(300L)
            .onEach { data ->
                if (data.title.isNotBlank() || data.body.isNotBlank() || data.uris.isNotEmpty() || data.checklists.isNotEmpty()) {
                    saveNote(data.title, data.body, data.uris, data.checklists)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun loadExistingNote(id: Long) {
        lifecycleScope.launch {
            repository.getNoteById(id)?.let { note ->
                isPinned = note.isPinned
                noteType = note.type

                binding.etTitle.setText(note.title)
                binding.etBody.setText(note.body)

                titleState.value = note.title
                bodyState.value = note.body

                if (note.imageUris.isNotEmpty()) {
                    imageUrisState.value = note.imageUris
                    mediaAdapter.submitList(note.imageUris)
                    binding.rvAttachedMedia.visibility = View.VISIBLE
                }

                if (note.checklistItems.isNotEmpty() || noteType == NoteType.CHECKLIST) {
                    binding.layoutChecklistContainer.visibility = View.VISIBLE
                    checklistItemsState.value = note.checklistItems
                    checklistAdapter.setChecklistItems(note.checklistItems)
                }

                updateWordAndCharCount(note.title, note.body)
                pushHistory(note.title, note.body)
            }
        }
    }

    private suspend fun saveNote(title: String, body: String, uris: List<String>, checklists: List<ChecklistItem>) {
        val noteToSave = Note(
            id = currentNoteId,
            title = title,
            body = body,
            type = noteType,
            isPinned = isPinned,
            imageUris = uris,
            checklistItems = checklists,
            timestamp = System.currentTimeMillis(),
        )
        val newId = repository.insertNote(noteToSave)
        if (currentNoteId == 0L && newId > 0L) {
            currentNoteId = newId
        }
    }

    private fun deleteNote() {
        if (currentNoteId != 0L) {
            lifecycleScope.launch {
                repository.deleteNoteById(currentNoteId)
                Toast.makeText(this@NoteEditorActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun updateWordAndCharCount(title: String, body: String) {
        val fullText = "$title $body".trim()
        val charCount = fullText.length
        val wordCount = if (fullText.isBlank()) 0 else fullText.split("\\s+".toRegex()).size
        binding.tvWordCharCount.text = "$wordCount WORDS // $charCount CHARS"
    }

    private fun pushHistory(title: String, body: String) {
        if (isUndoRedoAction) return
        val current = Pair(title, body)
        if (undoStack.isEmpty() || undoStack.peek() != current) {
            undoStack.push(current)
            redoStack.clear()
        }
    }

    private fun performUndo() {
        if (undoStack.size > 1) {
            val current = undoStack.pop()
            redoStack.push(current)
            val prev = undoStack.peek()
            if (prev != null) {
                isUndoRedoAction = true
                binding.etTitle.setText(prev.first)
                binding.etBody.setText(prev.second)
                isUndoRedoAction = false
            }
        }
    }

    private fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.pop()
            undoStack.push(next)
            isUndoRedoAction = true
            binding.etTitle.setText(next.first)
            binding.etBody.setText(next.second)
            isUndoRedoAction = false
        }
    }

    private data class NoteData(
        val title: String,
        val body: String,
        val uris: List<String>,
        val checklists: List<ChecklistItem>,
    )
}