package com.example.nomemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.databinding.ActivityMainBinding
import com.example.nomemo.ui.adapter.NoteBentoAdapter
import com.example.nomemo.ui.anim.RubberBandAnimation
import com.example.nomemo.ui.compose.NothingAppTheme
import com.example.nomemo.ui.compose.NothingDotMatrixBackground
import com.example.nomemo.ui.compose.NothingVoiceOverlay
import com.example.nomemo.ui.editor.NoteEditorActivity
import com.example.nomemo.ui.settings.SettingsActivity
import com.example.nomemo.ui.viewmodel.MainViewModel
import com.example.nomemo.voice.VoiceToTextManager
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: NoteBentoAdapter
    private lateinit var prefsManager: PreferencesManager
    private lateinit var voiceManager: VoiceToTextManager

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        } else {
            Toast.makeText(this, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        voiceManager = VoiceToTextManager(this)

        setupDotMatrixBackground()
        setupVoiceOverlay()
        setupRecyclerView()
        setupFilterPills()
        setupSearchBar()
        setupHeaderButtons()
        setupFloatingDock()
        setupThrowToDelete()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        applyThemeFromPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.stopListening()
    }

    private fun handleVoiceDockClick() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                voiceManager.startListening()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun applyThemeFromPreferences() {
        if (prefsManager.isPitchBlackEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupDotMatrixBackground() {
        binding.composeDotMatrixBg.setContent {
            NothingAppTheme(isDarkTheme = prefsManager.isPitchBlackEnabled) {
                NothingDotMatrixBackground(modifier = Modifier.fillMaxSize())
            }
        }
    }

    private fun setupVoiceOverlay() {
        binding.composeVoiceOverlay.setContent {
            val isListening by voiceManager.isListening.collectAsState()
            val liveText by voiceManager.spokenText.collectAsState()
            val volume by voiceManager.rmsVolume.collectAsState()

            NothingAppTheme(isDarkTheme = prefsManager.isPitchBlackEnabled) {
                NothingVoiceOverlay(
                    isListening = isListening,
                    spokenText = liveText,
                    rmsVolume = volume,
                    onSaveClick = { parsedNote ->
                        voiceManager.stopListening()
                        voiceManager.clearText()
                        openEditor(
                            type = NoteType.NOTE,
                            initialTitle = parsedNote.title,
                            initialBody = parsedNote.body,
                        )
                    },
                    onDismissClick = {
                        voiceManager.stopListening()
                        voiceManager.clearText()
                    },
                )
            }
        }
    }

    private fun performHaptic(view: View) {
        if (prefsManager.isHapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteBentoAdapter(
            onItemClick = { note ->
                openEditor(type = note.type, noteId = note.id)
            },
            onItemLongClick = { note ->
                adapter.toggleSelection(note.id)
                performHaptic(binding.root)
                true
            },
        )

        val staggeredLayoutManager = StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL,
        ).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

        binding.rvNotes.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            layoutManager = staggeredLayoutManager
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupThrowToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        ) {
            private val deleteBackground = ColorDrawable(Color.parseColor("#40D71921"))
            private val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
            private val hasVibratedForPosition = mutableSetOf<Int>()

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                    val noteToDelete = adapter.currentList[position]
                    viewModel.deleteNote(noteToDelete)
                    Toast.makeText(this@MainActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                val itemView = viewHolder.itemView
                val totalDistance = abs(dX)
                val dismissThreshold = 120f

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    itemView.rotation = (dX / 18f).coerceIn(-24f, 24f)
                    itemView.alpha = (1f - (totalDistance / (itemView.width * 0.8f))).coerceIn(0.2f, 1f)

                    val pos = viewHolder.adapterPosition
                    if (totalDistance >= dismissThreshold && !hasVibratedForPosition.contains(pos)) {
                        performHaptic(itemView)
                        hasVibratedForPosition.add(pos)
                    } else if (totalDistance < dismissThreshold) {
                        hasVibratedForPosition.remove(pos)
                    }

                    if (dX < 0) {
                        deleteBackground.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom,
                        )
                    } else {
                        deleteBackground.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom,
                        )
                    }
                    deleteBackground.draw(c)

                    deleteIcon?.let { icon ->
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight

                        if (dX < 0) {
                            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                            val iconRight = itemView.right - iconMargin
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        } else {
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        }
                        icon.draw(c)
                    }
                } else {
                    itemView.rotation = 0f
                    itemView.alpha = 1f
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.rotation = 0f
                viewHolder.itemView.alpha = 1f
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvNotes)
    }

    private fun setupFilterPills() {
        val pills = mapOf(
            "ALL" to binding.pillAll,
            "NOTES" to binding.pillNotes,
            "MEDIA" to binding.pillMedia,
            "CHECKLISTS" to binding.pillChecklists,
        )

        pills.forEach { (filterKey, textView) ->
            RubberBandAnimation.applyRubberBandTouchEffect(textView)
            textView.setOnClickListener {
                performHaptic(textView)
                RubberBandAnimation.animateRubberBandClick(textView) {
                    viewModel.setFilter(filterKey)
                }
            }
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun setupHeaderButtons() {
        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnSearch)
        binding.btnSearch.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                val isVisible = binding.cvSearchBar.visibility == View.VISIBLE
                binding.cvSearchBar.visibility = if (isVisible) View.GONE else View.VISIBLE
                if (isVisible) {
                    binding.etSearch.text?.clear()
                    viewModel.setSearchQuery("")
                } else {
                    binding.etSearch.requestFocus()
                }
            }
        }

        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnSettings)
        binding.btnSettings.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    private fun setupFloatingDock() {
        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnDockText)
        binding.btnDockText.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                openEditor(type = NoteType.NOTE)
            }
        }

        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnDockChecklist)
        binding.btnDockChecklist.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                openEditor(type = NoteType.CHECKLIST)
            }
        }

        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnDockImage)
        binding.btnDockImage.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                openEditor(type = NoteType.MEDIA, pickImage = true)
            }
        }

        RubberBandAnimation.applyRubberBandTouchEffect(binding.btnDockVoice)
        binding.btnDockVoice.setOnClickListener {
            performHaptic(it)
            RubberBandAnimation.animateRubberBandClick(it) {
                handleVoiceDockClick()
            }
        }
    }

    private fun openEditor(
        type: String,
        noteId: Long = -1L,
        pickImage: Boolean = false,
        initialTitle: String = "",
        initialBody: String = "",
    ) {
        val intent = Intent(this, NoteEditorActivity::class.java).apply {
            putExtra(NoteEditorActivity.EXTRA_NOTE_TYPE, type)
            putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId)
            putExtra(NoteEditorActivity.EXTRA_ACTION_PICK_IMAGE, pickImage)
            if (initialTitle.isNotBlank()) {
                putExtra(NoteEditorActivity.EXTRA_INITIAL_TITLE, initialTitle)
            }
            if (initialBody.isNotBlank()) {
                putExtra(NoteEditorActivity.EXTRA_INITIAL_BODY, initialBody)
            }
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notesList.collect { notes ->
                        val newList = ArrayList(notes)
                        adapter.submitList(newList) {
                            (binding.rvNotes.layoutManager as? StaggeredGridLayoutManager)?.invalidateSpanAssignments()
                            binding.rvNotes.post {
                                binding.rvNotes.scrollToPosition(0)
                            }
                        }
                        binding.layoutEmptyState.visibility =
                            if (newList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.activeFilter.collect { activeFilter ->
                        updateFilterPillsUi(activeFilter)
                    }
                }
            }
        }
    }

    private fun updateFilterPillsUi(activeFilter: String) {
        val filterMap = mapOf(
            "ALL" to binding.pillAll,
            "NOTES" to binding.pillNotes,
            "MEDIA" to binding.pillMedia,
            "CHECKLISTS" to binding.pillChecklists,
        )

        filterMap.forEach { (key, pillView) ->
            if (key.equals(activeFilter, ignoreCase = true)) {
                pillView.setBackgroundResource(R.drawable.bg_pill_selected)
                pillView.alpha = 1.0f
            } else {
                pillView.setBackgroundResource(R.drawable.bg_pill_unselected)
                pillView.alpha = 0.4f
            }
        }
    }
}