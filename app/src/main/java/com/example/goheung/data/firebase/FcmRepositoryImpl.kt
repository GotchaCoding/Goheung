package com.example.goheung.data.firebase

import com.example.goheung.base.BaseRepository
import com.example.goheung.base.Resource
import com.example.goheung.constants.Constants
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FcmRepositoryImpl @Inject constructor(
    private val realtimeDb: FirebaseDatabase,
    private val firebaseMessaging: FirebaseMessaging
) : BaseRepository(), FcmRepository {

    override fun saveToken(userId: String, token: String): Flow<Resource<Unit>> {
        return callFirebase {
            realtimeDb.getReference(Constants.PATH_FCM_TOKENS)
                .child(userId)
                .setValue(token)
                .await()
        }
    }

    override fun getToken(): Flow<Resource<String>> {
        return callFirebase {
            firebaseMessaging.token.await()
        }
    }
}
