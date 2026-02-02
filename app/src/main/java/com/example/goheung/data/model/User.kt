package com.example.goheung.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val department: String = "",
    val profileImageUrl: String? = null,
    val statusMessage: String? = null,
    val fcmToken: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION_NAME = "users"
    }
}
