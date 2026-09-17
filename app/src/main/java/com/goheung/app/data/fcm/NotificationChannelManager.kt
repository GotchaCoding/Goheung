package com.goheung.app.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.goheung.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_CHAT = "goheung_chat_messages"

        /**
         * v1.7(셔틀버스) 시절 채널. 코드에서 지워도 기존 단말에는 남아
         * 시스템 알림 설정에 죽은 토글로 노출되므로 명시적으로 삭제한다.
         * 보급률이 충분해지면 이 상수와 삭제 호출을 함께 제거할 것.
         */
        private const val LEGACY_CHANNEL_ID_BUS_ARRIVAL = "goheung_bus_arrival"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID_BUS_ARRIVAL)

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

            notificationManager.createNotificationChannels(listOf(chatChannel))
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
