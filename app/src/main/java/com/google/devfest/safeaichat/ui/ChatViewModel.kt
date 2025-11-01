package com.google.devfest.safeaichat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.devfest.safeaichat.data.Message
import com.google.devfest.safeaichat.data.SafetySettings
import com.google.devfest.safeaichat.service.GenerateResult
import com.google.devfest.safeaichat.service.GeminiNanoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val error: String? = null,
    val safetySettings: SafetySettings = SafetySettings(),
    val showPrivacyIndicator: Boolean = true,
    val showOfflineIndicator: Boolean = true
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiNanoService(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        initializeGemini()
    }

    private fun initializeGemini() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = geminiService.initialize()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isInitialized = true,
                    isLoading = false,
                    messages = listOf(
                        Message(
                            id = UUID.randomUUID().toString(),
                            text = "👋 Hi! I'm powered by Gemini Nano running entirely on your device. Your conversations are private and work offline. Try asking me anything!",
                            isUser = false
                        )
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize AI model"
                )
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true
        )

        viewModelScope.launch {
            var currentResponse = ""
            val responseId = UUID.randomUUID().toString()

            geminiService.generateResponse(text).collect { result ->
                when (result) {
                    is GenerateResult.Processing -> {
                        // Show loading state
                    }

                    is GenerateResult.Chunk -> {
                        currentResponse = result.text
                        updateOrAddResponse(responseId, currentResponse, false)
                    }

                    is GenerateResult.Complete -> {
                        updateOrAddResponse(responseId, result.text, false, isComplete = true)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }

                    is GenerateResult.Filtered -> {
                        val filteredMessage = Message(
                            id = responseId,
                            text = "⚠️ ${result.reason}\n\nThis message was blocked to keep our conversation safe and respectful.",
                            isUser = false,
                            wasFiltered = true,
                            filterReason = result.reason
                        )
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + filteredMessage,
                            isLoading = false
                        )
                    }

                    is GenerateResult.Error -> {
                        val errorMessage = Message(
                            id = responseId,
                            text = "Sorry, I encountered an error: ${result.message}",
                            isUser = false
                        )
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + errorMessage,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun updateOrAddResponse(
        id: String,
        text: String,
        wasFiltered: Boolean,
        isComplete: Boolean = false
    ) {
        val messages = _uiState.value.messages.toMutableList()
        val existingIndex = messages.indexOfLast { it.id == id }

        val message = Message(
            id = id,
            text = text,
            isUser = false,
            wasFiltered = wasFiltered
        )

        if (existingIndex != -1) {
            messages[existingIndex] = message
        } else {
            messages.add(message)
        }

        _uiState.value = _uiState.value.copy(
            messages = messages,
            isLoading = !isComplete
        )
    }

    fun updateSafetySettings(settings: SafetySettings) {
        geminiService.updateSafetySettings(settings)
        _uiState.value = _uiState.value.copy(safetySettings = settings)
    }

    fun clearChat() {
        viewModelScope.launch {
            // Clear local messages
            _uiState.value = _uiState.value.copy(
                messages = listOf(
                    Message(
                        id = UUID.randomUUID().toString(),
                        text = "✅ Chat history cleared. Your data has been deleted from this device.",
                        isUser = false
                    )
                )
            )

            // Clear any cached data in the service
            geminiService.clearData()
        }
    }

    fun togglePrivacyIndicator() {
        _uiState.value = _uiState.value.copy(
            showPrivacyIndicator = !_uiState.value.showPrivacyIndicator
        )
    }
}
