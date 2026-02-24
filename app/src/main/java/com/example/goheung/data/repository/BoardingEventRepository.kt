package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.local.BoardingEventDao
import com.example.goheung.data.local.BoardingEventEntity
import com.example.goheung.data.model.BoardingEvent
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing boarding event data
 */
@Singleton
class BoardingEventRepository @Inject constructor(
    private val boardingEventDao: BoardingEventDao,
    private val firebaseDatabase: FirebaseDatabase
) {
    companion object {
        private const val TAG = "BoardingEventRepository"
        private const val FIREBASE_PATH = "boarding_events"
    }

    /**
     * 탑승 이벤트 저장 (로컬 + Firebase)
     */
    suspend fun saveBoardingEvent(event: BoardingEvent) {
        Log.d(TAG, "Saving boarding event: ${event.id}")

        // 로컬 DB 저장
        boardingEventDao.insert(BoardingEventEntity.fromBoardingEvent(event))

        // Firebase 저장
        try {
            firebaseDatabase.reference
                .child(FIREBASE_PATH)
                .child(event.id)
                .setValue(event.toFirebaseMap())
                .await()
            Log.d(TAG, "Boarding event saved to Firebase: ${event.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save boarding event to Firebase", e)
        }
    }

    /**
     * 모든 탑승 이벤트 조회 (Flow)
     */
    fun observeAllBoardingEvents(): Flow<List<BoardingEvent>> {
        return boardingEventDao.getAllBoardingEvents().map { entities ->
            entities.map { it.toBoardingEvent() }
        }
    }

    /**
     * 특정 클러스터의 탑승 이벤트 조회 (Flow)
     */
    fun observeBoardingEventsByCluster(clusterId: String): Flow<List<BoardingEvent>> {
        return boardingEventDao.getBoardingEventsByCluster(clusterId).map { entities ->
            entities.map { it.toBoardingEvent() }
        }
    }

    /**
     * 클러스터에 할당되지 않은 이벤트 조회 (Flow)
     */
    fun observeUnclusteredEvents(): Flow<List<BoardingEvent>> {
        return boardingEventDao.getUnclusteredEvents().map { entities ->
            entities.map { it.toBoardingEvent() }
        }
    }

    /**
     * 특정 탑승 이벤트 조회
     */
    suspend fun getBoardingEventById(id: String): BoardingEvent? {
        return boardingEventDao.getBoardingEventById(id)?.toBoardingEvent()
    }

    /**
     * 클러스터 ID 업데이트
     */
    suspend fun updateClusterId(eventId: String, clusterId: String) {
        boardingEventDao.updateClusterId(eventId, clusterId)

        // Firebase 업데이트
        try {
            firebaseDatabase.reference
                .child(FIREBASE_PATH)
                .child(eventId)
                .child("clusterId")
                .setValue(clusterId)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update clusterId in Firebase", e)
        }
    }

    /**
     * 탑승 이벤트 삭제
     */
    suspend fun deleteBoardingEvent(id: String) {
        boardingEventDao.deleteById(id)

        // Firebase 삭제
        try {
            firebaseDatabase.reference
                .child(FIREBASE_PATH)
                .child(id)
                .removeValue()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete boarding event from Firebase", e)
        }
    }

    /**
     * 전체 탑승 이벤트 개수
     */
    suspend fun getTotalCount(): Int {
        return boardingEventDao.getCount()
    }

    /**
     * BoardingEvent를 Firebase용 Map으로 변환
     */
    private fun BoardingEvent.toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "passengerUid" to passengerUid,
            "driverUid" to driverUid,
            "lat" to lat,
            "lng" to lng,
            "timestamp" to timestamp,
            "clusterId" to clusterId
        )
    }
}
