package com.google.devfest.safeaichat.data

data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val wasFiltered: Boolean = false,
    val filterReason: String? = null
)
