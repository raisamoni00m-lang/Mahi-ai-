package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherInfo(
    val temperature: String = "24°C",
    val condition: String = "Clear",
    val icon: String = "☁"
)

class WeatherService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun getCurrentWeather(): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            // Default coordinates (e.g. London / Global sample) for public Open-Meteo
            val url = "https://api.open-meteo.com/v1/forecast?latitude=37.7749&longitude=-122.4194&current=temperature_2m,weather_code"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val current = json.optJSONObject("current")
                if (current != null) {
                    val temp = current.optDouble("temperature_2m", 22.0)
                    val code = current.optInt("weather_code", 0)

                    val (conditionText, iconChar) = when (code) {
                        0 -> "Clear" to "☀️"
                        1, 2, 3 -> "Partly Cloudy" to "⛅"
                        45, 48 -> "Foggy" to "🌫️"
                        51, 53, 55, 61, 63, 65 -> "Rainy" to "🌧️"
                        71, 73, 75 -> "Snowy" to "❄️"
                        95, 96, 99 -> "Storm" to "⛈️"
                        else -> "Mild" to "☁"
                    }
                    return@withContext WeatherInfo(
                        temperature = "${temp.toInt()}°C",
                        condition = conditionText,
                        icon = iconChar
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        WeatherInfo("24°C", "Sunny", "☀️")
    }
}
