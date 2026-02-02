package com.example.goheung.presentation.friend

import com.example.goheung.data.model.User

data class UserSearchItem(
    val user: User,
    val isFriend: Boolean,
    val isRequestPending: Boolean
)
