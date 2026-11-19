package com.example.nomemo.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import androidx.glance.appwidget.updateAll
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.entity.Note
import com.example.nomemo.data.entity.NoteType
import com.example.nomemo.data.model.ChecklistItem
import com.example.nomemo.data.repository.NoteRepository
import com.example.nomemo.widget.NothingBentoGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * On-Device Android AppFunctions for No Memo (Gemini Local Control).
 *
 * Exposes local on-device capabilities backed directly by Room NoteRepository,
 * allowing Gemini / Google Assistant to execute actions in the background without UI:
 * 1. createBurnNote: ephemeral 24h self-destructing tile
 * 2. appendChecklistItem: finds matching note (or top pinned checklist) and appends item
 * 3. logExpense: appends monetary entry to active/pinned Expense Ledger tile
 */
class NoMemoAppFunctions(private val context: Context) {

    private val repository: NoteRepository by lazy {
        NoteRepository(AppDatabase.getDatabase(context).noteDao(), context.applicationContext)
    }

    /**
     * Creates an ephemeral Burn Note that self-destructs after a specified number of hours.
     *
     * @param title The title of the burn note.
     * @param content The text body of the note.
     * @param burnHours Duration in hours before the note automatically shreds (default: 24).
     * @return Confirmation description of the created ephemeral note.
     */
    @AppFunction
    fun createBurnNote(appFunctionContext: AppFunctionContext, title: String, content: String, burnHours: Int = 24): String = runBlocking(Dispatchers.IO) {
        val durationMs = (burnHours.coerceAtLeast(1) * 3600 * 1000L)
        val burnExpiresAt = System.currentTimeMillis() + durationMs
        val note = Note(
            title = title.ifBlank { "BURN NOTE" },
            body = content,
            type = NoteType.NOTE,
            isBurn = true,
            burnExpiresAt = burnExpiresAt,
            timestamp = System.currentTimeMillis(),
        )
        val id = repository.insertNote(note)
        triggerWidgetSync()
        "Created Burn Note '${note.title}' (expires in ${burnHours}h, ID: $id)"
    }

    /**
     * Appends an unchecked checklist item to a matching note or top pinned checklist.
     *
     * @param noteTitleQuery Search query for the target checklist note title.
     * @param itemText The task item text to append.
     * @return Confirmation description of the appended item.
     */
    @AppFunction
    fun appendChecklistItem(appFunctionContext: AppFunctionContext, noteTitleQuery: String, itemText: String): String = runBlocking(Dispatchers.IO) {
        val activeNotes = repository.getAllActiveNotes().first()
        val targetNote = activeNotes.firstOrNull {
            it.type == NoteType.CHECKLIST && it.title.contains(noteTitleQuery, ignoreCase = true)
        } ?: activeNotes.firstOrNull {
            it.title.contains(noteTitleQuery, ignoreCase = true)
        } ?: activeNotes.firstOrNull {
            it.isPinned && it.type == NoteType.CHECKLIST
        } ?: activeNotes.firstOrNull {
            it.type == NoteType.CHECKLIST
        }

        if (targetNote != null) {
            val updatedItems = targetNote.checklistItems.toMutableList()
            updatedItems.add(ChecklistItem(text = itemText, isChecked = false))
            val updatedNote = targetNote.copy(
                checklistItems = updatedItems,
                type = NoteType.CHECKLIST,
                timestamp = System.currentTimeMillis(),
            )
            repository.updateNote(updatedNote)
            triggerWidgetSync()
            "Appended '[ ] $itemText' to '${targetNote.title.ifBlank { "Checklist" }}'"
        } else {
            val newNote = Note(
                title = noteTitleQuery.ifBlank { "CHECKLIST" }.uppercase(),
                type = NoteType.CHECKLIST,
                checklistItems = listOf(ChecklistItem(text = itemText, isChecked = false)),
                timestamp = System.currentTimeMillis(),
            )
            val id = repository.insertNote(newNote)
            triggerWidgetSync()
            "Created checklist '${newNote.title}' with '[ ] $itemText' (ID: $id)"
        }
    }

    /**
     * Appends an expense entry to the active/pinned Expense Ledger tile or creates an EXPENSES tile.
     *
     * @param item Description of the item or expense.
     * @param amount The monetary amount of the expense.
     * @param tag Optional hashtag category (e.g. "coffee", "food").
     * @return Confirmation description of the logged expense.
     */
    @AppFunction
    fun logExpense(appFunctionContext: AppFunctionContext, item: String, amount: Double, tag: String?): String = runBlocking(Dispatchers.IO) {
        val activeNotes = repository.getAllActiveNotes().first()
        val targetNote = activeNotes.firstOrNull {
            it.isPinned && (it.title.contains("expense", ignoreCase = true) || it.body.contains("#expense", ignoreCase = true))
        } ?: activeNotes.firstOrNull {
            it.title.contains("expense", ignoreCase = true) || it.title.contains("ledger", ignoreCase = true)
        } ?: activeNotes.firstOrNull {
            it.tags.contains("expense") || it.tags.contains("ledger")
        } ?: activeNotes.firstOrNull { it.isPinned }

        val formattedAmount = if ((amount % 1.0) == 0.0) amount.toLong().toString() else amount.toString()
        val tagSuffix = if (!tag.isNullOrBlank()) {
            val cleanTag = tag.trim().removePrefix("#")
            " #$cleanTag"
        } else ""
        val expenseLine = "$item $formattedAmount$tagSuffix"

        if (targetNote != null) {
            val newBody = if (targetNote.body.isBlank()) expenseLine else "${targetNote.body}\n$expenseLine"
            val updatedTags = (targetNote.tags + (if (!tag.isNullOrBlank()) listOf(tag.removePrefix("#").lowercase()) else emptyList())).distinct()
            val updatedNote = targetNote.copy(
                body = newBody,
                tags = updatedTags,
                timestamp = System.currentTimeMillis(),
            )
            repository.updateNote(updatedNote)
            triggerWidgetSync()
            "Logged '$expenseLine' to '${targetNote.title.ifBlank { "Ledger" }}'"
        } else {
            val newNote = Note(
                title = "EXPENSES",
                body = "#expense\n$expenseLine",
                tags = if (!tag.isNullOrBlank()) listOf("expense", tag.removePrefix("#").lowercase()) else listOf("expense"),
                timestamp = System.currentTimeMillis(),
            )
            val id = repository.insertNote(newNote)
            triggerWidgetSync()
            "Created 'EXPENSES' ledger with '$expenseLine' (ID: $id)"
        }
    }

    private suspend fun triggerWidgetSync() {
        try {
            NothingBentoGlanceWidget().updateAll(context.applicationContext)
        } catch (_: Exception) {}
    }
}
