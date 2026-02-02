package com.example.goheung.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Message model for Firestore
 * Represents a single message in a chat room
 */
data class Message(
    @DocumentId
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: MessageType = MessageType.TEXT,
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false
) {
    companion object {
        const val COLLECTION_NAME = "messages"
    }
}

enum class MessageType {
    TEXT,
    IMAGE,
    FILE
}
