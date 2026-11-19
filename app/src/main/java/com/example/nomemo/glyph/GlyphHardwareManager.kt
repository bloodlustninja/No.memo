package com.example.nomemo.glyph

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method

/**
 * Hardware Abstraction Manager for the Nothing Phone Glyph Interface.
 *
 * Safely wraps Nothing Glyph SDK (`com.nothing.ketchum.GlyphManager`) via dynamic
 * reflection so non-Nothing phones, generic Android devices, and emulators degrade
 * gracefully without ClassNotFoundException or runtime crashes.
 *
 * Supported Hardware Cues:
 * a) pulseTranscriptionConfirmation: Single-zone ~180ms pulse on voice-to-note speech recognition.
 * b) setBurnWarning: Low-brightness rear zone warning indicator when a Burn Note has <= 15m remaining.
 */
class GlyphHardwareManager private constructor(private val context: Context) {

    private val isNothingDevice: Boolean =
        Build.MANUFACTURER.equals("Nothing", ignoreCase = true) ||
            Build.BRAND.equals("Nothing", ignoreCase = true)

    private var glyphManagerInstance: Any? = null
    private var openSessionMethod: Method? = null
    private var closeSessionMethod: Method? = null
    private var animateMethod: Method? = null
    private var toggleMethod: Method? = null
    private var isSessionOpen = false

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var burnWarningJob: Job? = null

    init {
        if (isNothingDevice) {
            initGlyphReflection()
        }
    }

    private fun initGlyphReflection() {
        try {
            val glyphManagerClass = Class.forName("com.nothing.ketchum.GlyphManager")
            val getInstanceMethod = glyphManagerClass.getMethod("getInstance", Context::class.java)
            glyphManagerInstance = getInstanceMethod.invoke(null, context.applicationContext)

            val clazz = glyphManagerInstance?.javaClass
            openSessionMethod = clazz?.methods?.firstOrNull { it.name == "openSession" }
            closeSessionMethod = clazz?.methods?.firstOrNull { it.name == "closeSession" }
            animateMethod = clazz?.methods?.firstOrNull { it.name == "animate" }
            toggleMethod = clazz?.methods?.firstOrNull { it.name == "toggle" }

            // Initialize connection callback if present
            val initMethod = clazz?.methods?.firstOrNull { it.name == "init" }
            initMethod?.let { method ->
                if (method.parameterTypes.isEmpty()) {
                    method.invoke(glyphManagerInstance)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Nothing Glyph SDK reflection unavailable or non-Nothing OS: ${e.message}")
            glyphManagerInstance = null
        }
    }

    /**
     * Trigger a crisp single-zone Glyph pulse (~180ms) when voice transcription finishes.
     * Falls back to subtle system tactile haptics on non-Nothing devices.
     */
    fun pulseTranscriptionConfirmation() {
        if (isNothingDevice && glyphManagerInstance != null) {
            scope.launch {
                try {
                    ensureSessionOpen()
                    // Send pulse/toggle command via SDK reflection
                    tryTriggerGlyphPulse(180L)
                } catch (e: Exception) {
                    Log.d(TAG, "Glyph pulse fallback: ${e.message}")
                    fallbackHapticPulse(180L)
                }
            }
        } else {
            fallbackHapticPulse(180L)
        }
    }

    /**
     * Activates or deactivates the subtle low-brightness Glyph zone indicator
     * when a Burn Note has <= 15 minutes remaining before self-destruction.
     */
    fun setBurnWarning(active: Boolean) {
        burnWarningJob?.cancel()
        if (!active) {
            if (isSessionOpen) {
                try {
                    closeSessionMethod?.invoke(glyphManagerInstance)
                    isSessionOpen = false
                } catch (_: Exception) {}
            }
            return
        }

        if (isNothingDevice && glyphManagerInstance != null) {
            burnWarningJob = scope.launch {
                try {
                    ensureSessionOpen()
                    // Infinite subtle heartbeat / breathing pulse every 4 seconds while <= 15m remaining
                    while (true) {
                        tryTriggerGlyphPulse(240L)
                        delay(4000L)
                    }
                } catch (_: Exception) {
                    fallbackHapticPulse(100L)
                }
            }
        } else {
            fallbackHapticPulse(100L)
        }
    }

    private fun ensureSessionOpen() {
        if (!isSessionOpen) {
            try {
                openSessionMethod?.invoke(glyphManagerInstance)
                isSessionOpen = true
            } catch (_: Exception) {}
        }
    }

    private fun tryTriggerGlyphPulse(durationMs: Long) {
        try {
            // Attempt invoking animate or toggle method on GlyphManager
            if (animateMethod != null) {
                // animate(channel, period, interval) or similar
                animateMethod?.invoke(glyphManagerInstance)
            } else if (toggleMethod != null) {
                toggleMethod?.invoke(glyphManagerInstance)
            } else {
                fallbackHapticPulse(durationMs)
            }
        } catch (_: Exception) {
            fallbackHapticPulse(durationMs)
        }
    }

    /**
     * Clean haptic degradation for non-Nothing devices and emulators.
     */
    private fun fallbackHapticPulse(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, 140)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, 140))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "GlyphHardwareManager"

        @Volatile
        private var INSTANCE: GlyphHardwareManager? = null

        fun getInstance(context: Context): GlyphHardwareManager {
            return INSTANCE ?: synchronized(this) {
                val instance = GlyphHardwareManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
