package com.example.nomemo.glyph

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nomemo.data.db.AppDatabase
import kotlinx.coroutines.flow.first

/**
 * Background WorkManager worker for No Memo Burn Notes.
 *
 * Runs periodically to:
 * 1. Shred and purge expired Burn Notes from Room DB.
 * 2. Check for Burn Notes in their final 15 minutes before self-destruction
 *    and trigger the rear Glyph interface warning indicator via GlyphHardwareManager.
 */
class BurnTimerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val notes = db.noteDao().getAllActiveNotes().first()
            val now = System.currentTimeMillis()

            // 1. Purge expired burn notes
            val expired = notes.filter { it.isBurn && it.burnExpiresAt != null && it.burnExpiresAt <= now }
            for (note in expired) {
                db.noteDao().deleteNote(note)
            }

            // 2. Check for burn notes with <= 15 minutes remaining
            val finalHourNotes = notes.filter {
                it.isBurn && it.burnExpiresAt != null && (it.burnExpiresAt - now) in 1..(15 * 60 * 1000L)
            }

            val glyphManager = GlyphHardwareManager.getInstance(applicationContext)
            glyphManager.setBurnWarning(finalHourNotes.isNotEmpty())

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "com.example.nomemo.glyph.BurnTimerWorker"
    }
}
