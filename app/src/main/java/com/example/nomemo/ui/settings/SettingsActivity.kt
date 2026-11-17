package com.example.nomemo.ui.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.ui.compose.NothingSettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = PreferencesManager(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            NothingSettingsScreen(
                prefsManager = prefsManager,
                onBackClick = { finish() },
                onClearAllNotes = {
                    lifecycleScope.launch {
                        db.clearAllTables()
                        Toast.makeText(this@SettingsActivity, "ALL NOTES PURGED", Toast.LENGTH_SHORT).show()
                    }
                },
                onExportNotes = {
                    lifecycleScope.launch {
                        val notes = db.noteDao().getAllActiveNotes().first()
                        val exportText = buildString {
                            appendLine("# NO.MEMO BACKUP")
                            appendLine("Generated: ${java.util.Date()}\n")
                            notes.forEach { note ->
                                appendLine("## ${note.title}")
                                appendLine("Type: ${note.type} | Date: ${java.util.Date(note.timestamp)}")
                                if (note.tags.isNotEmpty()) appendLine("Tags: ${note.tags.joinToString(", ") { "#$it" }}")
                                appendLine()
                                appendLine(note.body)
                                if (note.checklistItems.isNotEmpty()) {
                                    appendLine("\n### Checklist:")
                                    note.checklistItems.forEach { item ->
                                        appendLine(if (item.isChecked) "- [x] ${item.text}" else "- [ ] ${item.text}")
                                    }
                                }
                                appendLine("\n---\n")
                            }
                        }

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, exportText)
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, "Export Notes"))
                    }
                },
            )
        }
    }
}
