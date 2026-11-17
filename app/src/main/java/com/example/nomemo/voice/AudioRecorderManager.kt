package com.example.nomemo.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * High-Fidelity Audio Memo Recorder for NO.MEMO.
 *
 * Records .m4a audio and tracks amplitude levels in real-time to generate
 * the dot-matrix audio waveform visualizer for Nothing OS 5.0 Bento cards.
 */
class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _amplitudeWaveform = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeWaveform = _amplitudeWaveform.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs = _elapsedTimeMs.asStateFlow()

    private var startTimeMs = 0L

    fun startRecording(): File? {
        val audioDir = File(context.filesDir, "audio_memos").apply { if (!exists()) mkdirs() }
        val outputFile = File(audioDir, "memo_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        _amplitudeWaveform.value = emptyList()
        _elapsedTimeMs.value = 0L
        startTimeMs = System.currentTimeMillis()

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true

            // Sample amplitudes every 100ms
            recordJob = scope.launch {
                val samples = mutableListOf<Float>()
                while (isActive && _isRecording.value) {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val normalized = (maxAmp / 32767f).coerceIn(0.1f, 1f)
                    samples.add(normalized)
                    if (samples.size > 28) samples.removeAt(0)
                    _amplitudeWaveform.value = samples.toList()
                    _elapsedTimeMs.value = System.currentTimeMillis() - startTimeMs
                    delay(100L)
                }
            }
            return outputFile
        } catch (_: Exception) {
            stopRecording()
            return null
        }
    }

    fun stopRecording(): RecordResult? {
        val duration = System.currentTimeMillis() - startTimeMs
        recordJob?.cancel()
        recordJob = null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _isRecording.value = false

        val file = currentOutputFile
        if (file != null && file.exists() && file.length() > 0) {
            return RecordResult(
                file = file,
                durationMs = duration,
                waveform = _amplitudeWaveform.value,
            )
        }
        return null
    }

    data class RecordResult(
        val file: File,
        val durationMs: Long,
        val waveform: List<Float>,
    )
}
