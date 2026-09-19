package com.aman.featureapp.bridge

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.webkit.JavascriptInterface
import java.io.File

/**
 * Microphone audio recording bridge exposed as `window.AndroidMic`.
 */
class MicBridge(
    private val context: Context,
    private val featureUid: String
) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recording = false

    private val rootDir: File = File(context.filesDir, "features/$featureUid").apply {
        if (!exists()) mkdirs()
    }

    @JavascriptInterface
    fun startRecording(): Boolean {
        return startRecording(null)
    }

    @JavascriptInterface
    fun startRecording(filename: String?): Boolean {
        if (!PermissionHelper.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
            return false
        }
        if (recording) return false

        return try {
            val name = if (!filename.isNullOrBlank()) filename else "recording_${System.currentTimeMillis()}.m4a"
            val target = File(rootDir, name)
            target.parentFile?.mkdirs()
            currentOutputFile = target

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            recording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            recording = false
            recorder?.release()
            recorder = null
            false
        }
    }

    @JavascriptInterface
    fun stopRecording(): String? {
        if (!recording) return null
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            recording = false
            currentOutputFile?.name
        } catch (e: Exception) {
            e.printStackTrace()
            recorder?.release()
            recorder = null
            recording = false
            null
        }
    }

    @JavascriptInterface
    fun isRecording(): Boolean {
        return recording
    }

    @JavascriptInterface
    fun getMaxAmplitude(): Int {
        if (!recording || recorder == null) return 0
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    @JavascriptInterface
    fun getSoundLevel(): Int {
        val amp = getMaxAmplitude()
        val pct = (amp.toFloat() / 32767f * 100f).toInt()
        return pct.coerceIn(0, 100)
    }
}
