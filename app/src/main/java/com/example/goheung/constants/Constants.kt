package com.example.goheung.constants

object Constants {
    // View Type IDs
    const val VIEW_TYPE_TITLE = 0
    const val VIEW_TYPE_FAIL = -100
    const val VIEW_TYPE_LOADING = -200
    const val VIEW_TYPE_EMPTY = -300

    const val VIEW_TYPE_CHAT_ROOM = 1
    const val VIEW_TYPE_SENT_MESSAGE = 2
    const val VIEW_TYPE_RECEIVED_MESSAGE = 3
    const val VIEW_TYPE_USER = 4

    // Model IDs
    const val KEY_FAIL_MODEL_ID = -100L
    const val KEY_LOADING_MODEL_ID = -200L
    const val KEY_EMPTY_MODEL_ID = -300L
    const val KEY_TITLE_MODEL_ID = -400L

    // Firebase Collection Names
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CHAT_ROOMS = "chatRooms"
    const val COLLECTION_MESSAGES = "messages"

    // Firebase Realtime DB Paths
    const val PATH_USER_PRESENCE = "userPresence"
    const val PATH_FCM_TOKENS = "fcmTokens"

    // FCM
    const val FCM_NOTIFICATION_CHANNEL_ID = "goheung_notifications"
    const val FCM_NOTIFICATION_ID = 2001
}
