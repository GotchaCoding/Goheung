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
    val createdBy: String = "",
    val favoriteBy: List<String> = emptyList(), // 즐겨찾기한 사용자 UID 목록
    val type: ChatRoomType = ChatRoomType.GROUP, // 채팅방 타입 (DM or GROUP)
    val hiddenBy: List<String> = emptyList() // 채팅방을 숨긴 사용자 UID 목록 (DM 전용)
) {
    companion object {
        const val COLLECTION_NAME = "chatRooms"
    }

    fun isFavoriteBy(userId: String): Boolean = userId in favoriteBy

    fun isHiddenBy(userId: String): Boolean = userId in hiddenBy

    fun isDM(): Boolean = type == ChatRoomType.DM

    fun isGroup(): Boolean = type == ChatRoomType.GROUP
}

/**
 * 채팅방 타입
 * 생성 시점에 결정되며 이후 변경되지 않음
 */
enum class ChatRoomType {
    DM,     // 1:1 Direct Message
    GROUP   // 그룹 채팅 (초기 인원 무관)
}
