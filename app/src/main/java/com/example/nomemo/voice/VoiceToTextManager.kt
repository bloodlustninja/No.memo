package com.example.nomemo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Continuous Persistent Voice Engine for Nothing OS 5.0.
 *
 * Keeps a single, persistent SpeechRecognizer instance open to eliminate mic toggling
 * or speech engine shutdowns mid-talk or when speaking keywords like "title" or "body".
 */
class VoiceToTextManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    // Real-time transcribed text stream
    private val _spokenText = MutableStateFlow("")
    val spokenText = _spokenText.asStateFlow()

    // Listening state
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    // Microphone RMS volume level (dB)
    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume = _rmsVolume.asStateFlow()

    private var isUserExplicitlyListening = false
    private var baseText = ""

    private fun getSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        isUserExplicitlyListening = true
        _isListening.value = true

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceToTextManager)
            }
        }

        try {
            speechRecognizer?.startListening(getSpeechIntent())
        } catch (_: Exception) {
            recreateAndStart()
        }
    }

    private fun recreateAndStart() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceToTextManager)
        }
        try {
            speechRecognizer?.startListening(getSpeechIntent())
        } catch (_: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        isUserExplicitlyListening = false
        _isListening.value = false
        _rmsVolume.value = 0f

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun clearText() {
        baseText = ""
        _spokenText.value = ""
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        if (isUserExplicitlyListening) {
            _rmsVolume.value = rmsdB
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        if (isUserExplicitlyListening) {
            // Keep mic alive seamlessly on same instance without destroying recognizer
            try {
                speechRecognizer?.startListening(getSpeechIntent())
            } catch (_: Exception) {
                recreateAndStart()
            }
        } else {
            _isListening.value = false
        }
    }

    override fun onError(error: Int) {
        if (isUserExplicitlyListening && error != SpeechRecognizer.ERROR_CLIENT) {
            // Seamless restart without destroying speech instance unless necessary
            try {
                speechRecognizer?.startListening(getSpeechIntent())
            } catch (_: Exception) {
                recreateAndStart()
            }
        } else if (!isUserExplicitlyListening) {
            _isListening.value = false
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val phrase = matches[0].trim()
            if (phrase.isNotBlank()) {
                baseText = if (baseText.isBlank()) phrase else "$baseText $phrase".trim()
                _spokenText.value = baseText
            }
        }

        if (isUserExplicitlyListening) {
            try {
                speechRecognizer?.startListening(getSpeechIntent())
            } catch (_: Exception) {
                recreateAndStart()
            }
        } else {
            _isListening.value = false
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0].trim()
            if (partial.isNotBlank()) {
                val fullText = if (baseText.isBlank()) partial else "$baseText $partial".trim()
                _spokenText.value = fullText
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}