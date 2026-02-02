package com.example.goheung.data.repository

import com.example.goheung.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersCollection = firestore.collection(User.COLLECTION_NAME)

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
                ?: return Result.failure(Exception("사용자를 찾을 수 없습니다"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(uid: String, fields: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(uid).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllUsers(excludeUid: String? = null): Flow<Result<List<User>>> = callbackFlow {
        val listenerRegistration = usersCollection
            .orderBy("displayName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                        .filter { it.uid != excludeUid }
                    trySend(Result.success(users))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getUsers(uids: List<String>): Result<List<User>> {
        if (uids.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val users = mutableListOf<User>()

            uids.chunked(10).forEach { chunk ->
                val snapshot = usersCollection
                    .whereIn("uid", chunk)
                    .get()
                    .await()
                users.addAll(snapshot.toObjects(User::class.java))
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
