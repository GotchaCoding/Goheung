package com.example.goheung.data.firebase

import com.example.goheung.base.BaseRepository
import com.example.goheung.base.Resource
import com.example.goheung.model.ChatMessageModel
import com.example.goheung.model.ChatRoomModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : BaseRepository(), ChatRepository {

    override fun getChatRooms(userId: String): Flow<List<ChatRoomModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = mutableListOf<ChatRoomModel>()
                snapshot.children.forEach { roomSnapshot ->
                    val participants = roomSnapshot.child("participants").children
                        .mapNotNull { it.key }

                    if (participants.contains(userId)) {
                        val participantNames = roomSnapshot.child("participantNames").children
                            .associate { it.key!! to (it.value as? String ?: "") }

                        val room = ChatRoomModel(
                            chatRoomId = roomSnapshot.key ?: "",
                            participants = participants,
                            participantNames = participantNames,
                            lastMessage = roomSnapshot.child("lastMessage").value as? String ?: "",
                            lastMessageTimestamp = roomSnapshot.child("lastMessageTimestamp").value as? Long ?: 0L
                        )
                        rooms.add(room)
                    }
                }
                // Sort by timestamp descending
                rooms.sortByDescending { it.lastMessageTimestamp }
                trySend(rooms)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("chat_rooms").addValueEventListener(listener)
        awaitClose { database.getReference("chat_rooms").removeEventListener(listener) }
    }

    override fun getMessages(chatRoomId: String): Flow<List<ChatMessageModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { messageSnapshot ->
                    ChatMessageModel(
                        messageId = messageSnapshot.key ?: "",
                        senderId = messageSnapshot.child("senderId").value as? String ?: "",
                        text = messageSnapshot.child("text").value as? String ?: "",
                        timestamp = messageSnapshot.child("timestamp").value as? Long ?: 0L
                    )
                }.sortedBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("chat_rooms/$chatRoomId/messages")
            .addValueEventListener(listener)

        awaitClose {
            database.getReference("chat_rooms/$chatRoomId/messages")
                .removeEventListener(listener)
        }
    }

    override fun sendMessage(chatRoomId: String, message: ChatMessageModel): Flow<Resource<Unit>> {
        return callFirebase {
            val chatRoomRef = database.getReference("chat_rooms/$chatRoomId")
            val messageRef = chatRoomRef.child("messages").push()

            val messageData = mapOf(
                "senderId" to message.senderId,
                "text" to message.text,
                "timestamp" to System.currentTimeMillis()
            )

            // Add message
            messageRef.setValue(messageData).await()

            // Update last message
            val updates = mapOf(
                "lastMessage" to message.text,
                "lastMessageTimestamp" to System.currentTimeMillis()
            )
            chatRoomRef.updateChildren(updates).await()
        }
    }

    override fun createChatRoom(
        participants: List<String>,
        participantNames: Map<String, String>
    ): Flow<Resource<String>> {
        return callFirebase {
            val chatRoomRef = database.getReference("chat_rooms").push()
            val chatRoomId = chatRoomRef.key!!

            val participantsMap = participants.associateWith { true }
            val chatRoomData = mapOf(
                "participants" to participantsMap,
                "participantNames" to participantNames,
                "lastMessage" to "",
                "lastMessageTimestamp" to System.currentTimeMillis()
            )

            chatRoomRef.setValue(chatRoomData).await()
            chatRoomId
        }
    }
}
