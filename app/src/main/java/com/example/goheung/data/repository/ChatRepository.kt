package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.ChatRoomType
import com.example.goheung.data.model.Message
import com.example.goheung.data.model.MessageType
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

        // 메시지 전송 시 hiddenBy 초기화 (숨겼던 사용자에게 다시 보임)
        unhideChatRoom(message.chatRoomId)

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
     * 복합 인덱스 없이 클라이언트 사이드 필터링 사용
     */
    suspend fun markMessagesAsRead(chatRoomId: String, userId: String): Result<Unit> = try {
        Log.d(TAG, "markMessagesAsRead: chatRoomId=$chatRoomId, userId=$userId")

        val messagesSnapshot = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .get()
            .await()

        Log.d(TAG, "markMessagesAsRead: Found ${messagesSnapshot.documents.size} messages")

        val batch = firestore.batch()
        var updateCount = 0
        messagesSnapshot.documents
            .filter { doc ->
                val senderId = doc.getString("senderId")
                val isRead = doc.getBoolean("isRead") ?: false
                senderId != userId && !isRead
            }
            .forEach { doc ->
                batch.update(doc.reference, "isRead", true)
                updateCount++
            }

        if (updateCount > 0) {
            batch.commit().await()
            Log.d(TAG, "markMessagesAsRead: Updated $updateCount messages to read")
        } else {
            Log.d(TAG, "markMessagesAsRead: No messages to update")
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "markMessagesAsRead: Failed", e)
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

        // Filter on client side for DM type with exact 2-person match
        val directChat = existingChats.documents.firstOrNull { doc ->
            val participants = doc.get("participants") as? List<*>
            val type = doc.getString("type")?.let { ChatRoomType.valueOf(it) } ?: ChatRoomType.GROUP
            type == ChatRoomType.DM && participants?.size == 2 && participants.contains(friendUid)
        }

        if (directChat != null) {
            Result.success(directChat.id)
        } else {
            // Create new direct chat
            val newChatRoom = ChatRoom(
                name = chatName,
                description = "1:1 채팅",
                participants = listOf(myUid, friendUid),
                createdBy = myUid,
                type = ChatRoomType.DM
            )
            val docRef = firestore.collection(ChatRoom.COLLECTION_NAME)
                .add(newChatRoom)
                .await()
            Result.success(docRef.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Toggle favorite status for a chat room
     */
    suspend fun toggleFavorite(chatRoomId: String, userId: String): Result<Unit> = try {
        Log.d(TAG, "toggleFavorite: chatRoomId=$chatRoomId, userId=$userId")
        val docRef = firestore.collection(ChatRoom.COLLECTION_NAME).document(chatRoomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val favoriteBy = (snapshot.get("favoriteBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            Log.d(TAG, "toggleFavorite: Current favoriteBy=$favoriteBy")

            val newFavoriteBy = if (userId in favoriteBy) {
                // Remove from favorites
                favoriteBy - userId
            } else {
                // Add to favorites
                favoriteBy + userId
            }

            Log.d(TAG, "toggleFavorite: New favoriteBy=$newFavoriteBy")
            transaction.update(docRef, "favoriteBy", newFavoriteBy)
        }.await()

        Log.d(TAG, "toggleFavorite: Success")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "toggleFavorite: Failed", e)
        Result.failure(e)
    }

    /**
     * Leave a chat room
     * DM: participants 유지 + hiddenBy에 추가 (메시지 오면 다시 나타남)
     * GROUP: participants에서 제거 + 시스템 메시지
     */
    suspend fun leaveChatRoom(chatRoomId: String, userId: String, userName: String): Result<Unit> = try {
        Log.d(TAG, "leaveChatRoom: chatRoomId=$chatRoomId, userId=$userId, userName=$userName")

        // 채팅방 정보 가져오기
        val chatRoomSnapshot = firestore.collection(ChatRoom.COLLECTION_NAME)
            .document(chatRoomId)
            .get()
            .await()

        val chatRoomType = chatRoomSnapshot.getString("type")?.let {
            ChatRoomType.valueOf(it)
        } ?: ChatRoomType.GROUP

        if (chatRoomType == ChatRoomType.DM) {
            // DM: hiddenBy에만 추가 (participants는 유지)
            Log.d(TAG, "leaveChatRoom: DM - Adding to hiddenBy")
            firestore.collection(ChatRoom.COLLECTION_NAME)
                .document(chatRoomId)
                .update("hiddenBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .await()
        } else {
            // GROUP: participants에서 제거 + 시스템 메시지
            Log.d(TAG, "leaveChatRoom: GROUP - Removing from participants")
            firestore.collection(ChatRoom.COLLECTION_NAME)
                .document(chatRoomId)
                .update("participants", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .await()

            // 시스템 메시지 추가
            val systemMessage = Message(
                chatRoomId = chatRoomId,
                senderId = "",
                senderName = "",
                text = "${userName}님이 나갔습니다",
                type = MessageType.SYSTEM
            )
            firestore.collection(Message.COLLECTION_NAME)
                .add(systemMessage)
                .await()
        }

        Log.d(TAG, "leaveChatRoom: Success")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "leaveChatRoom: Failed", e)
        Result.failure(e)
    }

    /**
     * Add system message when user joins
     */
    suspend fun addJoinMessage(chatRoomId: String, userName: String): Result<Unit> = try {
        val systemMessage = Message(
            chatRoomId = chatRoomId,
            senderId = "",
            senderName = "",
            text = "${userName}님이 입장했습니다",
            type = MessageType.SYSTEM
        )
        firestore.collection(Message.COLLECTION_NAME)
            .add(systemMessage)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Unhide chat room (clear hiddenBy list)
     * 메시지 전송 시 숨긴 사용자에게 다시 보이도록 함
     */
    private suspend fun unhideChatRoom(chatRoomId: String) {
        try {
            Log.d(TAG, "unhideChatRoom: chatRoomId=$chatRoomId")
            firestore.collection(ChatRoom.COLLECTION_NAME)
                .document(chatRoomId)
                .update("hiddenBy", emptyList<String>())
                .await()
            Log.d(TAG, "unhideChatRoom: Success")
        } catch (e: Exception) {
            // 실패해도 메시지 전송은 성공으로 처리
            Log.w(TAG, "unhideChatRoom: Failed but ignore", e)
        }
    }

    /**
     * Get unread message count for a chat room
     * 본인이 보내지 않은 메시지 중 읽지 않은 메시지 개수
     * Note: 복합 인덱스 필요 없이 클라이언트 사이드 필터링 사용
     */
    suspend fun getUnreadCount(chatRoomId: String, userId: String): Result<Int> = try {
        Log.d(TAG, "getUnreadCount: Starting query for chatRoomId=$chatRoomId, userId=$userId")

        val snapshot = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .get()
            .await()

        Log.d(TAG, "getUnreadCount: Found ${snapshot.documents.size} total messages")

        // 본인이 보낸 메시지 제외 + 읽지 않은 메시지만 카운트 (클라이언트 필터링)
        // isRead 필드가 없으면 false(읽지 않음)로 간주
        val unreadCount = snapshot.documents.count { doc ->
            val senderId = doc.getString("senderId")
            val isRead = doc.getBoolean("isRead") ?: false
            senderId != userId && !isRead
        }

        Log.d(TAG, "getUnreadCount: chatRoomId=$chatRoomId, unreadCount=$unreadCount")
        Result.success(unreadCount)
    } catch (e: Exception) {
        Log.e(TAG, "getUnreadCount: Failed for chatRoomId=$chatRoomId", e)
        Result.failure(e)
    }
}
