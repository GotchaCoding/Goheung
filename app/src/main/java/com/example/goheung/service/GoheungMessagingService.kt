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
        const val EXTRA_NAVIGATE_TO = "navigateTo"
        private const val MESSAGE_TYPE_CHAT = "CHAT"
        private const val MESSAGE_TYPE_BUS_ARRIVAL = "BUS_ARRIVAL"
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

        val messageType = remoteMessage.data["type"] ?: MESSAGE_TYPE_CHAT

        when (messageType) {
            MESSAGE_TYPE_BUS_ARRIVAL -> handleBusArrivalNotification(remoteMessage)
            else -> handleChatNotification(remoteMessage)
        }
    }

    private fun handleChatNotification(remoteMessage: RemoteMessage) {
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

        showChatNotification(title, body, chatRoomId, chatRoomName)
    }

    private fun handleBusArrivalNotification(remoteMessage: RemoteMessage) {
        val driverName = remoteMessage.data["driverName"] ?: "버스"
        val distance = remoteMessage.data["distance"] ?: "500m"
        val busStopName = remoteMessage.data["busStopName"]

        val title = getString(R.string.notification_bus_arrival_title)
        val body = if (busStopName != null) {
            getString(R.string.notification_bus_arrival_body_with_stop, driverName, distance, busStopName)
        } else {
            getString(R.string.notification_bus_arrival_body, driverName, distance)
        }

        showBusArrivalNotification(title, body)
    }

    private fun showChatNotification(
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

    private fun showBusArrivalNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE_TO, "location")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            "bus_arrival".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_BUS_ARRIVAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = "bus_arrival".hashCode()

        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }
}
