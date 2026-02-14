package com.example.goheung

import android.app.Application
import com.example.goheung.data.fcm.NotificationChannelManager
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GoheungApplication : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()

        // 카카오맵 SDK 초기화
        KakaoMapSdk.init(this, getString(R.string.kakao_native_app_key))
    }
}
