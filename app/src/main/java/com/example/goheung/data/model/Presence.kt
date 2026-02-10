package com.example.goheung.data.model

/**
 * Presence 상태 모델
 * Firebase Realtime Database의 presence/{uid}/ 경로에 저장
 */
data class Presence(
    val uid: String = "",
    val online: Boolean = false,
    val lastActive: Long = 0L,  // timestamp in milliseconds
    val inChat: Boolean = false,
    val chatRoomId: String? = null
)
