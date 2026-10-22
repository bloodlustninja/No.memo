package com.example.nomemo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.databinding.ItemNoteBentoBinding
import com.example.nomemo.ui.compose.BentoCardComposable
import com.example.nomemo.ui.compose.NothingAppTheme

class NoteBentoAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note) -> Boolean,
) : ListAdapter<Note, NoteBentoAdapter.NoteViewHolder>(NoteDiffCallback) {

    private val selectedNoteIds = mutableSetOf<Long>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    fun toggleSelection(noteId: Long) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.remove(noteId)
        } else {
            selectedNoteIds.add(noteId)
        }
        val pos = currentList.indexOfFirst { it.id == noteId }
        if (pos >= 0) {
            notifyItemChanged(pos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBentoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(val binding: ItemNoteBentoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            val isSelected = selectedNoteIds.contains(note.id)
            val prefs = PreferencesManager(binding.root.context)

            binding.composeFrostedCard.setContent {
                NothingAppTheme(isDarkTheme = prefs.isPitchBlackEnabled) {
                    BentoCardComposable(
                        note = note,
                        isSelected = isSelected,
                        onClick = { onItemClick(note) },
                        onLongClick = { onItemLongClick(note) },
                    )
                }
            }
        }
    }

    object NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}