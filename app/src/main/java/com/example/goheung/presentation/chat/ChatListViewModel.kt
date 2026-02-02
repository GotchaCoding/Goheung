package com.example.goheung.presentation.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.data.model.ChatRoom
import com.example.goheung.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatRooms = MutableLiveData<List<ChatRoom>>()
    val chatRooms: LiveData<List<ChatRoom>> = _chatRooms

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _createdChatRoom = MutableLiveData<Pair<String, String>?>()
    val createdChatRoom: LiveData<Pair<String, String>?> = _createdChatRoom

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        viewModelScope.launch {
            _loading.value = true
            chatRepository.getChatRooms()
                .catch { e ->
                    _loading.value = false
                    _error.value = e.message ?: "Failed to load chat rooms"
                }
                .collect { result ->
                    _loading.value = false
                    result.fold(
                        onSuccess = { rooms ->
                            _chatRooms.value = rooms
                            _error.value = null
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "Failed to load chat rooms"
                        }
                    )
                }
        }
    }

    fun createChatRoom(name: String, description: String, userId: String) {
        viewModelScope.launch {
            _loading.value = true
            val chatRoom = ChatRoom(
                name = name,
                description = description,
                participants = listOf(userId),
                createdBy = userId
            )

            val result = chatRepository.createChatRoom(chatRoom)
            _loading.value = false

            result.fold(
                onSuccess = { chatRoomId ->
                    _createdChatRoom.value = Pair(chatRoomId, name)
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Failed to create chat room"
                }
            )
        }
    }

    fun onNavigatedToCreatedRoom() {
        _createdChatRoom.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
