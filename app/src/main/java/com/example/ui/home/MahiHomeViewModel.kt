package com.example.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionResult
import com.example.data.ChatMessage
import com.example.data.GeminiChatService
import com.example.data.MahiMemoryRepository
import com.example.data.MemoryItem
import com.example.data.PhoneActionManager
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
    val activeMood: String = "Cheerful",
    val moodSubtitle: String = "Always here for you",
    val accessMode: String = "FREE MODE",
    val accessStatusText: String = "Unlimited access",
    val inputText: String = "",
    val liveTranscription: String? = null,
    val lastAiResponse: String = "হাই! 😊 আমি Mahi। তোমার সাথে কথা বলতে ভালো লাগছে। বলো, কীভাবে তোমাকে সাহায্য করতে পারি?",
    val chatMessages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val scannedBitmap: Bitmap? = null,
    val scanResultText: String? = null,
    val isScanning: Boolean = false,
    val showVoiceSettingsDialog: Boolean = false,
    val showNotificationAlert: Boolean = false,
    val isSpeakingActive: Boolean = false
)

class MahiHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatService = GeminiChatService(application)
    private val weatherService = WeatherService()
    private val memoryRepo = MahiMemoryRepository(application)
    private val phoneActionManager = PhoneActionManager(application)

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
                        isSpeakingActive = isSpeaking,
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

        _uiState.update {
            it.copy(
                greetingPrefix = greeting,
                todayDateNumber = dayNumber,
                todayDayAndMonth = dayAndMonth,
                memories = memories,
                accessMode = "FREE MODE",
                accessStatusText = "Unlimited access",
                chatMessages = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "হাই! 😊 আমি Mahi। তোমার সাথে কথা বলতে ভালো লাগছে। বলো, কীভাবে তোমাকে সাহায্য করতে পারি?"
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

    /**
     * Toggles voice interaction with real voice barge-in support.
     * If Mahi is currently speaking, stops audio immediately and starts listening.
     */
    fun toggleVoiceInteraction() {
        val currentState = _uiState.value.orbState
        if (currentState == OrbState.SPEAKING) {
            // Immediate barge-in / interrupt: stop playback and start listening
            speechHelper?.stopSpeaking()
            speechHelper?.startListening()
            _uiState.update { it.copy(orbState = OrbState.LISTENING, isSpeakingActive = false) }
        } else if (currentState == OrbState.LISTENING) {
            speechHelper?.stopListening()
            _uiState.update { it.copy(orbState = OrbState.IDLE) }
        } else {
            speechHelper?.startListening()
            _uiState.update { it.copy(orbState = OrbState.LISTENING) }
        }
    }

    /**
     * Interrupts current speech immediately, stops audio, and switches directly to LISTENING.
     */
    fun interruptSpeaking() {
        speechHelper?.stopSpeaking()
        speechHelper?.startListening()
        _uiState.update { it.copy(orbState = OrbState.LISTENING, isSpeakingActive = false) }
    }

    /**
     * Sends user message to Mahi AI.
     * Flow: IDLE -> LISTENING -> PROCESSING -> SPEAKING -> IDLE
     * Produces natural female audio via Gemini native audio/TTS.
     */
    fun sendMessage(text: String = _uiState.value.inputText) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Stop any ongoing playback before processing new turn
        speechHelper?.stopSpeaking()

        // Check if user requested a phone control action (e.g. Open YouTube, Call, SMS, Wi-Fi, Camera, Alarm, Settings)
        val actionResult = phoneActionManager.processCommand(trimmed)
        if (actionResult !is ActionResult.NotACommand) {
            handleDeviceCommandResult(actionResult, trimmed)
            return
        }

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

                // Generate natural female speech using Gemini Native Audio / TTS
                speakNaturalMahiVoice(aiReply)

            }.onFailure { error ->
                val fallbackReply = "বলো, আমি শুনছি। Connection সমস্যা হয়েছে, কিন্তু আমি সাথে আছি। (${error.localizedMessage ?: "Network"})"
                val assistantMessage = ChatMessage(isUser = false, text = fallbackReply)
                _uiState.update {
                    it.copy(
                        lastAiResponse = fallbackReply,
                        chatMessages = it.chatMessages + assistantMessage,
                        orbState = OrbState.IDLE
                    )
                }
            }
        }
    }

    private fun handleDeviceCommandResult(actionResult: ActionResult, userPrompt: String) {
        val userMessage = ChatMessage(isUser = true, text = userPrompt)
        val replyText = when (actionResult) {
            is ActionResult.Success -> {
                actionResult.intent?.let { intent ->
                    try {
                        getApplication<Application>().startActivity(intent)
                    } catch (e: Exception) {
                        Log.w("MahiHomeViewModel", "Failed to launch intent: ${e.message}")
                    }
                }
                actionResult.message
            }
            is ActionResult.MissingPermission -> {
                actionResult.settingsIntent?.let { intent ->
                    try {
                        getApplication<Application>().startActivity(intent)
                    } catch (_: Exception) {}
                }
                actionResult.message
            }
            is ActionResult.AndroidRestriction -> {
                actionResult.intent?.let { intent ->
                    try {
                        getApplication<Application>().startActivity(intent)
                    } catch (_: Exception) {}
                }
                actionResult.message
            }
            is ActionResult.AppNotFound -> actionResult.message
            is ActionResult.Failed -> actionResult.message
            ActionResult.NotACommand -> ""
        }

        val assistantMessage = ChatMessage(isUser = false, text = replyText)
        _uiState.update {
            it.copy(
                inputText = "",
                liveTranscription = null,
                lastAiResponse = replyText,
                chatMessages = it.chatMessages + userMessage + assistantMessage,
                orbState = OrbState.SPEAKING
            )
        }

        speakNaturalMahiVoice(replyText)
    }

    private fun speakNaturalMahiVoice(replyText: String) {
        viewModelScope.launch {
            val speechResult = chatService.generateSpeech(replyText, voiceName = GeminiChatService.PERSISTENT_FEMALE_VOICE)
            speechResult.onSuccess { (audioBytes, mimeType) ->
                speechHelper?.playGeminiAudio(
                    audioBytes = audioBytes,
                    mimeType = mimeType,
                    onDone = {
                        _uiState.update {
                            if (it.orbState == OrbState.SPEAKING) it.copy(orbState = OrbState.IDLE) else it
                        }
                    }
                )
            }.onFailure { e ->
                Log.w("MahiHomeViewModel", "Gemini TTS fallback invoked: ${e.message}")
                speechHelper?.speakFallback(replyText) {
                    _uiState.update {
                        if (it.orbState == OrbState.SPEAKING) it.copy(orbState = OrbState.IDLE) else it
                    }
                }
            }
        }
    }

    fun triggerQuickAction(action: String) {
        when (action) {
            "Music" -> {
                sendMessage("আমাকে মন ভালো করার মতো সুন্দর কিছু গান বা মিউজিক সাজেস্ট করো।")
            }
            "Study" -> {
                sendMessage("চলো একসাথে পড়াশোনা করি। আমাকে ২৫ মিনিটের একটি পোমোডোরো লক্ষ্য দাও।")
            }
            "Journal" -> {
                sendMessage("আজকের ডায়েরির জন্য আমাকে একটি সুন্দর প্রশ্ন বা ভাবনা দাও।")
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
                prompt = "Please analyze this image carefully and provide a helpful, natural breakdown of what you see.",
                attachedBitmap = bitmap
            )
            result.onSuccess { analysis ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResultText = analysis
                    )
                }
                speakNaturalMahiVoice("আমি ছবিটি দেখেছি। এটি চমৎকার, নিচে বিস্তারিত তথ্য লিখে দিয়েছি।")
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
