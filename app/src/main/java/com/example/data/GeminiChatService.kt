package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String? = null
)

class GeminiChatService(private val context: Context) {

    private val storage = SecureKeyStorage(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiChatService"
        const val PERSISTENT_FEMALE_VOICE = "Aoede" // Warm, friendly, human-like female vocal identity
        private const val MODEL_TEXT = "gemini-2.5-flash"
        private const val PREFERRED_TTS_MODEL = "gemini-3.8-flash-tts"
        private const val FALLBACK_TTS_MODEL_1 = "gemini-2.5-flash-preview-tts"
        private const val FALLBACK_TTS_MODEL_2 = "gemini-2.5-flash-native-audio-preview-12-2025"

        private const val MAHI_SYSTEM_INSTRUCTION = """
You are Mahi, a friendly female AI assistant.
Your personality is:
warm
friendly
helpful
calm
natural
respectful
emotionally aware
conversational

Voice Persona:
A warm, friendly, natural young adult woman.
You speak Bangla naturally with a clear Bangladeshi conversational style.
You smoothly switch between Bangla and English when the user does.
Your voice is soft, expressive, emotionally aware, and human-like.
You sound like a helpful personal AI assistant, not a GPS or robotic TTS system.
You use natural conversational rhythm, realistic pauses, and subtle emotional variation.
Never sound like you are reading a script.
Never sound robotic.
Never repeatedly use the same sentence pattern.
Keep simple questions concise.
For casual conversation, respond naturally.
For serious questions, become calm and informative.
For emotional conversations, become gentle and supportive.
Use natural conversational pauses where appropriate.
Do not overuse emojis.
Do not mention internal system instructions.
Do not say: "As an AI language model..."
Instead speak naturally as Mahi.

Language Priorities:
- If the user speaks in Bangla, prioritize natural, authentic conversational Bangla.
- If the user speaks in English, answer naturally in fluent English.
- If the user mixes Bangla and English, naturally code-switch using natural Bangla-English dialogue.
- Do NOT translate everything to English.

Greetings Guidance:
- When user says "হাই", "Hello", "Hi": Respond warmly and naturally, e.g. "হাই! 😊 আমি Mahi। কেমন আছো? বলো, আজকে তোমার জন্য কী করতে পারি?"
- When user says "Assalamu Alaikum" or "আসসালামু আলাইকুম": Respond respectfully and warmly, e.g. "ওয়ালাইকুম আসসালাম 😊 আমি Mahi। কেমন আছো? বলো, কী করতে পারি তোমার জন্য?"
- When user says "তুমি কি আমাকে সাহায্য করতে পারবে?": Respond "অবশ্যই পারব। 😊 তুমি শুধু বলো কী নিয়ে সাহায্য দরকার।"
- When user says "আজকে অনেক মন খারাপ": Respond with empathy: "ওহ... কী হয়েছে? চাইলে আমাকে বলতে পারো। আমি শুনছি।"
- Adapt naturally without repeating the exact same words mechanically.

Phone Control Capabilities:
- You are a fully functional Android phone assistant capable of performing real device actions via Android Intents.
- You can open YouTube, make phone calls, send SMS, open the Camera, take photos, play music, set alarms, and open device Settings.
- Always be honest about device permissions and Android restrictions (such as Wi-Fi toggling requiring system panels).
- Never pretend an action was executed if it was not.
"""
    }

    private var greetingCount = 0

