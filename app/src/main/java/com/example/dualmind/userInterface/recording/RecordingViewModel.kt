package com.example.dualmind.ui.recording
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.data.local.MeetingEntity
import com.example.dualmind.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// This represents the UI State
data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val timeElapsedSeconds: Int = 0,
    val statusMessage: String = "Ready to record",
    val currentMeetingId: Long? = null
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingDao: MeetingDao // Hilt automatically provides this!
) : ViewModel() {

    // Mutable state that only the ViewModel can change
    private val _uiState = MutableStateFlow(RecordingUiState())
    // Read-only state that the Compose UI will listen to (like a Dart Stream)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun toggleRecording() {
        val currentState = _uiState.value
        if (currentState.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            // 1. Create a new meeting in the database
            val newMeeting = MeetingEntity(title = "Meeting ${System.currentTimeMillis()}")
            val meetingId = meetingDao.insertMeeting(newMeeting)

            // 2. Update UI State
            _uiState.update {
                it.copy(
                    isRecording = true,
                    statusMessage = "Recording...",
                    currentMeetingId = meetingId,
                    timeElapsedSeconds = 0
                )
            }

            // 3. Start the Foreground Service
            val intent = Intent(context, RecordingService::class.java).apply {
                action = "START"
                putExtra("MEETING_ID", meetingId) // Pass the ID so the service knows where to save chunks
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // 4. Start the timer for the UI
            startTimer()
        }
    }

    private fun stopRecording() {
        // 1. Update UI State
        _uiState.update {
            it.copy(isRecording = false, statusMessage = "Processing Audio...")
        }
        timerJob?.cancel()

        // 2. Stop the Foreground Service
        val intent = Intent(context, RecordingService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)

        // 3. Update the database to show recording is finished
        viewModelScope.launch {
            _uiState.value.currentMeetingId?.let { id ->
                // In a real app, you'd fetch the entity first, update it, and save.
                // For now, we trigger the background worker to start transcription.
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Wait 1 second
                _uiState.update {
                    it.copy(timeElapsedSeconds = it.timeElapsedSeconds + 1)
                }
            }
        }
    }

    // Formats seconds into MM:SS for the UI
    fun getFormattedTime(): String {
        val totalSeconds = _uiState.value.timeElapsedSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(locale = Locale.getDefault(), "%02d:%02d", minutes,seconds)
    }
}

