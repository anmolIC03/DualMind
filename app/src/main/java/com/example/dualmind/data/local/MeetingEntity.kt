package com.example.dualmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "New Meeting",
    val createdAt: Long = System.currentTimeMillis(),
    val fullTranscript: String = "",
    val summary: String = "",
    val actionItems: String = "",
    val keyPoints: String = "",
    val isRecording: Boolean = true,
    val isProcessing: Boolean = false
)

@Entity(tableName = "audio_chunks")
data class AudioChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val meetingId: Int, // Links this chunk to a specific meeting
    val filePath: String, // Where the .wav or .m4a is saved on the phone
    val sequenceNumber: Int, // To keep the 30s chunks in the right order
    val isTranscribed: Boolean = false,
    val transcriptText: String = ""
)