package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiKeyValidator
import com.example.data.KeyValidationResult
import com.example.data.SecureKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val apiKey: String = "",
    val isKeyObscured: Boolean = true,
    val isValidating: Boolean = false,
    val isKeyValid: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val hasPreviouslySavedKey: Boolean = false,
    val isComplete: Boolean = false
)

class SetupMahiViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = SecureKeyStorage(application)
    private val validator = GeminiKeyValidator()

    private val _uiState = MutableStateFlow(
        SetupUiState(
            hasPreviouslySavedKey = storage.hasSavedKey()
        )
    )
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        // Pre-fill or check if existing key was saved
        val existingKey = storage.getApiKey()
        if (!existingKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    hasPreviouslySavedKey = true
                )
            }
        }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update {
            it.copy(
                apiKey = newKey,
                // Reset validation status if user edits key
                isKeyValid = false,
                feedbackMessage = null,
                isError = false
            )
        }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyObscured = !it.isKeyObscured) }
    }

    fun testKey() {
        val currentKey = _uiState.value.apiKey.trim()
        if (currentKey.isEmpty()) {
            _uiState.update {
                it.copy(
                    isError = true,
                    feedbackMessage = "Please enter an API key first."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isValidating = true,
                    feedbackMessage = null,
                    isError = false
                )
            }

            val result = validator.validateKey(currentKey)

            when (result) {
                is KeyValidationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            isKeyValid = true,
                            isError = false,
                            feedbackMessage = result.message
                        )
                    }
                }
                is KeyValidationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            isKeyValid = false,
                            isError = true,
                            feedbackMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun saveAndContinue() {
        val key = _uiState.value.apiKey.trim()
        if (key.isNotEmpty() && _uiState.value.isKeyValid) {
            storage.saveApiKey(key)
            _uiState.update {
                it.copy(
                    isComplete = true,
                    hasPreviouslySavedKey = true
                )
            }
        }
    }

    fun skip() {
        if (storage.hasSavedKey()) {
            _uiState.update { it.copy(isComplete = true) }
        } else {
            // User chose to skip even without a stored key
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    fun resetToSetup() {
        _uiState.update { it.copy(isComplete = false) }
    }
}