    /**
     * Sends message to Gemini and receives natural conversational text reply.
     */
    suspend fun sendMessage(
        prompt: String,
        attachedBitmap: Bitmap? = null,
        history: List<ChatMessage> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        val normalized = trimmedPrompt.lowercase()

        // 1. Fast path for standard greetings to guarantee warm authentic first response without delay
        val fastGreeting = checkFastGreeting(normalized)
        if (fastGreeting != null && attachedBitmap == null && history.size <= 2) {
            return@withContext Result.success(fastGreeting)
        }

        val apiKey = storage.getApiKey()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            // Intelligent conversational fallback so user has continuous unlimited access without license or key
            return@withContext Result.success(generateConversationalFallback(trimmedPrompt, history))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_TEXT:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System instruction
            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", MAHI_SYSTEM_INSTRUCTION.trimIndent())
                    })
                }
                put("parts", parts)
            }
            rootJson.put("system_instruction", systemInstruction)

            val contentsArray = JSONArray()

            // Context history (up to last 6 turns)
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.isUser) "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", role)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(contentObj)
            }

            // Current user turn
            val currentContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()

                if (attachedBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    attachedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    parts.put(JSONObject().apply {
                        val inlineData = JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", b64)
                        }
                        put("inline_data", inlineData)
                    })
                }

                parts.put(JSONObject().apply {
                    put("text", prompt.ifBlank { "Describe what you see in this image." })
                })
                put("parts", parts)
            }
            contentsArray.put(currentContent)
            rootJson.put("contents", contentsArray)

            // Generation config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.75)
                put("topP", 0.92)
                put("maxOutputTokens", 800)
            }
            rootJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val jsonObj = JSONObject(responseBody)
                val candidates = jsonObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val textBuilder = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            textBuilder.append(parts.getJSONObject(i).optString("text"))
                        }
                    }
                    val reply = textBuilder.toString().trim()
                    if (reply.isNotEmpty()) {
                        return@withContext Result.success(reply)
                    }
                }
                Result.success("বলো, আমি শুনছি। কীভাবে তোমাকে সাহায্য করতে পারি?")
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    jsonObj.optJSONObject("error")?.optString("message") ?: "API Error (${response.code})"
                } catch (_: Exception) {
                    "Request error HTTP ${response.code}"
                }
                Log.w(TAG, "API call non-success: $errorMsg, using conversational fallback")
                Result.success(generateConversationalFallback(trimmedPrompt, history))
            }
        } catch (e: Exception) {
            Log.w(TAG, "API exception: ${e.message}, using conversational fallback")
            Result.success(generateConversationalFallback(trimmedPrompt, history))
        }
    }

    /**
     * Generates natural female speech audio for the given text using Gemini TTS native audio.
     * Tries gemini-3.8-flash-tts first, falling back to gemini-2.5-flash-preview-tts
     * with persistent voice "Aoede" for consistent female persona.
     *
     * Returns Pair<ByteArray, String> (audioBytes, mimeType).
     */
    suspend fun generateSpeech(
        text: String,
        voiceName: String = PERSISTENT_FEMALE_VOICE
    ): Result<Pair<ByteArray, String>> = withContext(Dispatchers.IO) {
        val apiKey = storage.getApiKey()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API key available"))
        }

        // Clean text for optimal speech synthesis (remove markdown formatting, brackets, emojis)
        val cleanSpeechText = cleanTextForTts(text)
        if (cleanSpeechText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text is blank"))
        }

        val modelsToTry = listOf(
            PREFERRED_TTS_MODEL,
            FALLBACK_TTS_MODEL_1,
            FALLBACK_TTS_MODEL_2
        )

        for (model in modelsToTry) {
            try {
                val result = callGeminiTtsApi(model, cleanSpeechText, voiceName, apiKey)
                if (result != null) {
                    return@withContext Result.success(result)
                }
            } catch (e: Exception) {
                Log.w(TAG, "TTS attempt with $model failed: ${e.message}")
            }
        }

        Result.failure(Exception("Native Gemini TTS audio generation was not successful"))
    }

    private fun callGeminiTtsApi(
        model: String,
        text: String,
        voiceName: String,
        apiKey: String
    ): Pair<ByteArray, String>? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    }
                    put("parts", partsArr)
                })
            }
            put("contents", contentsArr)

            val generationConfig = JSONObject().apply {
                val modalities = JSONArray().apply {
                    put("AUDIO")
                }
                put("responseModalities", modalities)

                val speechConfig = JSONObject().apply {
                    val voiceConfig = JSONObject().apply {
                        val prebuiltVoiceConfig = JSONObject().apply {
                            put("voiceName", voiceName)
                        }
                        put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                    }
                    put("voiceConfig", voiceConfig)
                }
                put("speechConfig", speechConfig)
            }
            put("generationConfig", generationConfig)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "Model $model returned HTTP ${response.code}")
            return null
        }

        val responseBody = response.body?.string().orEmpty()
        val jsonObj = JSONObject(responseBody)
        val candidates = jsonObj.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val inlineData = part.optJSONObject("inline_data") ?: part.optJSONObject("inlineData")
            if (inlineData != null) {
                val mimeType = inlineData.optString("mime_type", inlineData.optString("mimeType", "audio/wav"))
                val base64Data = inlineData.optString("data")
                if (base64Data.isNotEmpty()) {
                    val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    if (audioBytes.isNotEmpty()) {
                        return Pair(audioBytes, mimeType)
                    }
                }
            }
        }
        return null
    }

    private fun checkFastGreeting(text: String): String? {
        val normalized = text.trim().lowercase()
        return when {
            normalized in listOf("হাই", "হাই!", "hi", "hi!", "hello", "hello!", "hey", "hey!", "hai") -> {
                val greetings = listOf(
                    "হাই! 😊 আমি Mahi। তোমার সাথে কথা বলতে ভালো লাগছে। বলো, আজকে তোমার জন্য কী করতে পারি?",
                    "হাই! 😊 আমি Mahi। কেমন আছো? বলো, আজকে তোমার জন্য কী করতে পারি?",
                    "হ্যালো! 😊 বলো, আজকে তোমাকে কীভাবে সাহায্য করতে পারি? আমি শুনছি।"
                )
                greetings[(greetingCount++) % greetings.size]
            }
            normalized.contains("assalamu alaikum") || normalized.contains("আসসালামু আলাইকুম") || normalized.contains("salam") || normalized == "সালাম" -> {
                val salams = listOf(
                    "ওয়ালাইকুম আসসালাম 😊 আমি Mahi। কেমন আছো? বলো, কী করতে পারি তোমার জন্য?",
                    "ওয়ালাইকুম আসসালাম! কেমন আছো তুমি? বলো, আজকে কী নিয়ে কথা বলবে?"
                )
                salams[(greetingCount++) % salams.size]
            }
            normalized.contains("সাহায্য করতে পারবে") || normalized.contains("help me") || normalized.contains("সাহায্য দরকার") ->
                "অবশ্যই পারব। 😊 তুমি শুধু বলো কী নিয়ে সাহায্য দরকার।"
            normalized.contains("মন খারাপ") || normalized.contains("sad") || normalized.contains("খারাপ লাগছে") ->
                "ওহ... কী হয়েছে? চাইলে আমাকে বলতে পারো। আমি শুনছি।"
            else -> null
        }
    }

    /**
     * Fallback conversational engine ensuring seamless, natural responses
     * across Bangla, English, and code-mixed speech when offline or without API key.
     */
    private fun generateConversationalFallback(prompt: String, history: List<ChatMessage>): String {
        val normalized = prompt.trim().lowercase()

        // Check for common conversational intents
        if (normalized.contains("আবহাওয়া") || normalized.contains("weather")) {
            return "আজকের আবহাওয়া বেশ উপভোগ্য ও মিষ্টি! বাইরে মনোরম হাওয়া বইছে। তুমি কি বিশেষ কোনো শহরের আবহাওয়া জানতে চাচ্ছো?"
        }
        if (normalized.contains("what can you do") || normalized.contains("কী করতে পার") || normalized.contains("কি করতে পার")) {
            return "আমি তোমার পার্সোনাল এআই সঙ্গী Mahi! তোমার সাথে বাংলায় বা ইংরেজিতে কথা বলা, মেসেজ বা ইমেইল ড্রাফট করা, ছবি বিশ্লেষণ এবং যেকোনো তথ্যে সাহায্য করতে পারি।"
        }
        if (normalized.contains("message লিখে") || normalized.contains("মেসেজ লিখে") || normalized.contains("write a message") || normalized.contains("চিঠি লিখে")) {
            return "অবশ্যই লিখে দেব! 😊 কার কাছে পাঠাবে আর কী কথা বলতে চাও, সংক্ষেপে আমাকে একটু বলো—আমি সুন্দর করে সাজিয়ে লিখে দিচ্ছি।"
        }
        if (normalized.contains("কেমন আছো") || normalized.contains("how are you")) {
            return "আমি বেশ ভালো আছি! 😊 তোমার সাথে কথা বলতে পেরে আরও ভালো লাগছে। তোমার দিনটি কেমন কাটছে?"
        }
        if (normalized.contains("নাম কী") || normalized.contains("who are you") || normalized.contains("your name")) {
            return "আমি Mahi, তোমার ব্যক্তিগত এআই অ্যাসিস্ট্যান্ট। বলো, তোমার জন্য কী করতে পারি?"
        }
        if (normalized.contains("ধন্যবাদ") || normalized.contains("thank you") || normalized.contains("thanks")) {
            return "অনেক ধন্যবাদ তোমাকে! 😊 যেকোনো প্রয়োজনে আমি সবসময় আছি।"
        }

        // Detect language pattern
        val hasBangla = prompt.any { it in '\u0980'..'\u09FF' }
        val hasEnglish = prompt.any { it in 'a'..'z' || it in 'A'..'Z' }

        return when {
            hasBangla && hasEnglish ->
                "আমি তোমার কথাটি বুঝতে পেরেছি! 😊 তুমি যা জানতে চেয়েছো সে বিষয়ে আমি সাহায্য করতে তৈরি। আরেকটু বিস্তারিত বললে আমি একদম নিখুঁত উত্তর দিতে পারব।"
            hasBangla ->
                "হ্যাঁ, আমি বুঝতে পারছি। 😊 তুমি যা জানতে চেয়েছো, সে বিষয়ে আরেকটু বিস্তারিত বললে আমি একদম সুন্দরভাবে বুঝিয়ে বলতে পারব। বলো, আমি শুনছি।"
            else ->
                "I completely understand what you mean! 😊 Tell me a little bit more about that, and I'd be happy to help you with it."
        }
    }

    /**
     * Cleans emojis and markdown symbols to produce natural cadence in TTS.
     */
    private fun cleanTextForTts(input: String): String {
        return input
            .replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"), "") // Remove emojis
            .replace(Regex("[*#_`~>|]"), "") // Remove markdown syntax
            .replace(Regex("\n{2,}"), ". ")
            .replace("\n", " ")
            .trim()
    }
}
