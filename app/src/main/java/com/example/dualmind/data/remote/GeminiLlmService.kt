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
    suspend fun generateMeetingSummary(transcript: String): AiSummaryResult {
        return withContext(Dispatchers.IO) {
            val prompt = """
            Analyze the following meeting transcript.
            You MUST return your response STRICTLY as a JSON object with exactly these three keys:
            {
                "summary": "A brief 2-3 sentence overview of the meeting.",
                "actionItems": "A bulleted list of tasks assigned.",
                "keyPoints": "A bulleted list of the main topics discussed."
            }
            Do NOT include markdown formatting like ```json. Return ONLY the raw JSON string.
            
            Transcript:
            $transcript
        """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            // Gemini sometimes includes markdown backticks anyway, so we clean them off just in case
            val text = response.text?.removePrefix("```json")?.removeSuffix("```")?.trim()
                ?: throw Exception("Gemini returned an empty response.")

            try {
                // Parse the JSON string into our data class
                val jsonObject = org.json.JSONObject(text)
                AiSummaryResult(
                    summary = jsonObject.optString("summary", "No summary provided."),
                    actionItems = jsonObject.optString("actionItems", "No action items found."),
                    keyPoints = jsonObject.optString("keyPoints", "No key points found.")
                )
            } catch (e: Exception) {
                throw Exception("Failed to parse the AI summary. Please try again.")
            }
        }
    }
}
data class AiSummaryResult(
    val summary: String,
    val actionItems: String,
    val keyPoints: String
)
