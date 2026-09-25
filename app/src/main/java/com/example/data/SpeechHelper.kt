package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechHelper(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context, this)
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        onListeningStateChanged(false)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            onSpeechResult(text)
                        }
                        onListeningStateChanged(false)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                // Configure gentle natural feminine cadence
                tts?.setPitch(1.15f)
                tts?.setSpeechRate(0.98f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
                onListeningStateChanged(true)
            } catch (_: Exception) {
                onListeningStateChanged(false)
            }
        } else {
            // Emulators or devices without SpeechRecognizer
            onListeningStateChanged(true)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        onListeningStateChanged(false)
    }

    fun speak(text: String) {
        if (isTtsReady && text.isNotBlank()) {
            tts?.stop()
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "mahi_response_${System.currentTimeMillis()}")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "mahi_response")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            onSpeakingStateChanged(false)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
