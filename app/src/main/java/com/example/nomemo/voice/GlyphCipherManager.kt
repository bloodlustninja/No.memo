package com.example.nomemo.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Nothing OS 5.0 "Glyph Morse Cipher" Engine.
 *
 * Encodes/decodes text into Morse signals and generates synchronous
 * audio pulses for the physical Glyph LED array.
 */
object GlyphCipherManager {

    private val MORSE_MAP = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..",
        '9' to "----.", ' ' to "/",
    )

    private val REVERSE_MORSE_MAP = MORSE_MAP.entries.associate { (k, v) -> v to k }

    fun textToMorse(text: String): String {
        return text.uppercase().map { ch ->
            MORSE_MAP[ch] ?: ""
        }.filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun morseToText(morse: String): String {
        return morse.split(" ").map { code ->
            if (code == "/") " " else (REVERSE_MORSE_MAP[code]?.toString() ?: "?")
        }.joinToString("")
    }

    /**
     * Synthesizes a pure sine wave tone burst for a Morse dot or dash.
     */
    fun playMorseTone(isDash: Boolean) {
        try {
            val sampleRate = 44100
            val durationMs = if (isDash) 180 else 70
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val freq = 880.0 // 880Hz authentic telegraph tone

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val sample = (sin(2.0 * PI * freq * t) * 0.35 * Short.MAX_VALUE).toInt()
                buffer[i] = sample.toShort()
            }

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
            Thread.sleep(durationMs.toLong())
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }
}
