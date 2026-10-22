package com.example.nomemo.ui.settings

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.nomemo.data.db.AppDatabase
import com.example.nomemo.data.pref.PreferencesManager
import com.example.nomemo.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        // Restore state
        if (prefsManager.isPitchBlackEnabled) {
            binding.radioPitchBlack.isChecked = true
        } else {
            binding.radioLight.isChecked = true
        }
        binding.switchGlassmorphism.isChecked = prefsManager.isGlassmorphismEnabled
        binding.switchHaptics.isChecked = prefsManager.isHapticsEnabled

        // Radio Group Listener for Pitch Black vs Nothing Light Mode
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            performHaptic()
            val isPitchBlack = checkedId == binding.radioPitchBlack.id
            prefsManager.isPitchBlackEnabled = isPitchBlack

            if (isPitchBlack) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Pitch Black Mode Active", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Nothing Light Mode Active", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchGlassmorphism.setOnCheckedChangeListener { _, isChecked ->
            performHaptic()
            prefsManager.isGlassmorphismEnabled = isChecked
            Toast.makeText(this, "Frosted Glass ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) performHaptic()
            prefsManager.isHapticsEnabled = isChecked
            Toast.makeText(this, "Haptics ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener {
            performHaptic()
            finish()
        }

        binding.btnClearData.setOnClickListener {
            performHaptic()
            confirmClearAllNotes()
        }

        binding.btnExportNotes.setOnClickListener {
            performHaptic()
            Toast.makeText(this, "Notes exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performHaptic() {
        if (prefsManager.isHapticsEnabled) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun confirmClearAllNotes() {
        AlertDialog.Builder(this)
            .setTitle("CLEAR ALL NOTES")
            .setMessage("Are you sure you want to permanently delete all notes?")
            .setPositiveButton("DELETE") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.clearAllTables()
                    Toast.makeText(this@SettingsActivity, "All notes cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}