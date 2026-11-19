package com.example.nomemo.mirror

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.pref.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework (SAF) Local Markdown Folder Mirror.
 *
 * Asynchronously mirrors notes to a user-selected local directory as clean .md files
 * formatted with YAML frontmatter (id, pinned, burnExpiresAt, updatedAt, type, tags).
 * Automatically updates files when notes change, and deletes them when shredded or removed.
 */
object MarkdownMirrorManager {

    private const val TAG = "MarkdownMirrorManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Asynchronously writes or updates a single note file in the mirrored SAF directory.
     */
    fun syncNote(context: Context, note: Note) {
        val uriStr = PreferencesManager(context).markdownMirrorUri ?: return
        scope.launch {
            try {
                val treeUri = Uri.parse(uriStr)
                val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return@launch
                if (!dir.canWrite()) return@launch

                val fileName = getSafeFileName(note)
                var file = dir.findFile(fileName)
                if (file == null) {
                    file = dir.createFile("text/markdown", fileName)
                }

                file?.let { docFile ->
                    context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { stream ->
                        val mdContent = buildMarkdown(note)
                        stream.write(mdContent.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to mirror note: ${e.message}")
            }
        }
    }

    /**
     * Asynchronously deletes a note's .md file from the mirrored SAF directory.
     */
    fun deleteNoteFile(context: Context, note: Note) {
        val uriStr = PreferencesManager(context).markdownMirrorUri ?: return
        scope.launch {
            try {
                val treeUri = Uri.parse(uriStr)
                val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return@launch
                val fileName = getSafeFileName(note)
                val file = dir.findFile(fileName)
                file?.delete()
            } catch (e: Exception) {
                Log.d(TAG, "Failed to delete mirrored note: ${e.message}")
            }
        }
    }

    /**
     * Mirrors all existing active notes into the target folder on initial selection.
     */
    fun mirrorAllNotes(context: Context, notes: List<Note>) {
        scope.launch {
            notes.forEach { note ->
                syncNote(context, note)
            }
        }
    }

    private fun getSafeFileName(note: Note): String {
        val sanitized = note.title.lowercase()
            .replace("[^a-z0-9_-]".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
        val base = if (sanitized.isNotBlank()) sanitized else "note-${note.id}"
        return "$base.md"
    }

    private fun buildMarkdown(note: Note): String {
        val sb = StringBuilder()
        sb.appendLine("---")
        sb.appendLine("id: ${note.id}")
        sb.appendLine("pinned: ${note.isPinned}")
        sb.appendLine("burnExpiresAt: ${note.burnExpiresAt ?: "null"}")
        sb.appendLine("updatedAt: ${note.timestamp}")
        sb.appendLine("type: ${note.type}")
        sb.appendLine("tags: [${note.tags.joinToString(", ")}]")
        sb.appendLine("---")
        sb.appendLine()

        if (note.type == NoteType.CHECKLIST) {
            note.checklistItems.forEach { item ->
                val box = if (item.isChecked) "- [x] " else "- [ ] "
                sb.appendLine("$box${item.text}")
            }
        } else {
            sb.appendLine(note.body)
        }
        return sb.toString()
    }
}
