package com.example.dualmind.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
// Explicitly importing the correct Result class for WorkManager!
import androidx.work.ListenableWorker.Result
import com.example.dualmind.data.local.MeetingDao
import com.example.dualmind.data.remote.GeminiLlmService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val meetingDao: MeetingDao,
    private val geminiApi: GeminiLlmService
) : CoroutineWorker(context, workerParams) {

    // Notice we use the explicit WorkManager Result here
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val pendingChunks = meetingDao.getPendingChunks()

                if (pendingChunks.isEmpty()) {
                    Log.d("TranscriptionWorker", "No pending chunks found.")
                    return@withContext Result.success()
                }

                Log.d("TranscriptionWorker", "Found ${pendingChunks.size} pending chunks.")

                for (chunk in pendingChunks) {
                    Log.d("TranscriptionWorker", "Transcribing chunk ${chunk.sequenceNumber}...")

                    val transcriptText = geminiApi.transcribeAudioChunk(chunk.filePath, chunk.sequenceNumber)

                    val updatedChunk = chunk.copy(
                        isTranscribed = true,
                        transcriptText = transcriptText
                    )
                    meetingDao.updateAudioChunk(updatedChunk)

                    Log.d("TranscriptionWorker", "Chunk ${chunk.sequenceNumber} transcribed successfully.")
                }

                Result.success()

            } catch (e: Exception) {
                Log.e("TranscriptionWorker", "Transcription failed, retrying...", e)
                Result.retry()
            }
        }
    }
}