package com.example.goheung

import android.app.Application
import com.example.goheung.data.fcm.NotificationChannelManager
import com.example.goheung.data.repository.BusStopRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GoheungApplication : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var busStopRepository: BusStopRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()

        // 카카오맵 SDK 초기화 (API Key는 local.properties에서 관리)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 디버그 빌드에서 Crashlytics 비활성화
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // 버스 정류장 데이터 초기화
        applicationScope.launch {
            busStopRepository.initializeBusStops()
        }
    }
}
