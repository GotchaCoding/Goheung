package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for chat operations with Firestore
 * Provides real-time chat room and message data
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    /**
     * Get chat rooms for a specific user with real-time updates
     */
    fun getChatRooms(userId: String): Flow<Result<List<ChatRoom>>> = callbackFlow {
        val listener = firestore.collection(ChatRoom.COLLECTION_NAME)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chatRooms = snapshot.toObjects(ChatRoom::class.java)
                        .sortedByDescending { it.lastMessageTimestamp }
                    trySend(Result.success(chatRooms))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get messages for a specific chat room with real-time updates
     */
    fun getMessages(chatRoomId: String): Flow<Result<List<Message>>> = callbackFlow {
        Log.d(TAG, "getMessages: Setting up listener for chatRoomId=$chatRoomId")
        val listener = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            // orderBy를 임시로 제거하여 인덱스 문제 확인
            // .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getMessages: Firestore error", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // 클라이언트 사이드에서 정렬
                    val messages = snapshot.toObjects(Message::class.java)
                        .sortedBy { it.timestamp }
                    Log.d(TAG, "getMessages: Received ${messages.size} messages from Firestore")
                    messages.forEachIndexed { index, message ->
                        Log.d(TAG, "  Message[$index]: id=${message.id}, text=${message.text}")
                    }
                    trySend(Result.success(messages))
                } else {
                    Log.w(TAG, "getMessages: snapshot is null")
                }
            }

        awaitClose {
            Log.d(TAG, "getMessages: Removing listener for chatRoomId=$chatRoomId")
            listener.remove()
        }
    }

    /**
     * Send a message to a chat room
     */
    suspend fun sendMessage(message: Message): Result<String> = try {
        Log.d(TAG, "sendMessage: Adding message to Firestore - chatRoomId=${message.chatRoomId}, text=${message.text}")
        val docRef = firestore.collection(Message.COLLECTION_NAME)
            .add(message)
            .await()

        Log.d(TAG, "sendMessage: Message added with id=${docRef.id}")

        // Update last message in chat room
        updateChatRoomLastMessage(
            chatRoomId = message.chatRoomId,
            lastMessage = message.text,
            senderId = message.senderId
        )

        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "sendMessage: Failed to send message", e)
        Result.failure(e)
    }

    /**
     * Create a new chat room
     */
    suspend fun createChatRoom(chatRoom: ChatRoom): Result<String> = try {
        val docRef = firestore.collection(ChatRoom.COLLECTION_NAME)
            .add(chatRoom)
            .await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get a specific chat room
     */
    suspend fun getChatRoom(chatRoomId: String): Result<ChatRoom> = try {
        val snapshot = firestore.collection(ChatRoom.COLLECTION_NAME)
            .document(chatRoomId)
            .get()
            .await()

        val chatRoom = snapshot.toObject(ChatRoom::class.java)
        if (chatRoom != null) {
            Result.success(chatRoom)
        } else {
            Result.failure(Exception("Chat room not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get a specific chat room with real-time updates
     */
    fun getChatRoomFlow(chatRoomId: String): Flow<Result<ChatRoom>> = callbackFlow {
        val listener = firestore.collection(ChatRoom.COLLECTION_NAME)
            .document(chatRoomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val chatRoom = snapshot.toObject(ChatRoom::class.java)
                    if (chatRoom != null) {
                        trySend(Result.success(chatRoom))
                    }
                } else {
                    trySend(Result.failure(Exception("Chat room not found")))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update last message info in chat room
     */
    private suspend fun updateChatRoomLastMessage(
        chatRoomId: String,
        lastMessage: String,
        senderId: String
    ) {
        try {
            firestore.collection(ChatRoom.COLLECTION_NAME)
                .document(chatRoomId)
                .update(
                    mapOf(
                        "lastMessage" to lastMessage,
                        "lastMessageSenderId" to senderId,
                        "lastMessageTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the send operation
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatRoomId: String, userId: String): Result<Unit> = try {
        val messagesSnapshot = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        messagesSnapshot.documents
            .filter { it.getString("senderId") != userId }
            .forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }

        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Update chat room name
     */
    suspend fun updateChatRoomName(chatRoomId: String, newName: String): Result<Unit> = try {
        firestore.collection(ChatRoom.COLLECTION_NAME)
            .document(chatRoomId)
            .update("name", newName)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Add participants to chat room
     */
    suspend fun addParticipants(chatRoomId: String, userIds: List<String>): Result<Unit> = try {
        Log.d(TAG, "addParticipants: chatRoomId=$chatRoomId, userIds=$userIds")
        firestore.collection(ChatRoom.COLLECTION_NAME)
            .document(chatRoomId)
            .update("participants", com.google.firebase.firestore.FieldValue.arrayUnion(*userIds.toTypedArray()))
            .await()
        Log.d(TAG, "addParticipants: Success")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "addParticipants: Failed", e)
        Result.failure(e)
    }

    /**
     * Find or create a direct chat between two users
     */
    suspend fun findOrCreateDirectChat(myUid: String, friendUid: String, chatName: String): Result<String> = try {
        // Query for existing chat rooms containing both users
        val existingChats = firestore.collection(ChatRoom.COLLECTION_NAME)
            .whereArrayContains("participants", myUid)
            .get()
            .await()

        // Filter on client side for exact 2-person match
        val directChat = existingChats.documents.firstOrNull { doc ->
            val participants = doc.get("participants") as? List<*>
            participants?.size == 2 && participants.contains(friendUid)
        }

        if (directChat != null) {
            Result.success(directChat.id)
        } else {
            // Create new direct chat
            val newChatRoom = ChatRoom(
                name = chatName,
                description = "1:1 채팅",
                participants = listOf(myUid, friendUid),
                createdBy = myUid
            )
            val docRef = firestore.collection(ChatRoom.COLLECTION_NAME)
                .add(newChatRoom)
                .await()
            Result.success(docRef.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
