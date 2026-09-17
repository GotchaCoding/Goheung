package com.goheung.app

import android.app.Application
import com.goheung.app.data.fcm.NotificationChannelManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()

        // 디버그 빌드에서 Crashlytics 비활성화
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        cleanUpLegacyData()
    }

    /**
     * v1.7(셔틀버스) 시절 남은 로컬 데이터를 업그레이드 설치에서 1회 정리한다.
     * 위치/정류장 기능이 제거되면서 Room DB가 고아로 남기 때문.
     * 보급률이 충분해지면 이 메서드째로 제거할 것.
     */
    private fun cleanUpLegacyData() {
        applicationScope.launch {
            val prefs = getSharedPreferences(PREFS_MIGRATION, MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_LEGACY_ROOM_DELETED, false)) {
                deleteDatabase(LEGACY_ROOM_DB_NAME)
                prefs.edit().putBoolean(KEY_LEGACY_ROOM_DELETED, true).apply()
            }
        }
    }

    companion object {
        private const val PREFS_MIGRATION = "flowon_migration"
        private const val KEY_LEGACY_ROOM_DELETED = "legacy_room_deleted"
        private const val LEGACY_ROOM_DB_NAME = "goheung_database"
    }
}
