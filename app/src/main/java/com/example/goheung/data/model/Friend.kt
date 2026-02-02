package com.example.goheung.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Friend(
    @DocumentId
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val profileImageUrl: String? = null,
    @ServerTimestamp
    val addedAt: Date? = null
) {
    companion object {
        const val SUBCOLLECTION_NAME = "friends"
    }
}
