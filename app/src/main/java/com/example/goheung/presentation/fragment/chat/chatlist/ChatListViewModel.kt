package com.example.goheung.presentation.fragment.chat.chatlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goheung.base.Resource
import com.example.goheung.data.firebase.AuthRepository
import com.example.goheung.data.firebase.ChatRepository
import com.example.goheung.data.firebase.UserRepository
import com.example.goheung.model.ChatRoomModel
import com.example.goheung.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _chatRooms = MutableLiveData<List<ChatRoomModel>>()
    val chatRooms: LiveData<List<ChatRoomModel>> = _chatRooms

    private val _users = MutableLiveData<List<UserModel>>()
    val users: LiveData<List<UserModel>> = _users

    private val _newChatRoomId = MutableLiveData<String>()
    val newChatRoomId: LiveData<String> = _newChatRoomId

    fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.uid ?: ""
    }

    fun loadChatRooms() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        chatRepository.getChatRooms(userId)
            .onEach { rooms ->
                _chatRooms.value = rooms
            }
            .catch { /* handle error */ }
            .launchIn(viewModelScope)
    }

    fun loadUsers() {
        userRepository.getAllUsers()
            .onEach { result ->
                if (result is Resource.Success) {
                    _users.value = result.model.filter { it.uid != getCurrentUserId() }
                }
            }
            .launchIn(viewModelScope)
    }

    fun createChatRoom(otherUserId: String, otherUserName: String) {
        val myId = getCurrentUserId()
        val myName = authRepository.getCurrentUser()?.displayName ?: ""
        val participants = listOf(myId, otherUserId)
        val names = mapOf(myId to myName, otherUserId to otherUserName)

        chatRepository.createChatRoom(participants, names)
            .onEach { result ->
                if (result is Resource.Success) {
                    _newChatRoomId.value = result.model
                }
            }
            .launchIn(viewModelScope)
    }
}
