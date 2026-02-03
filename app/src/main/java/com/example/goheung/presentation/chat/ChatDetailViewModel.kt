package com.example.goheung.presentation.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.Message
import com.example.goheung.data.model.MessageType
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ChatDetailFragment
 * Manages chat messages and sending operations
 */
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatRoomId: String = savedStateHandle.get<String>("chatRoomId") ?: ""

    private val _chatRoom = MutableLiveData<ChatRoom>()
    val chatRoom: LiveData<ChatRoom> = _chatRoom

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sendingMessage = MutableLiveData<Boolean>()
    val sendingMessage: LiveData<Boolean> = _sendingMessage

    private val _participants = MutableLiveData<List<User>>()
    val participants: LiveData<List<User>> = _participants

    private val _participantsDisplay = MutableLiveData<String>()
    val participantsDisplay: LiveData<String> = _participantsDisplay

    init {
        loadChatRoom()
        loadMessages()
    }

    /**
     * Load chat room details
     */
    private fun loadChatRoom() {
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "loadChatRoom: chatRoomId is empty")
            _error.value = "Invalid chat room ID"
            return
        }

        Log.d(TAG, "loadChatRoom: Loading chat room for chatRoomId=$chatRoomId")
        viewModelScope.launch {
            val result = chatRepository.getChatRoom(chatRoomId)
            result.fold(
                onSuccess = { room ->
                    Log.d(TAG, "loadChatRoom: Success - name=${room.name}, participants=${room.participants.size}")
                    _chatRoom.value = room
                    loadParticipants(room)
                },
                onFailure = { e ->
                    Log.e(TAG, "loadChatRoom: Failed", e)
                    _error.value = e.message ?: "Failed to load chat room"
                }
            )
        }
    }

    /**
     * Load messages with real-time updates
     */
    private fun loadMessages() {
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "loadMessages: chatRoomId is empty")
            return
        }

        Log.d(TAG, "loadMessages: Starting to load messages for chatRoomId=$chatRoomId")
        viewModelScope.launch {
            _loading.value = true
            chatRepository.getMessages(chatRoomId)
                .catch { e ->
                    Log.e(TAG, "loadMessages: Error in flow", e)
                    _loading.value = false
                    _error.value = e.message ?: "Failed to load messages"
                }
                .collect { result ->
                    _loading.value = false
                    result.fold(
                        onSuccess = { msgs ->
                            Log.d(TAG, "loadMessages: Received ${msgs.size} messages")
                            _messages.value = msgs
                            _error.value = null
                        },
                        onFailure = { e ->
                            Log.e(TAG, "loadMessages: Failed", e)
                            _error.value = e.message ?: "Failed to load messages"
                        }
                    )
                }
        }
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }

    /**
     * Send a text message
     */
    fun sendMessage(text: String, userId: String, userName: String) {
        if (text.isBlank() || chatRoomId.isEmpty()) {
            Log.e(TAG, "sendMessage: Invalid input - text.isBlank=${text.isBlank()}, chatRoomId=$chatRoomId")
            return
        }

        Log.d(TAG, "sendMessage: Sending message to chatRoomId=$chatRoomId")
        viewModelScope.launch {
            _sendingMessage.value = true

            val message = Message(
                chatRoomId = chatRoomId,
                senderId = userId,
                senderName = userName,
                text = text.trim(),
                type = MessageType.TEXT
            )

            val result = chatRepository.sendMessage(message)
            _sendingMessage.value = false

            result.fold(
                onSuccess = { messageId ->
                    Log.d(TAG, "sendMessage: Success - messageId=$messageId")
                    // Message sent successfully
                    // Real-time listener will automatically update the list
                },
                onFailure = { e ->
                    Log.e(TAG, "sendMessage: Failed", e)
                    _error.value = e.message ?: "Failed to send message"
                }
            )
        }
    }

    /**
     * Mark messages as read
     */
    fun markMessagesAsRead(userId: String) {
        if (chatRoomId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatRoomId, userId)
        }
    }

    /**
     * Load participant information
     */
    private fun loadParticipants(room: ChatRoom) {
        Log.d(TAG, "loadParticipants: Loading participants for ${room.participants.size} users")
        viewModelScope.launch {
            val result = userRepository.getUsers(room.participants)
            result.fold(
                onSuccess = { users ->
                    Log.d(TAG, "loadParticipants: Success - loaded ${users.size} users")
                    users.forEach { user ->
                        Log.d(TAG, "  User: ${user.displayName} (${user.uid})")
                    }
                    _participants.value = users
                    val displayText = formatParticipantsDisplay(users)
                    Log.d(TAG, "loadParticipants: Display text = '$displayText'")
                    _participantsDisplay.value = displayText
                },
                onFailure = { e ->
                    Log.e(TAG, "loadParticipants: Failed to load users", e)
                    _error.value = e.message ?: "Failed to load participants"
                }
            )
        }
    }

    /**
     * Format participants display text
     */
    private fun formatParticipantsDisplay(users: List<User>): String {
        return when {
            users.size <= 2 -> "" // 1:1 DM은 표시 안 함
            users.size <= 3 -> users.joinToString(", ") { it.displayName }
            else -> {
                val firstTwo = users.take(2).joinToString(", ") { it.displayName }
                val remaining = users.size - 2
                "$firstTwo 외 ${remaining}명"
            }
        }
    }

    /**
     * Update chat room name (그룹 채팅만)
     */
    fun updateChatRoomName(newName: String) {
        val room = _chatRoom.value ?: return

        // 1:1 DM은 제목 변경 불가
        if (room.participants.size <= 2) {
            _error.value = "1:1 대화방은 제목을 변경할 수 없습니다"
            return
        }

        if (newName.isBlank() || chatRoomId.isEmpty()) return

        viewModelScope.launch {
            val result = chatRepository.updateChatRoomName(chatRoomId, newName.trim())
            result.fold(
                onSuccess = {
                    // 변경 후 채팅방 정보 다시 로드
                    loadChatRoom()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to update chat room name"
                }
            )
        }
    }

    /**
     * 제목 편집 가능 여부 확인
     */
    fun canEditChatName(): Boolean {
        return (_chatRoom.value?.participants?.size ?: 0) > 2
    }

    fun clearError() {
        _error.value = null
    }
}
