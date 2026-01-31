package com.example.goheung.model

data class ChatMessageModel(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    override val viewType: Int = 0,
    override val id: Long = messageId.hashCode().toLong()
) : ItemModel(id, viewType)
