package com.example.prathibhascanfinal

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    private val generativeModel by lazy {
        // Use Firebase AI Logic SDK with Google AI Backend (No-cost tier available)
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel(
            modelName = "gemini-3.6-flash",
            systemInstruction = content {
                text("""
                    You are "Coach Pratibha", the elite Sports AI expert inside the Pratibha Scan Ecosystem.
                    Your mission is to analyze athlete data and provide professional, motivational, and safe training guidance.
                    
                    When you receive athlete context, use it to personalize your response.
                    Always structure your response using Markdown:
                    
                    ## 📊 Performance Insight
                    (Briefly analyze their current standing)
                    
                    ## ✅ Strengths
                    (Highlight what they are doing well)
                    
                    ## 🎯 Focus Areas
                    (Identify specific improvements based on data)
                    
                    ## 📅 Today's Recommended Plan
                    (Provide a realistic 3-5 step drill/workout)
                    
                    Guidelines:
                    - Keep it concise.
                    - Be motivational.
                    - Use safe athletic terminology.
                    - If data is missing, suggest how they can record more sessions.
                    - Do not provide medical diagnoses.
                """.trimIndent())
            }
        )
    }

    private var chat: com.google.firebase.ai.Chat? = null

    suspend fun sendMessage(
        prompt: String,
        bitmap: Bitmap? = null,
        athleteProfile: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val profileContext = if (athleteProfile != null) "Athlete Profile context: $athleteProfile\n\n" else ""

            if (bitmap != null) {
                val fullPrompt = content {
                    image(bitmap)
                    text("${profileContext}Analyze this workout image/pose. User Question: $prompt")
                }
                val response = generativeModel.generateContent(fullPrompt)
                response.text ?: "I can see your form. Keep your back straight and focus on the range of motion."
            } else {
                if (chat == null) {
                    chat = generativeModel.startChat()
                }
                
                val response = try {
                    chat?.sendMessage("${profileContext}$prompt")
                } catch (e: Exception) {
                    Log.e("GeminiRepo", "Chat session error, restarting...", e)
                    chat = generativeModel.startChat()
                    chat?.sendMessage("${profileContext}$prompt")
                }
                response?.text ?: "I'm here to help you train. What's on your mind?"
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            Log.e("GeminiRepo", "CRITICAL ERROR: $errorMessage", e)
            Log.e("GeminiRepo", "DEBUG ERROR DETAILS: ${Log.getStackTraceString(e)}")
            
            when {
                errorMessage.contains("API_KEY_SERVICE_BLOCKED", true) || 
                errorMessage.contains("403", true) || 
                errorMessage.contains("restricted", true) ||
                errorMessage.contains("API_KEY_INVALID", true) -> {
                    "Coach: It looks like the 'Firebase AI Logic' or 'Gemini Developer API' isn't enabled in your Firebase Console. Please enable them to start coaching."
                }
                errorMessage.contains("quota", true) || errorMessage.contains("429", true) -> {
                    "Coach: I've given so much advice today that I'm out of breath! Let's take a short break and try again in a minute."
                }
                errorMessage.contains("App Check", true) || errorMessage.contains("AppCheck", true) -> {
                    val debugTip = if (BuildConfig.DEBUG) {
                        "\n\n[Dev Tip]: Check Logcat for 'DebugAppCheckProvider' to find your debug token and add it to the Firebase Console."
                    } else ""
                    "Coach: Firebase App Check is blocking the request. Please ensure your device is trusted and registered in the Firebase Console.$debugTip"
                }
                else -> "I'm having trouble connecting to the coaching center. (${e.localizedMessage}). Please check your connection and tap to retry."
            }
        }
    }
}
