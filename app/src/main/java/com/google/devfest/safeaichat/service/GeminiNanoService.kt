package com.google.devfest.safeaichat.service

import android.content.Context
import com.google.devfest.safeaichat.data.SafetySettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Service for interacting with Gemini Nano on-device AI.
 *
 * NOTE: This is a demo implementation. In production, you would use the actual
 * Google AICore APIs for Gemini Nano integration.
 *
 * For the real implementation, see:
 * https://ai.google.dev/gemini-api/docs/models/gemini-nano
 */
class GeminiNanoService(private val context: Context) {

    private var safetySettings = SafetySettings()

    /**
     * Check if Gemini Nano is available on this device
     */
    suspend fun isAvailable(): Boolean {
        // In production: Check AICore availability
        // For demo: Simulate availability check
        delay(100)
        return true
    }

    /**
     * Initialize the Gemini Nano model
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            // In production: Initialize AICore and load model
            delay(500) // Simulate initialization
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update safety settings
     */
    fun updateSafetySettings(settings: SafetySettings) {
        safetySettings = settings
    }

    /**
     * Check if content should be filtered based on safety settings
     */
    private fun shouldFilterContent(input: String): Pair<Boolean, String?> {
        val lowerInput = input.lowercase()

        // Simple keyword-based filtering for demo purposes
        // In production: Use Gemini's built-in safety filters

        if (safetySettings.blockHarassment) {
            val harassmentKeywords = listOf("bully", "harass", "threaten", "attack")
            if (harassmentKeywords.any { lowerInput.contains(it) }) {
                return true to "Content blocked: Potential harassment detected"
            }
        }

        if (safetySettings.blockHateSpeech) {
            val hateKeywords = listOf("hate", "discriminate")
            if (hateKeywords.any { lowerInput.contains(it) }) {
                return true to "Content blocked: Potential hate speech detected"
            }
        }

        if (safetySettings.blockDangerousContent) {
            val dangerousKeywords = listOf("harm", "dangerous", "violence", "weapon")
            if (dangerousKeywords.any { lowerInput.contains(it) }) {
                return true to "Content blocked: Potentially dangerous content"
            }
        }

        return false to null
    }

    /**
     * Generate a response using Gemini Nano (streaming)
     */
    fun generateResponse(prompt: String): Flow<GenerateResult> = flow {
        // Check content safety first
        val (shouldFilter, filterReason) = shouldFilterContent(prompt)
        if (shouldFilter) {
            emit(GenerateResult.Filtered(filterReason ?: "Content blocked by safety filters"))
            return@flow
        }

        emit(GenerateResult.Processing)

        // Simulate on-device inference delay
        delay(300)

        // In production: Use actual Gemini Nano inference
        // For demo: Generate contextual responses
        val response = generateDemoResponse(prompt)

        // Stream the response word by word for better UX
        val words = response.split(" ")
        val chunks = mutableListOf<String>()

        for (word in words) {
            chunks.add(word)
            emit(GenerateResult.Chunk(chunks.joinToString(" ")))
            delay(50) // Simulate streaming
        }

        emit(GenerateResult.Complete(response))
    }

    /**
     * Generate demo responses that showcase on-device AI capabilities
     */
    private fun generateDemoResponse(prompt: String): String {
        val lowerPrompt = prompt.lowercase()

        return when {
            lowerPrompt.contains("devfest") || lowerPrompt.contains("gdg") ->
                "GDG DevFest 2025 is happening right now in Johannesburg at The Tryst, Sandton! DevFest is Google's annual community-led developer festival, bringing together developers to learn about the latest Google technologies, share knowledge, and network with the local tech community."

            lowerPrompt.contains("johannesburg") || lowerPrompt.contains("sandton") || lowerPrompt.contains("tryst") ->
                "We're at The Tryst in Sandton for GDG DevFest 2025! It's an amazing venue for learning about cutting-edge technologies like on-device AI, Android development, and Google Cloud. Are you attending today?"

            lowerPrompt.contains("event") || lowerPrompt.contains("conference") ->
                "This demo is being presented at GDG DevFest 2025 in Johannesburg! DevFest brings together developers from across the region to explore the latest in Google technologies, including exciting innovations like Gemini Nano that I'm powered by."

            lowerPrompt.contains("hello") || lowerPrompt.contains("hi") ->
                "Hello! I'm running entirely on your device using Gemini Nano. Your data never leaves your phone. How can I help you today?"

            lowerPrompt.contains("offline") || lowerPrompt.contains("internet") ->
                "Great question! I work completely offline because I'm powered by Gemini Nano, which runs directly on your device. No internet connection required, and your conversations stay 100% private."

            lowerPrompt.contains("privacy") || lowerPrompt.contains("private") ->
                "Privacy is my core feature! Since I run on-device with Gemini Nano, your messages never leave your phone. No servers, no cloud storage, no data collection. Everything stays with you."

            lowerPrompt.contains("safe") || lowerPrompt.contains("safety") ->
                "Safety is built into Gemini Nano. I use multiple safety filters to block harmful content, including harassment, hate speech, and dangerous instructions. You can see these filters in action in the settings."

            lowerPrompt.contains("fast") || lowerPrompt.contains("speed") ->
                "On-device AI is incredibly fast! There's no network latency since everything runs locally. Try turning on airplane mode - I'll still work perfectly!"

            lowerPrompt.contains("how") && lowerPrompt.contains("work") ->
                "I'm powered by Gemini Nano, Google's on-device AI model. It runs through Android's AICore, processing everything locally on your device. This means low latency, complete privacy, and offline capability."

            else ->
                "I'm an on-device AI assistant powered by Gemini Nano. I can answer questions, have conversations, and help with various tasks - all while keeping your data private and working offline. What would you like to know?"
        }
    }

    /**
     * Clear any cached data (for privacy)
     */
    suspend fun clearData() {
        // In production: Clear AICore cache/history
        delay(100)
    }
}

sealed class GenerateResult {
    object Processing : GenerateResult()
    data class Chunk(val text: String) : GenerateResult()
    data class Complete(val text: String) : GenerateResult()
    data class Filtered(val reason: String) : GenerateResult()
    data class Error(val message: String) : GenerateResult()
}
