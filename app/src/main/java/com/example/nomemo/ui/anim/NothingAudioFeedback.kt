package com.example.nomemo.ui.anim

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural Mechanical Audio Engine for Nothing OS 5.0.
 *
 * Generates zero-latency mechanical micro-clicks directly via PCM audio synthesis:
 * - 0 MB external asset overhead
 * - 0ms audio loading latency
 * - Perfect synchronization with haptic pulses for authentic hardware feel
 */
object NothingAudioFeedback {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 44100

    // Pre-rendered 16-bit PCM byte buffers
    private val tactileClickBuffer: ShortArray by lazy { generateClick(frequency = 1600.0, durationMs = 12, decay = 280.0) }
    private val checkChimeBuffer: ShortArray by lazy { generateChime(frequency = 2200.0, durationMs = 28, decay = 120.0) }
    private val lockBoltBuffer: ShortArray by lazy { generateBoltClick() }

    private fun generateClick(frequency: Double, durationMs: Int, decay: Double): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * decay)
            val sample = (sin(2.0 * PI * frequency * t) * envelope * 0.45 * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChime(frequency: Double, durationMs: Int, decay: Double): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * decay)
            // Fundamental + overtone for rich metallic chime
            val s1 = sin(2.0 * PI * frequency * t)
            val s2 = sin(2.0 * PI * (frequency * 1.5) * t) * 0.3
            val sample = ((s1 + s2) * envelope * 0.40 * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateBoltClick(): ShortArray {
        val numSamples = (sampleRate * (24 / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 180.0)
            val freq = 1200.0 - (t * 8000.0) // Pitch drop like a mechanical lock bolt falling
            val sample = (sin(2.0 * PI * freq * t) * envelope * 0.50 * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun playBuffer(buffer: ShortArray) {
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Auto-release after playing
                kotlinx.coroutines.delay(60L)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun playTactileClick() {
        playBuffer(tactileClickBuffer)
    }

    fun playCheckChime() {
        playBuffer(checkChimeBuffer)
    }

    fun playLockBolt() {
        playBuffer(lockBoltBuffer)
    }

    fun playLockToggle(isLocking: Boolean = false) {
        if (isLocking) {
            playLockBolt()
        } else {
            playTactileClick()
        }
    }
}
