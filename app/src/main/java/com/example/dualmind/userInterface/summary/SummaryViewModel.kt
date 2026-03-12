package com.example.dualmind.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.data.local.MeetingEntity
import com.example.dualmind.data.remote.GeminiLlmService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val meetingDao: MeetingDao,
    private val geminiService: GeminiLlmService // Inject Gemini here!
) : ViewModel() {

    private val _meeting = MutableStateFlow<MeetingEntity?>(null)
    val meeting: StateFlow<MeetingEntity?> = _meeting.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    // --- NEW STATES FOR THE UI ---
    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _summaryError = MutableStateFlow<String?>(null)
    val summaryError: StateFlow<String?> = _summaryError.asStateFlow()

    fun loadMeetingData(meetingId: Int) {
        viewModelScope.launch {
            meetingDao.getMeetingById(meetingId).collect { currentMeeting ->
                _meeting.value = currentMeeting

                // If we have a meeting, check if we need to generate a summary
                if (currentMeeting != null) {
                    fetchChunksAndCheckSummary(currentMeeting)
                }
            }
        }
    }

    private suspend fun fetchChunksAndCheckSummary(currentMeeting: MeetingEntity) {
        meetingDao.getChunksForMeeting(currentMeeting.id).collect { chunks ->
            // Glue the text together
            val combinedText = chunks
                .filter { it.isTranscribed }
                .sortedBy { it.sequenceNumber }
                .joinToString(" ") { it.transcriptText }
                .trim()

            _fullTranscript.value = combinedText

            // If the transcript is ready but the summary is empty, generate it automatically!
            if (combinedText.isNotEmpty() && currentMeeting.summary.isEmpty() && !_isGeneratingSummary.value) {
                generateAiSummary(currentMeeting, combinedText)
            }
        }
    }

    // The Retry function for your UI Button
    fun retryGeneratingSummary(meetingId: Int) {
        val currentMeeting = _meeting.value ?: return
        val transcript = _fullTranscript.value
        if (transcript.isNotEmpty()) {
            generateAiSummary(currentMeeting, transcript)
        }
    }

    private fun generateAiSummary(meeting: MeetingEntity, transcript: String) {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            _summaryError.value = null

            try {
                // Call Gemini
                val aiResult = geminiService.generateMeetingSummary(transcript)

                // Save it to the database
                val updatedMeeting = meeting.copy(
                    fullTranscript = transcript,
                    summary = aiResult.summary,
                    actionItems = aiResult.actionItems,
                    keyPoints = aiResult.keyPoints,
                    isProcessing = false
                )
                meetingDao.updateMeeting(updatedMeeting) // Make sure you have this in your DAO!

            } catch (e: Exception) {
                _summaryError.value = e.message ?: "An unknown error occurred."
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }
}