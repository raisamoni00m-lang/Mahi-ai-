package com.example.ui.home

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.GeminiChatService
import com.example.data.MahiMemoryRepository
import com.example.data.MemoryItem
import com.example.data.SpeechHelper
import com.example.data.WeatherInfo
import com.example.data.WeatherService
import com.example.ui.home.components.OrbState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BottomTab {
    HOME,
    SCAN,
    MEMORIES,
    CHAT
}

data class HomeUiState(
    val currentTab: BottomTab = BottomTab.HOME,
    val orbState: OrbState = OrbState.IDLE,
    val greetingPrefix: String = "Good evening,",
    val greetingName: String = "there",
    val greetingSubtitle: String = "Mahi AI is ready to help you.",
    val todayDateNumber: String = "25",
    val todayDayAndMonth: String = "Fri, Sep",
    val weather: WeatherInfo = WeatherInfo("24°C", "Sunny", "☀️"),
    val energyCount: Int = 1,
    val isLicenseActive: Boolean = false,
    val licenseStatusText: String = "Free mode • 10:00 min left today",
    val inputText: String = "",
    val liveTranscription: String? = null,
    val lastAiResponse: String = "How can I help you today?",
    val chatMessages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val scannedBitmap: Bitmap? = null,
    val scanResultText: String? = null,
    val isScanning: Boolean = false,
    val showLicenseDialog: Boolean = false,
    val showVoiceSettingsDialog: Boolean = false,
    val showNotificationAlert: Boolean = false,
    val activeMood: String = "Warm",
    val moodSubtitle: String = "All good"
)

class MahiHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatService = GeminiChatService(application)
    private val weatherService = WeatherService()
    private val memoryRepo = MahiMemoryRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var speechHelper: SpeechHelper? = null

    init {
        speechHelper = SpeechHelper(
            context = application,
            onSpeechResult = { recognizedText ->
                _uiState.update { it.copy(liveTranscription = recognizedText) }
                if (recognizedText.isNotBlank()) {
                    sendMessage(recognizedText)
                }
            },
            onPartialResult = { partial ->
                _uiState.update { it.copy(liveTranscription = partial) }
            },
            onListeningStateChanged = { isListening ->
                _uiState.update {
                    it.copy(
                        orbState = if (isListening) OrbState.LISTENING else if (it.orbState == OrbState.LISTENING) OrbState.IDLE else it.orbState
                    )
                }
            },
            onSpeakingStateChanged = { isSpeaking ->
                _uiState.update {
                    it.copy(
                        orbState = if (isSpeaking) OrbState.SPEAKING else if (it.orbState == OrbState.SPEAKING) OrbState.IDLE else it.orbState
                    )
                }
            }
        )

        loadInitialData()
    }

    private fun loadInitialData() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..21 -> "Good evening,"
            else -> "Good night,"
        }

        val date = Date()
        val dayNumber = SimpleDateFormat("dd", Locale.getDefault()).format(date)
        val dayAndMonth = SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(date)

        val memories = memoryRepo.getMemories()
        val energy = memoryRepo.getEnergy()
        val isLicenseActive = memoryRepo.isLicenseActive()

        _uiState.update {
            it.copy(
                greetingPrefix = greeting,
                todayDateNumber = dayNumber,
                todayDayAndMonth = dayAndMonth,
                memories = memories,
                energyCount = energy,
                isLicenseActive = isLicenseActive,
                licenseStatusText = if (isLicenseActive) "Unlimited License Active" else "Free mode • 10:00 min left today",
                chatMessages = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "Hello! I'm Mahi AI, your personal companion. You can ask me anything or tap the mic to speak."
                    )
                )
            )
        }

        // Fetch real weather data
        viewModelScope.launch {
            val weather = weatherService.getCurrentWeather()
            _uiState.update { it.copy(weather = weather) }
        }
    }

    fun onTabSelected(tab: BottomTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleVoiceInteraction() {
        val currentState = _uiState.value.orbState
        if (currentState == OrbState.LISTENING) {
            speechHelper?.stopListening()
        } else if (currentState == OrbState.SPEAKING) {
            speechHelper?.stopSpeaking()
        } else {
            speechHelper?.startListening()
        }
    }

    fun sendMessage(text: String = _uiState.value.inputText) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val updatedHistory = _uiState.value.chatMessages + userMessage

        _uiState.update {
            it.copy(
                inputText = "",
                liveTranscription = null,
                chatMessages = updatedHistory,
                orbState = OrbState.THINKING
            )
        }

        viewModelScope.launch {
            val result = chatService.sendMessage(prompt = trimmed, history = updatedHistory)
            result.onSuccess { aiReply ->
                val assistantMessage = ChatMessage(isUser = false, text = aiReply)
                _uiState.update {
                    it.copy(
                        lastAiResponse = aiReply,
                        chatMessages = it.chatMessages + assistantMessage,
                        orbState = OrbState.SPEAKING
                    )
                }
                // Speak response with natural feminine voice
                speechHelper?.speak(aiReply)
            }.onFailure { error ->
                val fallbackReply = "I encountered an issue: ${error.localizedMessage ?: "Network error"}. Please check your connection or Gemini key in Setup."
                val assistantMessage = ChatMessage(isUser = false, text = fallbackReply)
                _uiState.update {
                    it.copy(
                        lastAiResponse = fallbackReply,
                        chatMessages = it.chatMessages + assistantMessage,
                        orbState = OrbState.IDLE
                    )
                }
                speechHelper?.speak(fallbackReply)
            }
        }
    }

    fun triggerQuickAction(action: String) {
        when (action) {
            "Music" -> {
                sendMessage("Recommend a great soothing music playlist or track for relaxing right now.")
            }
            "Study" -> {
                sendMessage("Let's do a productive study session. Give me a 25-minute Pomodoro focus goal.")
            }
            "Journal" -> {
                sendMessage("Give me a thoughtful daily reflection prompt for my personal journal.")
            }
        }
    }

    fun onScanImage(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                scannedBitmap = bitmap,
                isScanning = true,
                scanResultText = "Analyzing image with Gemini Vision..."
            )
        }

        viewModelScope.launch {
            val result = chatService.sendMessage(
                prompt = "Please analyze this image carefully and provide a helpful, detailed breakdown of what you see.",
                attachedBitmap = bitmap
            )
            result.onSuccess { analysis ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResultText = analysis
                    )
                }
                speechHelper?.speak("I've analyzed the image. Here is what I found.")
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResultText = "Analysis failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun addMemory(title: String, category: String) {
        memoryRepo.addMemory(title, category)
        _uiState.update { it.copy(memories = memoryRepo.getMemories()) }
    }

    fun deleteMemory(id: String) {
        memoryRepo.deleteMemory(id)
        _uiState.update { it.copy(memories = memoryRepo.getMemories()) }
    }

    fun activateLicense() {
        memoryRepo.activateLicense()
        _uiState.update {
            it.copy(
                isLicenseActive = true,
                energyCount = 99,
                licenseStatusText = "Unlimited License Active",
                showLicenseDialog = false
            )
        }
    }

    fun toggleLicenseDialog(show: Boolean) {
        _uiState.update { it.copy(showLicenseDialog = show) }
    }

    fun toggleVoiceSettings(show: Boolean) {
        _uiState.update { it.copy(showVoiceSettingsDialog = show) }
    }

    fun toggleNotificationAlert(show: Boolean) {
        _uiState.update { it.copy(showNotificationAlert = show) }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.release()
    }
}
