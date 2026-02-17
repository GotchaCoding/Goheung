package com.example.goheung.data.repository

import android.util.Log
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.ChatRoomType
import com.example.goheung.data.model.Message
import com.example.goheung.data.model.MessageType
import com.example.goheung.data.model.TypingStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for chat operations with Firestore
 * Provides real-time chat room and message data
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDatabase: FirebaseDatabase
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val TYPING_STATUS_PATH = "typing_status"
        private const val DEFAULT_PAGE_SIZE = 50
        private const val LOAD_MORE_PAGE_SIZE = 30
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

    // ==================== Pagination Functions ====================

    /**
     * Get latest messages (initial load)
     * @param chatRoomId Chat room ID
     * @param limit Number of messages to load (default 50)
     * @return Result containing list of messages sorted by timestamp ascending
     */
    suspend fun getLatestMessages(
        chatRoomId: String,
        limit: Int = DEFAULT_PAGE_SIZE
    ): Result<List<Message>> = try {
        Log.d(TAG, "getLatestMessages: chatRoomId=$chatRoomId, limit=$limit")

        val snapshot = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val messages = snapshot.toObjects(Message::class.java)
            .sortedBy { it.timestamp }

        Log.d(TAG, "getLatestMessages: Loaded ${messages.size} messages")
        Result.success(messages)
    } catch (e: Exception) {
        Log.e(TAG, "getLatestMessages: Failed", e)
        Result.failure(e)
    }

    /**
     * Get messages before a specific timestamp (pagination load)
     * @param chatRoomId Chat room ID
     * @param beforeTimestamp Load messages before this timestamp
     * @param limit Number of messages to load (default 30)
     * @return Result containing list of messages sorted by timestamp ascending
     */
    suspend fun getMessagesBefore(
        chatRoomId: String,
        beforeTimestamp: Long,
        limit: Int = LOAD_MORE_PAGE_SIZE
    ): Result<List<Message>> = try {
        Log.d(TAG, "getMessagesBefore: chatRoomId=$chatRoomId, beforeTimestamp=$beforeTimestamp, limit=$limit")

        val beforeDate = Date(beforeTimestamp)
        val snapshot = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .whereLessThan("timestamp", beforeDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val messages = snapshot.toObjects(Message::class.java)
            .sortedBy { it.timestamp }

        Log.d(TAG, "getMessagesBefore: Loaded ${messages.size} older messages")
        Result.success(messages)
    } catch (e: Exception) {
        Log.e(TAG, "getMessagesBefore: Failed", e)
        Result.failure(e)
    }

    /**
     * Observe new messages in real-time (after initial load)
     * @param chatRoomId Chat room ID
     * @param afterTimestamp Only receive messages after this timestamp
     * @return Flow emitting new messages one by one
     */
    fun observeNewMessages(
        chatRoomId: String,
        afterTimestamp: Long
    ): Flow<Result<Message>> = callbackFlow {
        Log.d(TAG, "observeNewMessages: Starting listener for chatRoomId=$chatRoomId, afterTimestamp=$afterTimestamp")

        val afterDate = Date(afterTimestamp)
        val listener = firestore.collection(Message.COLLECTION_NAME)
            .whereEqualTo("chatRoomId", chatRoomId)
            .whereGreaterThan("timestamp", afterDate)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeNewMessages: Error", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val message = change.document.toObject(Message::class.java)
                            Log.d(TAG, "observeNewMessages: New message received - id=${message.id}")
                            trySend(Result.success(message))
                        }
                    }
                }
            }

        awaitClose {
            Log.d(TAG, "observeNewMessages: Removing listener")
            listener.remove()
        }
    }

    // ==================== Typing Status Functions ====================

    /**
     * Update typing status in Firebase Realtime Database
     * @param chatRoomId Chat room ID
     * @param userId User ID
     * @param userName User display name
     * @param isTyping Whether the user is typing
     */
    suspend fun updateTypingStatus(
        chatRoomId: String,
        userId: String,
        userName: String,
        isTyping: Boolean
    ): Result<Unit> = try {
        Log.d(TAG, "updateTypingStatus: chatRoomId=$chatRoomId, userId=$userId, isTyping=$isTyping")

        val typingRef = realtimeDatabase.reference
            .child(TYPING_STATUS_PATH)
            .child(chatRoomId)
            .child(userId)

        if (isTyping) {
            val typingStatus = mapOf(
                "userId" to userId,
                "userName" to userName,
                "isTyping" to true,
                "timestamp" to System.currentTimeMillis()
            )
            typingRef.setValue(typingStatus).await()
        } else {
            typingRef.removeValue().await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "updateTypingStatus: Failed", e)
        Result.failure(e)
    }

    /**
     * Observe typing status in a chat room
     * @param chatRoomId Chat room ID
     * @param excludeUserId Exclude this user from typing list (usually current user)
     * @return Flow emitting list of users currently typing
     */
    fun observeTypingStatus(
        chatRoomId: String,
        excludeUserId: String
    ): Flow<List<TypingStatus>> = callbackFlow {
        Log.d(TAG, "observeTypingStatus: Starting listener for chatRoomId=$chatRoomId")

        val typingRef = realtimeDatabase.reference
            .child(TYPING_STATUS_PATH)
            .child(chatRoomId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingUsers = mutableListOf<TypingStatus>()
                val currentTime = System.currentTimeMillis()
                val staleThreshold = 5000L // 5 seconds

                for (child in snapshot.children) {
                    val userId = child.child("userId").getValue(String::class.java) ?: continue
                    val userName = child.child("userName").getValue(String::class.java) ?: continue
                    val isTyping = child.child("isTyping").getValue(Boolean::class.java) ?: false
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                    // Exclude current user and check if not stale
                    if (userId != excludeUserId && isTyping && (currentTime - timestamp) < staleThreshold) {
                        typingUsers.add(
                            TypingStatus(
                                userId = userId,
                                userName = userName,
                                isTyping = true,
                                timestamp = timestamp
                            )
                        )
                    }
                }

                Log.d(TAG, "observeTypingStatus: ${typingUsers.size} users typing")
                trySend(typingUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeTypingStatus: Cancelled", error.toException())
                trySend(emptyList())
            }
        }

        typingRef.addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "observeTypingStatus: Removing listener")
            typingRef.removeEventListener(listener)
        }
    }

    /**
     * Clear typing status when leaving chat
     */
    suspend fun clearTypingStatus(chatRoomId: String, userId: String): Result<Unit> = try {
        realtimeDatabase.reference
            .child(TYPING_STATUS_PATH)
            .child(chatRoomId)
            .child(userId)
            .removeValue()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "clearTypingStatus: Failed", e)
        Result.failure(e)
    }
}
