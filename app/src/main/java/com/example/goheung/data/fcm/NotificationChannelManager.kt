package com.example.goheung.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.goheung.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_CHAT = "goheung_chat_messages"
        const val CHANNEL_ID_BUS_ARRIVAL = "goheung_bus_arrival"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // 채팅 메시지 채널
            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                context.getString(R.string.notification_channel_chat_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_chat_description)
                enableVibration(true)
                enableLights(true)
            }

            // 버스 도착 알림 채널
            val busArrivalChannel = NotificationChannel(
                CHANNEL_ID_BUS_ARRIVAL,
                context.getString(R.string.notification_channel_bus_arrival_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_bus_arrival_description)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(listOf(chatChannel, busArrivalChannel))
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
