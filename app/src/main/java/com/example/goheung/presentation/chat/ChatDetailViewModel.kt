package com.example.goheung.presentation.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.model.Message
import com.example.goheung.data.model.MessageType
import com.example.goheung.data.repository.ChatRepository
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

    init {
        loadChatRoom()
        loadMessages()
    }

    /**
     * Load chat room details
     */
    private fun loadChatRoom() {
        if (chatRoomId.isEmpty()) {
            _error.value = "Invalid chat room ID"
            return
        }

        viewModelScope.launch {
            val result = chatRepository.getChatRoom(chatRoomId)
            result.fold(
                onSuccess = { room ->
                    _chatRoom.value = room
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to load chat room"
                }
            )
        }
    }

    /**
     * Load messages with real-time updates
     */
    private fun loadMessages() {
        if (chatRoomId.isEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            chatRepository.getMessages(chatRoomId)
                .catch { e ->
                    _loading.value = false
                    _error.value = e.message ?: "Failed to load messages"
                }
                .collect { result ->
                    _loading.value = false
                    result.fold(
                        onSuccess = { msgs ->
                            _messages.value = msgs
                            _error.value = null
                        },
                        onFailure = { e ->
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
        if (text.isBlank() || chatRoomId.isEmpty()) return

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
                onSuccess = {
                    // Message sent successfully
                    // Real-time listener will automatically update the list
                },
                onFailure = { e ->
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

    fun clearError() {
        _error.value = null
    }
}
