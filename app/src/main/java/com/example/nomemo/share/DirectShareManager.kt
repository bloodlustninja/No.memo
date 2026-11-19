package com.example.nomemo.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.nomemo.MainActivity
import com.example.nomemo.R
import com.example.nomemo.data.entity.Note

/**
 * Android Direct Share Target Manager.
 *
 * Dynamically publishes the user's top 2 pinned bento tiles as Direct Share targets
 * (category "com.nomemo.share.TARGET_PINNED_NOTE") via ShortcutManagerCompat.
 */
object DirectShareManager {

    const val CATEGORY_PINNED_NOTE = "com.nomemo.share.TARGET_PINNED_NOTE"
    const val EXTRA_TARGET_NOTE_ID = "EXTRA_TARGET_NOTE_ID"

    fun updatePinnedDirectShareTargets(context: Context, pinnedNotes: List<Note>) {
        try {
            val top2 = pinnedNotes.take(2)
            val shortcuts = top2.map { note ->
                val label = note.title.ifBlank { "PINNED NOTE" }.uppercase()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    component = ComponentName(context, MainActivity::class.java)
                    type = "text/plain"
                    putExtra(EXTRA_TARGET_NOTE_ID, note.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                ShortcutInfoCompat.Builder(context, "pinned_note_${note.id}")
                    .setShortLabel(label.take(15))
                    .setLongLabel("Append to $label")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_pin))
                    .setIntent(shareIntent)
                    .setCategories(setOf(CATEGORY_PINNED_NOTE))
                    .build()
            }

            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            if (shortcuts.isNotEmpty()) {
                ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
            }
        } catch (_: Exception) {}
    }
}
