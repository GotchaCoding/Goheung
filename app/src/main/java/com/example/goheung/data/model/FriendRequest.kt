package com.example.goheung.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromDisplayName: String = "",
    val fromProfileImageUrl: String? = null,
    val status: String = FriendRequestStatus.PENDING.value,
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION_NAME = "friendRequests"
    }
}

enum class FriendRequestStatus(val value: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected")
}
