package com.example.nomemo.ui.adapter

import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nomemo.R
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.databinding.ItemChecklistEditorBinding

class ChecklistAdapter(
    private val onItemChanged: () -> Unit,
    private val onItemRemoved: (ChecklistItem) -> Unit
) : ListAdapter<ChecklistItem, ChecklistAdapter.ChecklistViewHolder>(ChecklistDiffCallback) {

    private val itemsList = mutableListOf<ChecklistItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val binding = ItemChecklistEditorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChecklistViewHolder(private val binding: ItemChecklistEditorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem) {
            binding.cbChecklist.setOnCheckedChangeListener(null)
            binding.cbChecklist.isChecked = item.isChecked

            updateItemStyle(item.isChecked)

            binding.cbChecklist.setOnCheckedChangeListener { _, isChecked ->
                binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < itemsList.size) {
                    itemsList[pos] = itemsList[pos].copy(isChecked = isChecked)
                    updateItemStyle(isChecked)
                    onItemChanged()
                }
            }

            binding.etChecklistText.setText(item.text)
            binding.etChecklistText.doAfterTextChanged { text ->
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < itemsList.size) {
                    itemsList[pos] = itemsList[pos].copy(text = text?.toString().orEmpty())
                    onItemChanged()
                }
            }

            binding.btnRemoveChecklistItem.setOnClickListener {
                binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onItemRemoved(item)
            }
        }

        private fun updateItemStyle(isChecked: Boolean) {
            val context = binding.root.context
            if (isChecked) {
                binding.etChecklistText.paintFlags =
                    binding.etChecklistText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.etChecklistText.setTextColor(
                    ContextCompat.getColor(context, R.color.nos_text_secondary)
                )
            } else {
                binding.etChecklistText.paintFlags =
                    binding.etChecklistText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.etChecklistText.setTextColor(
                    ContextCompat.getColor(context, R.color.nos_text_primary)
                )
            }
        }
    }

    fun setChecklistItems(items: List<ChecklistItem>) {
        itemsList.clear()
        itemsList.addAll(items)
        submitList(ArrayList(itemsList))
    }

    fun getChecklistItems(): List<ChecklistItem> = itemsList

    object ChecklistDiffCallback : DiffUtil.ItemCallback<ChecklistItem>() {
        override fun areItemsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
            return oldItem == newItem
        }
    }
}