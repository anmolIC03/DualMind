package com.example.dualmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    // --- Meeting Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long // Returns the new ID

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("SELECT * FROM meetings ORDER BY createdAt DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getMeetingById(meetingId: Int): Flow<MeetingEntity?>

    // --- Audio Chunk Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioChunk(chunk: AudioChunkEntity)

    @Update
    suspend fun updateAudioChunk(chunk: AudioChunkEntity)

    // Gets all chunks for a meeting, strictly ordered so the transcript makes sense
    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY sequenceNumber ASC")
    fun getChunksForMeeting(meetingId: Int): Flow<List<AudioChunkEntity>>

    // The Worker will call this to find chunks that failed or haven't been sent to the API yet
    @Query("SELECT * FROM audio_chunks WHERE isTranscribed = 0 ORDER BY sequenceNumber ASC")
    suspend fun getPendingChunks(): List<AudioChunkEntity>
}