package com.example.goheung.data.repository

import com.example.goheung.data.model.Friend
import com.example.goheung.data.model.FriendRequest
import com.example.goheung.data.model.FriendRequestStatus
import com.example.goheung.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getFriends(userId: String): Flow<Result<List<Friend>>> = callbackFlow {
        val listener = firestore.collection(User.COLLECTION_NAME)
            .document(userId)
            .collection(Friend.SUBCOLLECTION_NAME)
            .orderBy("displayName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val friends = snapshot.toObjects(Friend::class.java)
                    trySend(Result.success(friends))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendFriendRequest(
        fromUid: String,
        toUid: String,
        fromDisplayName: String,
        fromProfileImageUrl: String?
    ): Result<Unit> = try {
        val existing = firestore.collection(FriendRequest.COLLECTION_NAME)
            .whereEqualTo("fromUid", fromUid)
            .whereEqualTo("toUid", toUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.value)
            .get()
            .await()

        if (existing.isEmpty) {
            val request = FriendRequest(
                fromUid = fromUid,
                toUid = toUid,
                fromDisplayName = fromDisplayName,
                fromProfileImageUrl = fromProfileImageUrl
            )
            firestore.collection(FriendRequest.COLLECTION_NAME)
                .add(request)
                .await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getPendingRequests(userId: String): Flow<Result<List<FriendRequest>>> = callbackFlow {
        val listener = firestore.collection(FriendRequest.COLLECTION_NAME)
            .whereEqualTo("toUid", userId)
            .whereEqualTo("status", FriendRequestStatus.PENDING.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.toObjects(FriendRequest::class.java)
                    trySend(Result.success(requests))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> = try {
        val batch = firestore.batch()

        // Update request status
        val requestRef = firestore.collection(FriendRequest.COLLECTION_NAME)
            .document(request.id)
        batch.update(requestRef, "status", FriendRequestStatus.ACCEPTED.value)

        // Get the requester's user info for the reverse friend entry
        val toUserSnapshot = firestore.collection(User.COLLECTION_NAME)
            .document(request.toUid)
            .get()
            .await()
        val toUser = toUserSnapshot.toObject(User::class.java)

        // Add friend to receiver's list
        val receiverFriendRef = firestore.collection(User.COLLECTION_NAME)
            .document(request.toUid)
            .collection(Friend.SUBCOLLECTION_NAME)
            .document(request.fromUid)
        batch.set(
            receiverFriendRef,
            Friend(
                uid = request.fromUid,
                displayName = request.fromDisplayName,
                profileImageUrl = request.fromProfileImageUrl
            )
        )

        // Add friend to sender's list
        val senderFriendRef = firestore.collection(User.COLLECTION_NAME)
            .document(request.fromUid)
            .collection(Friend.SUBCOLLECTION_NAME)
            .document(request.toUid)
        batch.set(
            senderFriendRef,
            Friend(
                uid = request.toUid,
                displayName = toUser?.displayName ?: "",
                profileImageUrl = toUser?.profileImageUrl
            )
        )

        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> = try {
        firestore.collection(FriendRequest.COLLECTION_NAME)
            .document(requestId)
            .update("status", FriendRequestStatus.REJECTED.value)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeFriend(myUid: String, friendUid: String): Result<Unit> = try {
        val batch = firestore.batch()

        val myFriendRef = firestore.collection(User.COLLECTION_NAME)
            .document(myUid)
            .collection(Friend.SUBCOLLECTION_NAME)
            .document(friendUid)
        batch.delete(myFriendRef)

        val theirFriendRef = firestore.collection(User.COLLECTION_NAME)
            .document(friendUid)
            .collection(Friend.SUBCOLLECTION_NAME)
            .document(myUid)
        batch.delete(theirFriendRef)

        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun isFriend(myUid: String, otherUid: String): Boolean {
        return try {
            val doc = firestore.collection(User.COLLECTION_NAME)
                .document(myUid)
                .collection(Friend.SUBCOLLECTION_NAME)
                .document(otherUid)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPendingRequest(fromUid: String, toUid: String): Boolean {
        return try {
            val snapshot = firestore.collection(FriendRequest.COLLECTION_NAME)
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("toUid", toUid)
                .whereEqualTo("status", FriendRequestStatus.PENDING.value)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
