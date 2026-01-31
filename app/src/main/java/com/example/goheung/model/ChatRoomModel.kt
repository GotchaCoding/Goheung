package com.example.goheung.model

import com.example.goheung.constants.Constants

data class ChatRoomModel(
    val chatRoomId: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    override val viewType: Int = Constants.VIEW_TYPE_CHAT_ROOM,
    override val id: Long = chatRoomId.hashCode().toLong()
) : ItemModel(id, viewType)
