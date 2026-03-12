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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    meetingDao: MeetingDao
) : ViewModel() {

    // stateIn converts the Flow from Room into a StateFlow that Jetpack Compose can observe.
    // It automatically pauses database queries when the app is in the background!
    val meetings: StateFlow<List<MeetingEntity>> = meetingDao.getAllMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}