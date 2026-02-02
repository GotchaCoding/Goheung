package com.example.goheung.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * ChatRoom model for Firestore
 * Represents a chat room with its metadata
 */
data class ChatRoom(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val createdBy: String = ""
) {
    companion object {
        const val COLLECTION_NAME = "chatRooms"
    }
}
