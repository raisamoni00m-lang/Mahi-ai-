package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Speech management for Mahi AI.
 * Plays natural female Gemini audio with real voice barge-in and instant interruption.
 * Captures Bangla, English, and mixed Bangla-English speech.
 */
class SpeechHelper(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private val audioPlayer = MahiAudioPlayer(context)
    private var speechRecognizer: SpeechRecognizer? = null
    private var fallbackTts: TextToSpeech? = null
    private var isFallbackTtsReady = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var isSpeaking = false

    init {
        initRecognizer()
        // Initialize fallback TTS in background in case network is disconnected
        try {
            fallbackTts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.w("SpeechHelper", "Fallback TTS init error", e)
        }
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onListeningStateChanged(true)
                        }

                        override fun onBeginningOfSpeech() {
                            // REAL BARGE-IN: If user begins speaking while Mahi is speaking, interrupt immediately!
                            if (isSpeaking || audioPlayer.isPlaying()) {
                                stopSpeaking()
                                isListening = true
                                onListeningStateChanged(true)
                            }
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Interruption trigger if significant user voice energy detected while speaking
                            if (rmsdB > 6.0f && (isSpeaking || audioPlayer.isPlaying())) {
                                stopSpeaking()
                                isListening = true
                                onListeningStateChanged(true)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            onListeningStateChanged(false)
                        }

                        override fun onError(error: Int) {
                            if (!isSpeaking) {
                                isListening = false
                                onListeningStateChanged(false)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull().orEmpty().trim()
                            if (isSpeaking || audioPlayer.isPlaying()) {
                                stopSpeaking()
                            }
                            isListening = false
                            if (text.isNotBlank()) {
                                onSpeechResult(text)
                            }
                            onListeningStateChanged(false)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull().orEmpty().trim()
                            if (text.isNotBlank()) {
                                // Real barge-in: If user speaks any word or explicitly "থামো" / "stop"
                                if (isSpeaking || audioPlayer.isPlaying()) {
                                    stopSpeaking()
                                    isListening = true
                                    onListeningStateChanged(true)
                                }
                                onPartialResult(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.w("SpeechHelper", "SpeechRecognizer creation failed", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = fallbackTts?.setLanguage(Locale("bn", "BD"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                fallbackTts?.setLanguage(Locale.US)
            }
            isFallbackTtsReady = true
            fallbackTts?.setPitch(1.18f)
            fallbackTts?.setSpeechRate(0.96f)

            fallbackTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                }
            })
        }
    }

    /**
     * Plays high-fidelity natural female speech from Gemini Native Audio / TTS.
     */
    fun playGeminiAudio(
        audioBytes: ByteArray,
        mimeType: String? = null,
        onDone: () -> Unit = {}
    ) {
        stopSpeaking()
        isSpeaking = true
        onSpeakingStateChanged(true)

        scope.launch {
            audioPlayer.playAudio(
                audioBytes = audioBytes,
                mimeType = mimeType,
                onStart = {
                    isSpeaking = true
                    onSpeakingStateChanged(true)
                    try {
                        startBargeInListening()
                    } catch (_: Exception) {}
                },
                onDone = {
                    isSpeaking = false
                    onSpeakingStateChanged(false)
                    stopListening()
                    onDone()
                }
            )
        }
    }

    private fun startBargeInListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Starts listening with natural support for Bangla, English, and mixed speech.
     */
    fun startListening() {
        // Immediate barge-in: stop any ongoing playback
        stopSpeaking()

        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Support Bangla (Bangladesh) and English seamlessly
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                onListeningStateChanged(true)
            } catch (e: Exception) {
                Log.w("SpeechHelper", "startListening error", e)
                onListeningStateChanged(false)
            }
        } else {
            onListeningStateChanged(true)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
        onListeningStateChanged(false)
    }

    /**
     * Instantly stops audio playback for barge-in or interruptions.
     */
    fun stopSpeaking() {
        audioPlayer.stop()
        try {
            fallbackTts?.stop()
        } catch (_: Exception) {}
        isSpeaking = false
        onSpeakingStateChanged(false)
    }

    fun speakFallback(text: String, onDone: () -> Unit = {}) {
        if (isFallbackTtsReady && text.isNotBlank()) {
            stopSpeaking()
            isSpeaking = true
            onSpeakingStateChanged(true)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "mahi_voice_${System.currentTimeMillis()}")
            }
            fallbackTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "mahi_voice")
        } else {
            onDone()
        }
    }

    fun release() {
        try {
            audioPlayer.release()
            speechRecognizer?.destroy()
            fallbackTts?.stop()
            fallbackTts?.shutdown()
        } catch (_: Exception) {}
    }
}
