package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
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
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        prompt: String,
        attachedBitmap: Bitmap? = null,
        history: List<ChatMessage> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = storage.getApiKey()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key found. Please configure it in Setup.")
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System instruction to ensure Mahi AI personality
            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are Mahi AI, a brilliant, warm, conversational and supportive personal AI assistant built by Mizan. Keep answers natural, empathetic, crisp, and concise unless detailed analysis is requested. Never mention Maya. Your name is Mahi AI.")
                    })
                }
                put("parts", parts)
            }
            rootJson.put("system_instruction", systemInstruction)

            val contentsArray = JSONArray()

            // Add recent history (up to 4 turns for context)
            val recentHistory = history.takeLast(4)
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

            // Current message
            val currentContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()

                // If image attached
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
                Result.success("I heard you, but I couldn't formulate a response. How else may I assist?")
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    jsonObj.optJSONObject("error")?.optString("message") ?: "API Error (${response.code})"
                } catch (_: Exception) {
                    "Request error HTTP ${response.code}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
