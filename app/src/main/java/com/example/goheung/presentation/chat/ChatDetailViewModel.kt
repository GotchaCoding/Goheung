package com.example.goheung.presentation.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.ChatRoomType
import com.example.goheung.data.model.Message
import com.example.goheung.data.model.MessageType
import com.example.goheung.data.model.TypingStatus
import com.example.goheung.data.model.User
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Typing indicator state
    private val _typingUsers = MutableLiveData<List<TypingStatus>>(emptyList())
    val typingUsers: LiveData<List<TypingStatus>> = _typingUsers

    private var typingJob: Job? = null
    private var currentUserId: String = ""
    private var currentUserName: String = ""

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
        Log.d(TAG, "markMessagesAsRead: chatRoomId=$chatRoomId, userId=$userId")
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "markMessagesAsRead: chatRoomId is empty")
            return
        }

        viewModelScope.launch {
            val result = chatRepository.markMessagesAsRead(chatRoomId, userId)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "markMessagesAsRead: Success")
                },
                onFailure = { e ->
                    Log.e(TAG, "markMessagesAsRead: Failed", e)
                }
            )
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
        if (room.type == ChatRoomType.DM) {
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
     * 그룹 채팅 여부 확인
     */
    fun isGroupChat(): Boolean {
        return _chatRoom.value?.type == ChatRoomType.GROUP
    }

    /**
     * 제목 편집 가능 여부 확인
     */
    fun canEditChatName(): Boolean {
        return _chatRoom.value?.type == ChatRoomType.GROUP
    }

    /**
     * 채팅방에 사용자 초대
     */
    fun inviteUsers(userIds: List<String>) {
        if (userIds.isEmpty() || chatRoomId.isEmpty()) {
            Log.w(TAG, "inviteUsers: Invalid parameters")
            return
        }

        Log.d(TAG, "inviteUsers: Inviting ${userIds.size} users to chatRoomId=$chatRoomId")
        viewModelScope.launch {
            // 참여자 추가
            val addResult = chatRepository.addParticipants(chatRoomId, userIds)
            addResult.fold(
                onSuccess = {
                    Log.d(TAG, "inviteUsers: Success")

                    // 초대된 사용자들의 정보 가져오기
                    val usersResult = userRepository.getUsers(userIds)
                    usersResult.fold(
                        onSuccess = { users ->
                            // 각 사용자에 대해 입장 시스템 메시지 추가
                            users.forEach { user ->
                                viewModelScope.launch {
                                    chatRepository.addJoinMessage(chatRoomId, user.displayName)
                                }
                            }
                        },
                        onFailure = { e ->
                            Log.w(TAG, "inviteUsers: Failed to get user names", e)
                        }
                    )

                    // 채팅방 정보 다시 로드하여 참여자 업데이트
                    loadChatRoom()
                },
                onFailure = { e ->
                    Log.e(TAG, "inviteUsers: Failed", e)
                    _error.value = e.message ?: "Failed to invite users"
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ==================== Typing Indicator Functions ====================

    /**
     * Initialize typing status observation
     * Should be called when fragment resumes with current user info
     */
    fun initTypingStatus(userId: String, userName: String) {
        currentUserId = userId
        currentUserName = userName
        observeTypingStatus()
    }

    /**
     * Observe typing status from other users
     */
    private fun observeTypingStatus() {
        if (chatRoomId.isEmpty() || currentUserId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.observeTypingStatus(chatRoomId, currentUserId)
                .catch { e ->
                    Log.e(TAG, "observeTypingStatus: Error", e)
                }
                .collect { typingList ->
                    Log.d(TAG, "observeTypingStatus: ${typingList.size} users typing")
                    _typingUsers.value = typingList
                }
        }
    }

    /**
     * Update typing status
     * Called when user starts/stops typing
     * Auto-clears after 2 seconds of no input
     */
    fun notifyTypingStatus(isTyping: Boolean) {
        if (chatRoomId.isEmpty() || currentUserId.isEmpty()) return

        // Cancel previous typing timeout
        typingJob?.cancel()

        viewModelScope.launch {
            if (isTyping) {
                // Update typing status
                chatRepository.updateTypingStatus(
                    chatRoomId = chatRoomId,
                    userId = currentUserId,
                    userName = currentUserName,
                    isTyping = true
                )

                // Auto-clear after 2 seconds
                typingJob = viewModelScope.launch {
                    delay(TYPING_TIMEOUT_MS)
                    clearTypingStatus()
                }
            } else {
                clearTypingStatus()
            }
        }
    }

    /**
     * Clear typing status
     */
    private suspend fun clearTypingStatus() {
        chatRepository.updateTypingStatus(
            chatRoomId = chatRoomId,
            userId = currentUserId,
            userName = currentUserName,
            isTyping = false
        )
    }

    /**
     * Clean up typing status when leaving chat
     */
    fun cleanupTypingStatus() {
        if (chatRoomId.isEmpty() || currentUserId.isEmpty()) return

        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.clearTypingStatus(chatRoomId, currentUserId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupTypingStatus()
    }

    companion object {
        private const val TAG = "ChatDetailViewModel"
        private const val TYPING_TIMEOUT_MS = 2000L
    }
}
