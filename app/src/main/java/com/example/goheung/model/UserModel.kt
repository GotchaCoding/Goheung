package com.example.goheung.model

import com.example.goheung.constants.Constants

data class UserModel(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val fcmToken: String = "",
    val status: String = "offline", // online, offline, away, leave
    val statusMessage: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    override val viewType: Int = Constants.VIEW_TYPE_USER,
    override val id: Long = uid.hashCode().toLong()
) : ItemModel(id, viewType)
