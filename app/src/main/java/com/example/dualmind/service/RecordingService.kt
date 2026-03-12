package com.example.dualmind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dualmind.R
import com.example.dualmind.data.local.AudioChunkEntity
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.service.worker.TranscriptionWorker
import com.example.dualmind.util.AudioUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var meetingDao: MeetingDao

    private var activeRecorder: MediaRecorder? = null
    private var isRecordingPaused = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var audioManager: AudioManager
    private var sequenceNumber = 0

    private var currentAudioFile: File? = null
    private var currentMeetingId = 1

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startRecordingSession()
            "STOP" -> stopRecordingSession()
        }
        return START_STICKY
    }

    private fun startRecordingSession() {
        val focusResult = audioManager.requestAudioFocus(
            focusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            startForeground(1, createNotification("Recording...", "00:00"))

            serviceScope.launch {
                while (isActive) {
                    currentAudioFile = AudioUtil.createAudioFile(
                        this@RecordingService,
                        currentMeetingId,
                        sequenceNumber
                    )
                    activeRecorder = AudioUtil.getConfiguredRecorder(
                        this@RecordingService,
                        currentAudioFile!!
                    )
                    activeRecorder?.start()

                    delay(28000)

                    try { activeRecorder?.stop() } catch (e: Exception) { }
                    activeRecorder?.release()
                    activeRecorder = null

                    // FIX 1 + 3: capture values and null out currentAudioFile before async work
                    saveCurrentChunkAndTriggerWorker()
                    sequenceNumber++
                }
            }
        }
    }

    private fun stopRecordingSession() {
        // Stop the coroutine loop first so it can't race with us
        serviceScope.cancel()

        try { activeRecorder?.stop() } catch (e: Exception) { }
        activeRecorder?.release()
        activeRecorder = null

        // Save whatever was recorded before the 28s chunk completed
        saveCurrentChunkAndTriggerWorker()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveCurrentChunkAndTriggerWorker() {
        // FIX 3: null out immediately so a racing call can't double-save
        val capturedFile = currentAudioFile ?: return
        currentAudioFile = null

        // FIX 1: capture sequenceNumber now, before the coroutine is scheduled
        val capturedSequence = sequenceNumber

        CoroutineScope(Dispatchers.IO).launch {
            val chunkEntity = AudioChunkEntity(
                meetingId = currentMeetingId,
                filePath = capturedFile.absolutePath,
                sequenceNumber = capturedSequence
            )
            meetingDao.insertAudioChunk(chunkEntity)

            // FIX 2: unique work prevents multiple workers processing the same chunks in parallel
            WorkManager.getInstance(this@RecordingService).enqueueUniqueWork(
                "transcription",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<TranscriptionWorker>().build()
            )
        }
    }

    // --- EDGE CASE 1: Audio Focus Loss ---
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseRecording("Paused - Audio focus lost")
            AudioManager.AUDIOFOCUS_GAIN -> resumeRecording()
        }
    }

    // --- EDGE CASE 2: Phone Calls ---
    private fun setupPhoneStateListener() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                mainExecutor,
                object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        when (state) {
                            TelephonyManager.CALL_STATE_OFFHOOK,
                            TelephonyManager.CALL_STATE_RINGING -> pauseRecording("Paused - Phone call")
                            TelephonyManager.CALL_STATE_IDLE -> resumeRecording()
                        }
                    }
                }
            )
        }
    }

    private fun pauseRecording(reason: String) {
        if (!isRecordingPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activeRecorder?.pause()
            isRecordingPaused = true
            updateNotification(reason)
        }
    }

    private fun resumeRecording() {
        if (isRecordingPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activeRecorder?.resume()
            isRecordingPaused = false
            updateNotification("Recording...")
        }
    }

    // --- NOTIFICATIONS ---
    private fun createNotification(status: String, timerText: String): Notification {
        val builder = NotificationCompat.Builder(this, "DUALMIND_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("DualMind")
            .setContentText(status)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }
        return builder.build()
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification(status, "00:00"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "DUALMIND_CHANNEL",
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}