package com.example.dualmind.data.remote
import com.example.dualmind.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


class GeminiLlmService @Inject constructor() {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun transcribeAudioChunk(filePath: String, sequence: Int): String {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                throw Exception("Audio file not found at $filePath")
            }

            // Convert the audio file into raw bytes
            val audioBytes = file.readBytes()

            // Send the prompt and the audio blob to Gemini
            val response = generativeModel.generateContent(
                content {
                    blob("audio/mp4", audioBytes) // Matches the MPEG_4 format we set in AudioHelper
                    text("You are a highly accurate audio transcription engine. Transcribe the spoken words in this audio clip. Return ONLY the transcribed text, with no extra conversational formatting.")
                }
            )

            // Return the text, or throw an error to trigger WorkManager's retry logic
            response.text ?: throw Exception("Gemini returned an empty response")
        }
    }
}