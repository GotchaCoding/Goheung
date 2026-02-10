package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.Presence
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "PresenceRepository"
        private const val PRESENCE_PATH = "presence"
    }

    /**
     * 현재 사용자를 온라인으로 설정
     */
    suspend fun setOnline(uid: String): Result<Unit> {
        return try {
            val presenceRef = database.getReference("$PRESENCE_PATH/$uid")
            val presenceData = mapOf(
                "uid" to uid,
                "online" to true,
                "lastActive" to ServerValue.TIMESTAMP,
                "inChat" to false,
                "chatRoomId" to null
            )
            presenceRef.setValue(presenceData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set online", e)
            Result.failure(e)
        }
    }

    /**
     * 연결 해제 시 자동 오프라인 처리 설정
     */
    fun setupDisconnectHandler(uid: String) {
        val presenceRef = database.getReference("$PRESENCE_PATH/$uid")
        val connectedRef = database.getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase")
                    // 연결됨
                    presenceRef.child("online").setValue(true)
                    presenceRef.child("lastActive").setValue(ServerValue.TIMESTAMP)

                    // 연결 해제 시 자동 처리
                    presenceRef.child("online").onDisconnect().setValue(false)
                    presenceRef.child("lastActive").onDisconnect().setValue(ServerValue.TIMESTAMP)
                    presenceRef.child("inChat").onDisconnect().setValue(false)
                    presenceRef.child("chatRoomId").onDisconnect().setValue(null)
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
     * 특정 사용자의 Presence 실시간 감시
     */
    fun observePresence(uid: String): Flow<Presence?> = callbackFlow {
        val presenceRef = database.getReference("$PRESENCE_PATH/$uid")

        val listener = presenceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presence = snapshot.getValue(Presence::class.java)
                trySend(presence)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to observe presence for $uid", error.toException())
                close(error.toException())
            }
        })

        awaitClose {
            presenceRef.removeEventListener(listener)
        }
    }

    /**
     * 채팅방 입장/퇴장 상태 업데이트
     */
    suspend fun updateChatStatus(
        uid: String,
        inChat: Boolean,
        chatRoomId: String?
    ): Result<Unit> {
        return try {
            val presenceRef = database.getReference("$PRESENCE_PATH/$uid")
            val updates = mapOf(
                "inChat" to inChat,
                "chatRoomId" to chatRoomId,
                "lastActive" to ServerValue.TIMESTAMP
            )
            presenceRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update chat status", e)
            Result.failure(e)
        }
    }

    /**
     * 오프라인으로 설정
     */
    suspend fun setOffline(uid: String): Result<Unit> {
        return try {
            val presenceRef = database.getReference("$PRESENCE_PATH/$uid")
            val updates = mapOf(
                "online" to false,
                "lastActive" to ServerValue.TIMESTAMP,
                "inChat" to false,
                "chatRoomId" to null
            )
            presenceRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set offline", e)
            Result.failure(e)
        }
    }
}
