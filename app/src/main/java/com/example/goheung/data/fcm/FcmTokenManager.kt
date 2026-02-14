package com.example.goheung.data.fcm

import android.util.Log
import com.example.goheung.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) {
    companion object {
        private const val TAG = "FcmTokenManager"
    }

    suspend fun getToken(): Result<String> {
        return try {
            val token = firebaseMessaging.token.await()
            Log.d(TAG, "FCM token: $token")
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            Result.failure(e)
        }
    }

    suspend fun registerToken(): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val token = firebaseMessaging.token.await()
            Log.d(TAG, "FCM token retrieved: ${token.take(20)}...")

            saveTokenToFirestore(uid, token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
            Result.failure(e)
        }
    }

    suspend fun saveTokenToFirestore(uid: String, token: String): Result<Unit> {
        return try {
            userRepository.updateProfile(uid, mapOf("fcmToken" to token))
            Log.d(TAG, "FCM token saved to Firestore for user: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun clearToken(): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            userRepository.updateProfile(uid, mapOf("fcmToken" to FieldValue.delete()))
            Log.d(TAG, "FCM token cleared from Firestore for user: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FCM token", e)
            Result.failure(e)
        }
    }
}
