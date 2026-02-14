package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.UserLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "LocationRepository"
        private const val LOCATIONS_PATH = "locations"
    }

    /**
     * 사용자 위치 업데이트
     */
    suspend fun updateLocation(
        uid: String,
        lat: Double,
        lng: Double,
        role: String,
        displayName: String
    ): Result<Unit> {
        return try {
            val locationRef = database.getReference("$LOCATIONS_PATH/$uid")
            val locationData = mapOf(
                "uid" to uid,
                "lat" to lat,
                "lng" to lng,
                "role" to role,
                "timestamp" to System.currentTimeMillis(),
                "displayName" to displayName
            )
            locationRef.setValue(locationData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location", e)
            Result.failure(e)
        }
    }

    /**
     * 모든 사용자 위치 실시간 구독
     */
    fun observeAllLocations(): Flow<List<UserLocation>> = callbackFlow {
        val locationsRef = database.getReference(LOCATIONS_PATH)

        val listener = locationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = snapshot.children.mapNotNull {
                    it.getValue(UserLocation::class.java)
                }
                trySend(locations)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to observe locations", error.toException())
                close(error.toException())
            }
        })

        awaitClose {
            locationsRef.removeEventListener(listener)
        }
    }

    /**
     * 연결 해제 시 위치 데이터 자동 삭제 설정
     */
    fun setupDisconnectHandler(uid: String) {
        val locationRef = database.getReference("$LOCATIONS_PATH/$uid")
        val connectedRef = database.getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase - setup disconnect handler")
                    // 연결 해제 시 위치 데이터 자동 삭제
                    locationRef.onDisconnect().removeValue()
                } else {
                    Log.d(TAG, "Disconnected from Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Connection listener error", error.toException())
            }
        })
    }

    /**
     * 위치 데이터 수동 삭제
     */
    suspend fun clearLocation(uid: String): Result<Unit> {
        return try {
            val locationRef = database.getReference("$LOCATIONS_PATH/$uid")
            locationRef.removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear location", e)
            Result.failure(e)
        }
    }
}
