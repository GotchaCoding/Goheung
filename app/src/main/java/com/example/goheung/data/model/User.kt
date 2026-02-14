package com.example.goheung.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val department: String = "",
    val profileImageUrl: String? = null,
    val statusMessage: String? = null,
    val fcmToken: String? = null,
    val role: String? = null,  // "DRIVER" | "PASSENGER" | null
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION_NAME = "users"
    }
}
