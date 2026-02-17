package com.example.goheung.data.model

/**
 * Typing status model for Firebase Realtime Database
 * Represents a user's typing status in a chat room
 */
data class TypingStatus(
    val userId: String = "",
    val userName: String = "",
    val isTyping: Boolean = false,
    val timestamp: Long = 0L
)
