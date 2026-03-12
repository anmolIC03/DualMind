package com.example.dualmind.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.data.local.MeetingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingDao: MeetingDao
) : ViewModel() {

    val meetings: StateFlow<List<MeetingEntity>> = meetingDao.getAllMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // FIX 2: provide assembled transcript per meeting for the card preview
    fun getTranscriptForMeeting(meetingId: Int): Flow<String> =
        meetingDao.getChunksForMeeting(meetingId).map { chunks ->
            chunks
                .filter { it.isTranscribed }
                .sortedBy { it.sequenceNumber }
                .joinToString(" ") { it.transcriptText }
                .trim()
        }
}