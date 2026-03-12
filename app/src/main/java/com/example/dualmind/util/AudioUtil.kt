package com.example.dualmind.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

object AudioUtil {

    // Creates a new file in the app's internal storage
    fun createAudioFile(context: Context, meetingId: Int, sequence: Int): File {
        val dir = File(context.filesDir, "audio_chunks")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "meeting_${meetingId}_chunk_${sequence}.m4a")
    }

    // Configures the recorder for speech (M4A/AAC is lightweight and supported by Whisper/Gemini)
    fun getConfiguredRecorder(context: Context, outputFile: File): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
    }
}