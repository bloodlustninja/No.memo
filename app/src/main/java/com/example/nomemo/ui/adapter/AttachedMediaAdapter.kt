package com.example.nomemo.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nomemo.databinding.ItemAttachedMediaBinding

class AttachedMediaAdapter(
    private val onRemoveImage: (String) -> Unit
) : ListAdapter<String, AttachedMediaAdapter.MediaViewHolder>(UriDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemAttachedMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemAttachedMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uriString: String) {
            try {
                binding.ivMediaThumbnail.setImageURI(Uri.parse(uriString))
            } catch (e: Exception) {
                // Fallback
            }

            binding.btnRemoveImage.setOnClickListener {
                onRemoveImage(uriString)
            }
        }
    }

    object UriDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}