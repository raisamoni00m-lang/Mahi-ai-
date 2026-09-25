package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class KeyValidationResult {
    data class Success(val message: String = "Key verified successfully!") : KeyValidationResult()
    data class Error(val message: String) : KeyValidationResult()
}

class GeminiKeyValidator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun validateKey(apiKey: String): KeyValidationResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext KeyValidationResult.Error("Please enter your Gemini API key.")
        }

        // Basic sanity check for key format
        if (trimmedKey.length < 15) {
            return@withContext KeyValidationResult.Error("API key looks too short. Please verify and try again.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$trimmedKey"

        // Minimal prompt payload to test the key
        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {"text": "Ping"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                KeyValidationResult.Success("Key verified successfully with Gemini!")
            } else {
                // Parse Gemini API error message if available
                val errorMessage = try {
                    val jsonObj = JSONObject(responseBody)
                    val errorObj = jsonObj.optJSONObject("error")
                    val msg = errorObj?.optString("message")
                    if (!msg.isNullOrBlank()) {
                        when {
                            msg.contains("API key not valid", ignoreCase = true) ->
                                "API key is not valid. Please copy it from Google AI Studio."
                            msg.contains("quota", ignoreCase = true) ->
                                "Key is valid, but quota exceeded for this project."
                            else -> msg
                        }
                    } else {
                        "Verification failed (HTTP ${response.code}). Check your key."
                    }
                } catch (_: Exception) {
                    when (response.code) {
                        400 -> "Invalid API key format or arguments."
                        403 -> "API key is not valid or unauthorized."
                        429 -> "Too many requests. Please wait a moment."
                        else -> "Verification failed with status ${response.code}."
                    }
                }
                KeyValidationResult.Error(errorMessage)
            }
        } catch (e: java.net.UnknownHostException) {
            KeyValidationResult.Error("No internet connection. Please connect to the internet to test your key.")
        } catch (e: java.net.SocketTimeoutException) {
            KeyValidationResult.Error("Request timed out. Please check your connection and try again.")
        } catch (e: Exception) {
            KeyValidationResult.Error("Validation error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
