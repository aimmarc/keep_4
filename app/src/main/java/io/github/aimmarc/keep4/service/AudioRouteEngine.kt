package io.github.aimmarc.keep4.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRouteEngine(context: Context) {
    private val appContext = context.applicationContext
    private val outputFile = File(appContext.cacheDir, "keep4-routing.amr")
    private var recorder: MediaRecorder? = null

    init {
        outputFile.delete()
    }

    @Synchronized
    fun synchronize(audioPlaying: Boolean): ServiceSnapshot {
        return when {
            audioPlaying && recorder == null -> startRecording()
            !audioPlaying && recorder != null -> {
                stopRecording()
                ServiceSnapshot(ServiceMode.MONITORING)
            }
            recorder != null -> ServiceSnapshot(ServiceMode.RECORDING)
            else -> ServiceSnapshot(ServiceMode.MONITORING)
        }
    }

    @Synchronized
    fun close() {
        stopRecording()
    }

    private fun startRecording(): ServiceSnapshot {
        val candidate = createRecorder()
        return try {
            outputFile.delete()
            candidate.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = candidate
            ServiceSnapshot(ServiceMode.RECORDING)
        } catch (error: Exception) {
            runCatching { candidate.release() }
            outputFile.delete()
            ServiceSnapshot(ServiceMode.ERROR, error.message ?: "无法启动录音路由")
        }
    }

    private fun stopRecording() {
        val activeRecorder = recorder ?: return
        recorder = null
        try {
            activeRecorder.stop()
        } catch (_: RuntimeException) {
            // A recorder can fail to stop when it did not receive enough audio data.
        } finally {
            runCatching { activeRecorder.release() }
            outputFile.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }
}
