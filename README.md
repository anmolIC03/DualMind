# DualMind - Voice Recording & AI Summary App

DualMind is a robust Android application designed to record real-world conversations, transcribe them in the background, and generate structured, actionable AI summaries. Built entirely with modern Android development practices (Jetpack Compose, MVVM, Room, and WorkManager), it handles complex audio interruptions seamlessly.

##  Features

1. **Robust Background Recording**
   - Utilizes a Foreground Service to record audio even when the app is minimized.
   - Intelligently splits audio into 30-second chunks (with overlap) to prevent data loss.
   - Features a persistent notification with live status updates.

2. **AI-Powered Transcription**
   - Integrates the Google Gemini 2.5 Flash SDK to accurately transcribe audio chunks.
   - Uses `WorkManager` to queue transcriptions, ensuring retries on network failures and maintaining the correct conversational sequence.

3. **Structured AI Summaries**
   - Analyzes the glued-together transcript to generate a structured JSON response.
   - Displays a beautifully formatted UI containing: **Title, Executive Summary, Action Items, and Key Points.**
   - Handles loading states and provides a fallback "Retry" mechanism for AI network failures.

##  Edge Cases Handled

A core focus of this project is real-world reliability. The recording service explicitly handles:
- **Phone Calls:** Automatically pauses recording when an incoming/outgoing call starts and resumes when idle.
- **Audio Focus Loss:** Pauses recording if another app (like YouTube or Spotify) takes over the microphone, and resumes when focus is returned.
- **Process Death:** Audio chunks are immediately saved to a Room database so no data is lost if the system kills the app.

##  Tech Stack & Architecture

- **Language:** Kotlin
- **UI:** 100% Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Local Storage:** Room Database (Single Source of Truth)
- **Background Processing:** WorkManager & Foreground Services
- **Dependency Injection:** Dagger Hilt
- **Asynchrony:** Kotlin Coroutines & StateFlow
- **AI Integration:** Google Generative AI SDK (Gemini 2.5 Flash)
- **Minimum SDK:** API 24 (Android 7.0)

##  Setup & Installation Instructions

For security reasons, the API key is not committed to version control. To build and run this project, please follow these steps carefully:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/anmolIC03/DualMind.git
2. Open the project in **Android Studio**.

3. **Get an API Key:** Generate a free Gemini API key from Google AI Studio.

4. **Configure Local Properties:** Open the local.properties file located in the root directory of the project and add your API key at the bottom:

    **GEMINI_API_KEY**=your_actual_api_key_here   

5. **Sync and Run**: Click Sync Project with Gradle Files, then build and run the application on an emulator or physical device.

## Demo
Link to the screen recording demonstrating the app flow, background recording, and AI summary generation will be placed here.
   
   
   
