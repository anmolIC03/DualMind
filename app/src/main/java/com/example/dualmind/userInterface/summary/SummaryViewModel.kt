package com.example.dualmind.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.data.local.MeetingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val meetingDao: MeetingDao
) : ViewModel() {

    private val _meeting = MutableStateFlow<MeetingEntity?>(null)
    val meeting: StateFlow<MeetingEntity?> = _meeting.asStateFlow()

    private val _fullTranscript = MutableStateFlow("")
    val fullTranscript: StateFlow<String> = _fullTranscript.asStateFlow()

    fun loadMeetingData(meetingId: Int) {
        // 1. Fetch the meeting details
        viewModelScope.launch {
            meetingDao.getMeetingById(meetingId).collect {
                _meeting.value = it
            }
        }

        // 2. Fetch all chunks, sort them, and combine the text
        viewModelScope.launch {
            meetingDao.getChunksForMeeting(meetingId).collect { chunks ->
                val combinedText = chunks
                    .filter { it.isTranscribed }
                    .sortedBy { it.sequenceNumber } // Ensures the conversation is in order
                    .joinToString(" ") { it.transcriptText }

                _fullTranscript.value = combinedText.trim()
            }
        }
    }
}