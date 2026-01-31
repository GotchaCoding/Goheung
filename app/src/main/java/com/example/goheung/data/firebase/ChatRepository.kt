package com.example.goheung.data.firebase

import com.example.goheung.base.Resource
import com.example.goheung.model.ChatMessageModel
import com.example.goheung.model.ChatRoomModel
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(userId: String): Flow<List<ChatRoomModel>>
    fun getMessages(chatRoomId: String): Flow<List<ChatMessageModel>>
    fun sendMessage(chatRoomId: String, message: ChatMessageModel): Flow<Resource<Unit>>
    fun createChatRoom(participants: List<String>, participantNames: Map<String, String>): Flow<Resource<String>>
}
