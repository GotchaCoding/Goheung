package com.example.goheung

import android.app.Application
import com.example.goheung.data.fcm.NotificationChannelManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

        // 카카오맵 SDK 초기화 (API Key는 local.properties에서 관리)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 디버그 빌드에서 Crashlytics 비활성화
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
