package com.example.goheung.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.goheung.MainActivity
import com.example.goheung.R
import com.example.goheung.data.fcm.FcmTokenManager
import com.example.goheung.data.fcm.NotificationChannelManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GoheungMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "GoheungMessagingService"
        const val EXTRA_CHAT_ROOM_ID = "chatRoomId"
        const val EXTRA_CHAT_ROOM_NAME = "chatRoomName"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(20)}...")

        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            serviceScope.launch {
                fcmTokenManager.saveTokenToFirestore(uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val currentUserId = firebaseAuth.currentUser?.uid
        val senderId = remoteMessage.data["senderId"]

        // 자신이 보낸 메시지는 알림 표시하지 않음
        if (currentUserId != null && currentUserId == senderId) {
            Log.d(TAG, "Ignoring notification for own message")
            return
        }

        val chatRoomId = remoteMessage.data["chatRoomId"]
        val chatRoomName = remoteMessage.data["chatRoomName"]
        val senderName = remoteMessage.data["senderName"] ?: "알 수 없음"
        val messageText = remoteMessage.data["messageText"] ?: ""

        val title = chatRoomName ?: senderName
        val body = if (chatRoomName != null) "$senderName: $messageText" else messageText

        showNotification(title, body, chatRoomId, chatRoomName)
    }

    private fun showNotification(
        title: String,
        body: String,
        chatRoomId: String?,
        chatRoomName: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            chatRoomId?.let { putExtra(EXTRA_CHAT_ROOM_ID, it) }
            chatRoomName?.let { putExtra(EXTRA_CHAT_ROOM_NAME, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatRoomId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = chatRoomId?.hashCode() ?: System.currentTimeMillis().toInt()

        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }
}
