package com.google.devfest.safeaichat.data

enum class SafetyLevel {
    STRICT,      // Block most potentially harmful content
    MODERATE,    // Balance between safety and utility
    PERMISSIVE   // Allow most content (for demos/testing only)
}

data class SafetySettings(
    val level: SafetyLevel = SafetyLevel.STRICT,
    val blockHarassment: Boolean = true,
    val blockHateSpeech: Boolean = true,
    val blockSexualContent: Boolean = true,
    val blockDangerousContent: Boolean = true
)
